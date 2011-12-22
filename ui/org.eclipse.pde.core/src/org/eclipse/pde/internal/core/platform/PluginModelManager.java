/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.platform;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.platform.PluginModelResourceListener.ResourceChange;
import org.eclipse.pde.internal.core.platform.PluginModelResourceListener.WorkspacePluginContext;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;

/**
 * The new and improved model manager stores as much information as possible in the PDEState. It acts
 * as an access point for the models, manages the various change listeners, and updates the models and
 * state as the workspace and target bundles change.
 *
 */
public class PluginModelManager {

	/**
	 * keeps the combined view of the target and workspace
	 */
	private PDEState fState;

	/**
	 * a list of listeners interested in changes to the plug-in models 
	 */
	private ListenerList/*<IPluginModelListener>*/fListeners;

	/**
	 * a list of listeners interested in changes to the PDE/resolver State
	 */
	private ListenerList/*<IStateDeltaListener>*/fStateListeners;

	/**
	 * a list of listeners interested in changes affecting known extension points
	 */
	private ListenerList/*<IExtensionDeltaListener>*/fExtensionListeners;

	/**
	 * Called as part of save participant. Save's PDE model state.
	 */
	public void saveState() {
		if (fState != null) {
			fState.saveState();
		}
	}

	/**
	 * Perform cleanup upon shutting down
	 */
	public void dispose() {
		if (fListeners != null) {
			fListeners.clear();
			fListeners = null;
		}
		if (fStateListeners != null) {
			fStateListeners.clear();
			fStateListeners = null;
		}
		if (fExtensionListeners != null) {
			fExtensionListeners.clear();
			fExtensionListeners = null;
		}
	}

	/**
	 * Adds a plug-in model listener to the model manager
	 * 
	 * @param listener  the listener to be added
	 */
	public void addPluginModelListener(IPluginModelListener listener) {
		if (fListeners == null)
			fListeners = new ListenerList();
		fListeners.add(listener);
	}

	/**
	 * Adds a StateDelta listener to the model manager
	 * 
	 * @param listener	the listener to be added
	 */
	public void addStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners == null)
			fStateListeners = new ListenerList();
		fStateListeners.add(listener);
	}

	/**
	 * Adds an extension delta listener to the model manager
	 * 
	 * @param listener	the listener to be added
	 */
	public void addExtensionDeltaListener(IExtensionDeltaListener listener) {
		if (fExtensionListeners == null)
			fExtensionListeners = new ListenerList();
		fExtensionListeners.add(listener);
	}

	/**
	 * Notify all interested listeners that changes are made to the plug-in models
	 * 
	 * @param delta  the delta of model changes
	 */
	private void firePluginModelDelta(PluginModelDelta delta) {
		if (fListeners != null) {
			Object[] listeners = fListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IPluginModelListener) listeners[i]).modelsChanged(delta);
			}
		}
	}

	/**
	 * Notify all interested listeners in changes made to the resolver State
	 * 
	 * @param delta	the delta from the resolver State.
	 */
	private void fireStateDelta(StateDelta delta) {
		if (fStateListeners != null) {
			Object[] listeners = fStateListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IStateDeltaListener) listeners[i]).stateResolved(delta);
			}
		}
	}

	/**
	 * Notify all interested listeners that extension points have changed
	 * 
	 * @param delta the delta of extension changes
	 */
	private void fireExtensionDelta(IExtensionDeltaEvent delta) {
		if (fExtensionListeners != null) {
			Object[] listeners = fExtensionListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IExtensionDeltaListener) listeners[i]).extensionsChanged(delta);
			}
		}
	}

	/**
	 * Handles changes in the workspace, updating all relevant models, state and
	 * managers. Fires events to registered listeners once changes have been made.
	 * 
	 * @param context contains information on what changed in the workspace
	 */
	public void workspaceChanged(WorkspacePluginContext context) {
		PluginModelDelta delta = new PluginModelDelta();
		ExtensionDeltaEvent extensionDelta = new ExtensionDeltaEvent(types, added, removed, changed)
		Set addedBSNs = new HashSet(); // TODO Need some way of knowing whether to resolve the state
		if (context.addedProjects != null) {
			for (Iterator iterator = context.addedProjects.iterator(); iterator.hasNext();) {
				fState.addWorkspaceBundle((IProject) iterator.next(), delta);
			}
		}
		if (context.removedProjects != null) {
			for (Iterator iterator = context.removedProjects.iterator(); iterator.hasNext();) {
				fState.removeWorkspaceBundle((IProject) iterator.next(), delta);

			}
		}
		if (context.changedManifests != null) {
			for (Iterator iterator = context.changedManifests.iterator(); iterator.hasNext();) {
				handleBundleManifestFileChange((ResourceChange) iterator.next(), delta);
			}
		}
		if (context.changedExtensions != null) {
			for (Iterator iterator = context.changedExtensions.iterator(); iterator.hasNext();) {
				handleExtensionFileChange((ResourceChange) iterator.next(), delta);
			}
		}
		if (context.changedBuildProperties != null) {
			for (Iterator iterator = context.addedProjects.iterator(); iterator.hasNext();) {
				ResourceChange resourceChange = (ResourceChange) iterator.next();

				// change in build.properties should trigger a Classpath Update
				// we therefore fire a notification
				//TODO this is inefficient.  we could do better.
//				Object model = getModel(project);
//				if (model != null) {
//					addChange(model, IModelProviderEvent.MODELS_CHANGED);
//				}

			}
		}
		if (context.changedLocalizationFiles != null) {
			for (Iterator iterator = context.changedLocalizationFiles.iterator(); iterator.hasNext();) {
				handleLocalizationFileChange((ResourceChange) iterator.next());
			}
		}
		if (context.changedSchemas != null) {
			for (Iterator iterator = context.changedSchemas.iterator(); iterator.hasNext();) {
				handleSchemaFileChange((ResourceChange) iterator.next());
			}
		}
		if (context.changedTracingFiles != null) {
			PDECore.getDefault().getTracingOptionsManager().reset();
		}

		// TODO resolve the state
		// TODO handle extension changes and fire event
		firePluginModelDelta(delta);
	}

	/**
	 * Reacts to changes in the MANIFEST.MF file.
	 * <ul>
	 * <li>If the file has been added, create a new bundle model</li>
	 * <li>If the file has been deleted, switch to the old-style plug-in if a plugin.xml file exists</li>
	 * <li>If the file has been modified, reload the model, reset the resource bundle
	 * if the localization has changed and fire a notification that the model has changed</li>
	 * </ul>
	 * 
	 * @param file the manifest file that was modified
	 * @param delta the resource delta
	 */
	private void handleBundleManifestFileChange(ResourceChange resourceChange, PluginModelDelta delta) {
		int kind = resourceChange.getKind();
		IProject project = resourceChange.getResource().getProject();
		IPluginModelBase model = fState.getModel(project);
		if (kind == IResourceDelta.ADDED || model == null) {
			fState.addWorkspaceBundle(project, delta);
		} else if (kind == IResourceDelta.REMOVED) {
			fState.removeWorkspaceBundle(project, delta);
			// switch to legacy plugin structure, if applicable
			fState.addWorkspaceBundle(project, delta);
		} else if (kind == IResourceDelta.CHANGED) {
			if (model instanceof IBundlePluginModelBase) {
				// check to see if localization changed (bug 146912)
				String oldLocalization = ((IBundlePluginModelBase) model).getBundleLocalization();
				IBundleModel bmodel = ((IBundlePluginModelBase) model).getBundleModel();
				boolean wasFragment = bmodel.isFragmentModel();
				loadModel(bmodel, true);
				String newLocalization = ((IBundlePluginModelBase) model).getBundleLocalization();

				// Fragment-Host header was added or removed
				if (wasFragment != bmodel.isFragmentModel()) {
					fState.removeWorkspaceBundle(project, delta);
					fState.addWorkspaceBundle(project, delta);
				} else {
					if (model instanceof AbstractNLModel && (oldLocalization != null && (newLocalization == null || !oldLocalization.equals(newLocalization))) || (newLocalization != null && (oldLocalization == null || !newLocalization.equals(oldLocalization)))) {
						((AbstractNLModel) model).resetNLResourceHelper();
					}
					fState.updateWorkspaceBundle(project, delta);
				}
			}
		}
	}

	/**
	 * Reacts to changes in the plugin.xml or fragment.xml file.
	 * <ul>
	 * <li>If the file has been deleted and the project has a MANIFEST.MF file,
	 * then this deletion only affects extensions and extension points.</li>
	 * <li>If the file has been deleted and the project does not have a MANIFEST.MF file,
	 * then it's an old-style plug-in and the entire model must be removed from the table.</li>
	 * <li>If the file has been added and the project already has a MANIFEST.MF, then
	 * this file only contributes extensions and extensions.  No need to send a notification
	 * to trigger update classpath of dependent plug-ins</li>
	 * <li>If the file has been added and the project does not have a MANIFEST.MF, then
	 * an old-style plug-in has been created.</li>
	 * <li>If the file has been modified and the project already has a MANIFEST.MF,
	 * then reload the extensions model but do not send out notifications</li>
	 * </li>If the file has been modified and the project has no MANIFEST.MF, then
	 * it's an old-style plug-in, reload and send out notifications to trigger a classpath update
	 * for dependent plug-ins</li>
	 * </ul>
	 * @param file the manifest file
	 * @param delta the resource delta
	 */
	private void handleExtensionFileChange(ResourceChange resourceChange, PluginModelDelta delta) {
		int kind = resourceChange.getKind();
		IFile file = resourceChange.getResource();
		IPluginModelBase model = fState.getModel(file.getProject());
		if (kind == IResourceDelta.ADDED) {
			if (model instanceof IBundlePluginModelBase) {
				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
				extensions.setEditable(false);
				((IBundlePluginModelBase) model).setExtensionsModel(extensions);
				extensions.setBundleModel((IBundlePluginModelBase) model);
				loadModel(extensions, false);
				addExtensionChange(model, IModelProviderEvent.MODELS_ADDED);
			} else {
				// Try to add as an old school plug-in
				fState.addWorkspaceBundle(file.getProject(), delta);
			}
		} else if (kind == IResourceDelta.REMOVED) {
			if (model instanceof IBundlePluginModelBase) {
				((IBundlePluginModelBase) model).setExtensionsModel(null);
				addExtensionChange(model, IModelProviderEvent.MODELS_REMOVED);
			} else {
				// Old school plug-in, remove it as the plugin.xml is gone
				fState.removeWorkspaceBundle(file.getProject(), delta);
			}
		} else if (kind == IResourceDelta.CHANGED) {
			if (model instanceof IBundlePluginModelBase) {
				ISharedExtensionsModel extensions = ((IBundlePluginModelBase) model).getExtensionsModel();
				boolean reload = extensions != null;
				if (extensions == null) {
					extensions = new WorkspaceExtensionsModel(file);
					((WorkspaceExtensionsModel) extensions).setEditable(false);
					((IBundlePluginModelBase) model).setExtensionsModel(extensions);
					((WorkspaceExtensionsModel) extensions).setBundleModel((IBundlePluginModelBase) model);
				}
				loadModel(extensions, reload);
			} else if (model != null) {
				fState.updateWorkspaceBundle(file.getProject(), delta);
//				loadModel(model, true);
			}
			addExtensionChange(model, IModelProviderEvent.MODELS_CHANGED);
		}
	}

	private void handleLocalizationFileChange(ResourceChange change) {
		// reset bundle resource if localization file has changed.
		IPluginModelBase model = fState.getModel(change.getResource().getProject());
		if (model != null) {
			((AbstractNLModel) model).resetNLResourceHelper();
		}
	}

	private void handleSchemaFileChange(ResourceChange change) {
		IFile schemaFile = change.getResource();
		// We are only interested in schema files whose contents have changed
		if (change.getKind() != IResourceDelta.CHANGED)
			return;
		// Get the schema preview file session property
		Object property = null;
		try {
			property = schemaFile.getSessionProperty(PDECore.SCHEMA_PREVIEW_FILE);
		} catch (CoreException e) {
			// Ignore
			return;
		}
		// Check if the schema file has an associated HTML schema preview file
		// (That is, whether a show description action has been executed before)
		// Property set in
		// org.eclipse.pde.internal.ui.search.ShowDescriptionAction.linkPreviewFileToSchemaFile()
		if (property == null) {
			return;
		} else if ((property instanceof File) == false) {
			return;
		}
		File schemaPreviewFile = (File) property;
		// Ensure the file exists and is writable
		if (schemaPreviewFile.exists() == false) {
			return;
		} else if (schemaPreviewFile.isFile() == false) {
			return;
		} else if (schemaPreviewFile.canWrite() == false) {
			return;
		}
		// Get the schema model object
		ISchemaDescriptor descriptor = new SchemaDescriptor(schemaFile, false);
		ISchema schema = descriptor.getSchema(false);

		try {
			// Re-generate the schema preview file contents in order to reflect
			// the changes in the schema
			recreateSchemaPreviewFileContents(schemaPreviewFile, schema);
		} catch (IOException e) {
			// Ignore
		}
	}

	private void recreateSchemaPreviewFileContents(File schemaPreviewFile, ISchema schema) throws IOException {
		SchemaTransformer transformer = new SchemaTransformer();
		OutputStream os = new FileOutputStream(schemaPreviewFile);
		PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, ICoreConstants.UTF_8), true);
		transformer.transform(schema, printWriter);
		os.flush();
		os.close();
	}

}
