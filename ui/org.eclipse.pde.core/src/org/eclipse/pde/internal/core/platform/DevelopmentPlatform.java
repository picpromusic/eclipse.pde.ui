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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;

public class DevelopmentPlatform implements IDevelopmentPlatform {

	private PDEState fState;
	private PluginModelManager fPluginModelManager;
	private FeatureModelManager fFeatureModelManager;
	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;
	private TracingOptionsManager fTracingOptionsManager;
	private PDEExtensionRegistry fExtensionRegistry;
	private SchemaRegistry fSchemaRegistry;
	private ITargetDefinition fTargetDefinition;

	public DevelopmentPlatform(ITargetDefinition targetDefinition) {
		fTargetDefinition = targetDefinition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#isInitialized()
	 */
	public boolean isInitialized() {
		// The state is initialized last
		return fState != null;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getTargetDefinition()
	 */
	public ITargetDefinition getTargetDefinition() {
		return fTargetDefinition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getState()
	 */
	public PDEState getState() {
		return fState;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getPluginModelManager()
	 */
	public PluginModelManager getPluginModelManager() {
		return fPluginModelManager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getFeatureModelManager()
	 */
	public FeatureModelManager getFeatureModelManager() {
		return fFeatureModelManager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getExtensionRegistry()
	 */
	public PDEExtensionRegistry getExtensionRegistry() {
		return fExtensionRegistry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getJavadocLocationManager()
	 */
	public JavadocLocationManager getJavadocLocationManager() {
		return fJavadocLocationManager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getSourceLocationManager()
	 */
	public SourceLocationManager getSourceLocationManager() {
		return fSourceLocationManager;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getSchemaRegistry()
	 */
	public SchemaRegistry getSchemaRegistry() {
		return fSchemaRegistry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#getTracingOptionsManager()
	 */
	public TracingOptionsManager getTracingOptionsManager() {
		return fTracingOptionsManager;
	}

	public void resolve(IProgressMonitor monitor) throws CoreException {
		// TODO Should we return a multi status instead of handling it ourselves
		// TODO Support tracing statements here
		// TODO Remember to clean up strings
		// TODO handleReload used to remove duplicates, is this broken now?
		// TODO Someone has to save the workspace target handle preference
		SubMonitor subMon = SubMonitor.convert(monitor, "Resolving development platform", 1000);
		try {
			// Reset any managers and clear listener lists
			clearManagers();

			// TODO If using the default target, load a target definition?
//			if (fTargetDefinition == null) {
//				ITargetPlatformService targetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
//				if (targetService == null) {
//					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Unable to acquire target platform service"));
//				}
//				fTargetDefinition = targetService.newDefaultTarget();
//			}
//			if (subMon.isCanceled()) {
//				return;
//			}

			// Resolve the target definition
			subMon.subTask("Resolving target definition");
			IStatus status = null;
			if (!fTargetDefinition.isResolved()) {
				status = fTargetDefinition.resolve(subMon.newChild(200));
			}
			subMon.setWorkRemaining(800);
			if (subMon.isCanceled()) {
				return;
			}

			// Collect bundles from the target
			List targetURLs = new ArrayList();
			if (status != null && status.getSeverity() == IStatus.ERROR) {
				// Log the status and assume there are no target bundles
				PDECore.log(status);
			} else {
				TargetBundle[] targetBundles = fTargetDefinition.getBundles();
				for (int i = 0; i < targetBundles.length; i++) {
					BundleInfo currentBundle = targetBundles[i].getBundleInfo();
					File bundleLocation = org.eclipse.core.runtime.URIUtil.toFile(currentBundle.getLocation());
					if (bundleLocation == null) {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind("Could not open plug-in at: {0}", currentBundle.getLocation())));
					} else {
						targetURLs.add(bundleLocation);
					}
				}
			}
			subMon.setWorkRemaining(750);
			if (subMon.isCanceled()) {
				return;
			}

			// Collect bundles from the workspace
			ArrayList workspaceURLs = new ArrayList();
			IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
			for (int i = 0; i < projects.length; i++) {
				if (WorkspaceModelManager.isPluginProject(projects[i])) {
					try {
						IPath path = projects[i].getLocation();
						if (path != null) {
							workspaceURLs.add(path.toFile().toURL());
						}
					} catch (MalformedURLException e) {
					}
				}
			}
			subMon.setWorkRemaining(700);
			if (subMon.isCanceled()) {
				return;
			}

			// Create the state
			subMon.subTask("Create plug-in models and OSGi state");
			PDEState state = new PDEState(getPluginPaths(), (URL[]) targetURLs.toArray(new URL[targetURLs.size()]), subMon.newChild(300));
			subMon.setWorkRemaining(400);
			if (subMon.isCanceled()) {
				return;
			}

			// TODO Create plug-in model manager
			PluginModelManager pluginManager = new org.eclipse.pde.internal.core.platform.PluginModelManager();
			pluginManager.initialize(subMon.newChild(50));
			subMon.setWorkRemaining(350);
			if (subMon.isCanceled()) {
				return;
			}

			// TODO Create feature model manager
			// TODO Filter features by included
			subMon.subTask("Create feature models");
			FeatureModelManager featureManager = new FeatureModelManager();
			featureManager.initialize(subMon.newChild(100));
			subMon.setWorkRemaining(250);
			if (subMon.isCanceled()) {
				return;
			}

			// Create managers and final cleanup
			subMon.subTask("Create additional managers");
			loadJRE(fTargetDefinition);

			// TODO We can't use the preferences anymore
			EclipseHomeInitializer.resetEclipseHomeVariable();

			// TODO Clean extracted libraries
			// TODO Look at better ways of clearning the extracted libs, this was part of external model manager
			ExternalLibraryCache.getInstance().cleanExtractedLibraries(pluginManager.getExternalModels());

			fState = state;
			fPluginModelManager = pluginManager;
			fFeatureModelManager = featureManager;
			createManagers();

		} finally {
			subMon.done();
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	private void clearManagers() {
		// TODO Do we need to dispose the state?
		fState = null;
		if (fPluginModelManager != null) {
			fPluginModelManager.dispose();
			fPluginModelManager = null;
		}
		if (fFeatureModelManager != null) {
			fFeatureModelManager.dispose();
			fFeatureModelManager = null;
		}
		// TODO Still accessing static managers
		if (fExtensionRegistry != null) {
			fExtensionRegistry.stop();
			fExtensionRegistry = null;
		}
		// TODO Still accessing static managers
		fSchemaRegistry = null;
		// TODO Still accessing static managers
		fTracingOptionsManager = null;

		fSourceLocationManager = null;
		fJavadocLocationManager = null;
	}

	/**
	 * Sets the workspace default JRE based on the target's JRE container.
	 *
	 * @param definition target to get jre from
	 */
	private void loadJRE(ITargetDefinition definition) {
		if (definition != null) {
			IPath container = definition.getJREContainer();
			if (container != null) {
				IVMInstall jre = JavaRuntime.getVMInstall(container);
				if (jre != null) {
					IVMInstall def = JavaRuntime.getDefaultVMInstall();
					if (!jre.equals(def)) {
						try {
							JavaRuntime.setDefaultVMInstall(jre, null);
						} catch (CoreException e) {
							PDECore.log(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates the additional managers that will not finish initializing until they
	 * are first called.
	 */
	private void createManagers() {
		fSourceLocationManager = new SourceLocationManager(this);
		fJavadocLocationManager = new JavadocLocationManager(this);
		fTracingOptionsManager = new TracingOptionsManager();
		fSchemaRegistry = new SchemaRegistry();
		fExtensionRegistry = new PDEExtensionRegistry();
	}

	/**
	 * Return URLs to projects in the workspace that have a manifest file (MANIFEST.MF
	 * or plugin.xml)
	 * 
	 * @return an array of URLs to workspace plug-ins
	 */
	private URL[] getPluginPaths() {
		ArrayList list = new ArrayList();
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (WorkspaceModelManager.isPluginProject(projects[i])) {
				try {
					IPath path = projects[i].getLocation();
					if (path != null) {
						list.add(path.toFile().toURL());
					}
				} catch (MalformedURLException e) {
				}
			}
		}
		return (URL[]) list.toArray(new URL[list.size()]);
	}

	/**
	 * Sets the TARGET_PROFILE preference which stores the ID of the target profile used 
	 * (if based on an target extension) or the workspace location of the file that
	 * was used. For now we just clear it.
	 * <p>
	 * Sets the WORKSPACE_TARGET_HANDLE.
	 * </p>
	 * @param pref
	 */
//	private void loadAdditionalPreferences(PDEPreferencesManager pref) throws CoreException {
//		pref.setValue(ICoreConstants.TARGET_PROFILE, ""); //$NON-NLS-1$
//		String memento = fTarget.getHandle().getMemento();
//		if (fNone) {
//			memento = ICoreConstants.NO_TARGET;
//		}
//		pref.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, memento);
//		ITargetLocation[] containers = fTarget.getTargetLocations();
//		boolean profile = false;
//		if (containers != null && containers.length > 0) {
//			profile = containers[0] instanceof ProfileBundleContainer;
//		}
//		pref.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, profile);
//	}

}
