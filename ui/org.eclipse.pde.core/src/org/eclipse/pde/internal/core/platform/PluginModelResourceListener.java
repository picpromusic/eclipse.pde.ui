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

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;

/**
 * Resource change listener for the plugin model manager.  When changes happen in the workspace this 
 * listener will visit the delta, make any necessary changes to the models and managers, then updates
 * the plugin model manager with the list of any projects that were added, changed or removed.
 * 
 * @see PluginModelManager
 *
 */
public class PluginModelResourceListener implements IResourceChangeListener {
	private PluginModelManager fModelManager;

	public PluginModelResourceListener(PluginModelManager manager) {
		fModelManager = manager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE :
				if (event.getDelta() != null) {
					try {
						PluginModelResourceDeltaVisitor visitor = new PluginModelResourceDeltaVisitor();
						event.getDelta().accept(visitor);
						fModelManager.workspaceChanged(visitor.getContext());
					} catch (CoreException e) {
						PDECore.logException(e);
					}
				}
				break;
			case IResourceChangeEvent.PRE_CLOSE :
				if (event.getResource() instanceof IProject) {
					WorkspacePluginContext context = new WorkspacePluginContext();
					context.removedProjects = Arrays.asList(new Object[] {event.getResource()});
					// TODO When a model is removed, the extension listeners must be updated too
					fModelManager.workspaceChanged(context);
				}
				break;
		}
	}

	/**
	 * Visits a resource change delta to find out what workspace plug-in models have been
	 * added, changed and removed.
	 */
	class PluginModelResourceDeltaVisitor implements IResourceDeltaVisitor {
		private WorkspacePluginContext fContext = new WorkspacePluginContext();

		/**
		 * @return added {@link IProject}s, possibly <code>null</code>
		 */
		public WorkspacePluginContext getContext() {
			return fContext;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			final IResource resource = delta.getResource();
			if (!resource.isDerived()) {
				switch (resource.getType()) {
					case IResource.ROOT :
						return true;
					case IResource.PROJECT : {
						IProject project = (IProject) resource;
						if (WorkspaceModelManager.isPluginProject(project) && (delta.getKind() == IResourceDelta.ADDED || (delta.getFlags() & IResourceDelta.OPEN) != 0)) {
							if (fContext.addedProjects == null) {
								fContext.addedProjects = new ArrayList();
							}
							fContext.addedProjects.add(project);
							return false;
						} else if (delta.getKind() == IResourceDelta.REMOVED) {
							if (fContext.removedProjects == null) {
								fContext.removedProjects = new ArrayList();
							}
							fContext.removedProjects.add(project);
							return false;
						}
						return true;
					}
					case IResource.FOLDER :
						return isInterestingFolder((IFolder) resource);
					case IResource.FILE :
						// Process new files, removed files and changed files with content change
						if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED || (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0)) {
							handleFileDelta(delta);
						}
						return false;
				}
			}
			return false;
		}

		/**
		 * Returns true if the folder being visited is of interest to PDE.
		 * In this case, PDE is only interested in META-INF folders at the root of the bundle
		 * We are also interested in schema folders.
		 * 
		 * @return <code>true</code> if the folder (and its children) is of interest to PDE;
		 * <code>false</code> otherwise.
		 * 
		 */
		private boolean isInterestingFolder(IFolder folder) {
			IContainer root = PDEProject.getBundleRoot(folder.getProject());
			if (folder.getProjectRelativePath().isPrefixOf(root.getProjectRelativePath())) {
				return true;
			}
			String folderName = folder.getName();
			if (("META-INF".equals(folderName) || "OSGI-INF".equals(folderName) || "schema".equals(folderName)) && folder.getParent().equals(root)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return true;
			}
			if ("OSGI-INF/l10n".equals(folder.getProjectRelativePath().toString())) { //$NON-NLS-1$
				return true;
			}
			return false;
		}

		/**
		 * Reacts to changes in files of interest to PDE
		 */
		private void handleFileDelta(IResourceDelta delta) {
			IFile file = (IFile) delta.getResource();
			IProject project = file.getProject();
			String filename = file.getName();
			if (file.equals(PDEProject.getOptionsFile(project))) {
				if (fContext.changedTracingFiles == null) {
					fContext.changedTracingFiles = new ArrayList();
				}
				fContext.changedTracingFiles.add(new ResourceChange(file, delta.getKind()));
//				PDECore.getDefault().getTracingOptionsManager().reset();
			} else if (file.equals(PDEProject.getBuildProperties(project))) {
				if (fContext.changedBuildProperties == null) {
					fContext.changedBuildProperties = new ArrayList();
				}
				fContext.changedBuildProperties.add(new ResourceChange(file, delta.getKind()));
				// change in build.properties should trigger a Classpath Update
				// we therefore fire a notification
				//TODO this is inefficient.  we could do better.
			} else if (file.equals(PDEProject.getLocalizationFile(project))) {
				if (fContext.changedLocalizationFiles == null) {
					fContext.changedLocalizationFiles = new ArrayList();
				}
				fContext.changedLocalizationFiles.add(new ResourceChange(file, delta.getKind()));

				// reset bundle resource if localization file has changed.
//				IPluginModelBase model = fModelManager.getModel(project);
//				if (model != null) {
//					((AbstractNLModel) model).resetNLResourceHelper();
//				}
			} else if (filename.endsWith(".exsd")) { //$NON-NLS-1$
				if (fContext.changedSchemas == null) {
					fContext.changedSchemas = new ArrayList();
				}
				fContext.changedSchemas.add(new ResourceChange(file, delta.getKind()));

//				handleEclipseSchemaDelta(file, delta);
			} else {
				if (file.equals(PDEProject.getPluginXml(project)) || file.equals(PDEProject.getFragmentXml(project))) {
					if (fContext.changedExtensions == null) {
						fContext.changedExtensions = new ArrayList();
					}
					fContext.changedExtensions.add(new ResourceChange(file, delta.getKind()));

//					handleExtensionFileDelta(file, delta);
				} else if (file.equals(PDEProject.getManifest(project))) {
					if (fContext.changedManifests == null) {
						fContext.changedManifests = new ArrayList();
					}
					fContext.changedManifests.add(new ResourceChange(file, delta.getKind()));

//					handleBundleManifestDelta(file, delta);
				}
			}
		}
	}

	/**
	 * Contains information taken from an {@link IResourceDelta} that is important to the 
	 * {@link PluginModelManager}.  The information is broken down by what was changed. This
	 * class is only intended to be access by {@link PluginModelManager}.
	 * 
	 * @see ResourceChange
	 */
	class WorkspacePluginContext {
		/**
		 * Added {@link IProject}s, possibly <code>null</code>
		 */
		Collection addedProjects;
		/**
		 * Removed or closed {@link IProject}s, possibly <code>null</code>
		 */
		Collection removedProjects;
		/**
		 * {@link ResourceChange}s for changed tracing option files, possibly <code>null</code>
		 */
		Collection changedTracingFiles;
		/**
		 * {@link ResourceChange}s for changed schema files, possibly <code>null</code>
		 */
		Collection changedSchemas;
		/**
		 * {@link ResourceChange}s for changed localization files, possibly <code>null</code>
		 */
		Collection changedLocalizationFiles;
		/**
		 * {@link ResourceChange}s for changed build properties files, possibly <code>null</code>
		 */
		Collection changedBuildProperties;
		/**
		 * {@link ResourceChange}s for changed bundle manifest files, possibly <code>null</code>
		 */
		Collection changedManifests;
		/**
		 * {@link ResourceChange}s for changed extension files (plugin.xml, fragment.xml), possibly <code>null</code>
		 */
		Collection changedExtensions;
	}

	/**
	 * Represents a change to a workspace file.  Stores a minimal amount of information
	 * taken from the resource delta.  This is only intended to be used to pass information
	 * to the {@link PluginModelManager}.
	 * 
	 * @see WorkspacePluginContext
	 * @see ResourceChange
	 */
	class ResourceChange {
		/**
		 * The file being modified
		 */
		private IFile resource;

		/**
		 * One of {@link IResourceDelta#ADDED}, {@link IResourceDelta#REMOVED}, {@link IResourceDelta#CHANGED}
		 * Change types will always be content changes.
		 */
		private int changeKind;

		public ResourceChange(IFile resource, int changeKind) {
			this.resource = resource;
			this.changeKind = changeKind;
		}

		/**
		 * @return the workspace file associated with this change
		 */
		public IFile getResource() {
			return resource;
		}

		/**
		 * @return the kind of change from the delta. One of {@link IResourceDelta#ADDED}, 
		 * {@link IResourceDelta#REMOVED}, {@link IResourceDelta#CHANGED}. If the kind is
		 * {@link IResourceDelta#CHANGED}, a content change will have occurred.
		 */
		public int getKind() {
			return changeKind;
		}

	}

}
