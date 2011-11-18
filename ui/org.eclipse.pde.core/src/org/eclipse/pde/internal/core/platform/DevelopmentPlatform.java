package org.eclipse.pde.internal.core.platform;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.PDECore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;

public class DevelopmentPlatform implements IDevelopmentPlatform {

	private PDEState fState;
	private PluginModelManager fPluginModelManager;
	private FeatureModelManager fFeatureModelManager;
	private ITargetDefinition fTargetDefinition;

//	public static IPluginModelBase[] createModels(URL[] bundleFiles){
//		
//	}

	public DevelopmentPlatform(ITargetDefinition targetDefinition) {
		fTargetDefinition = targetDefinition;
	}

	public void initialize(IProgressMonitor monitor) throws CoreException {
		// TODO Should we return a multi status instead of handling it ourselves
		// TODO Support tracing statements here
		SubMonitor subMon = SubMonitor.convert(monitor, "Resolving development platform", 1000);

		// Create the state
		fState = null;

		// If using the default target, load a target definition
		if (fTargetDefinition == null) {
			ITargetPlatformService targetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (targetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Unable to acquire target platform service"));
			}
			fTargetDefinition = ((TargetPlatformService) targetService).newDefaultTargetDefinition();
		}

		if (subMon.isCanceled()) {
			return;
		}

		// Resolve the target bundles
		List targetURLs = new ArrayList();
		subMon.subTask("Resolving target definition");
		IStatus status = null;
		if (!fTargetDefinition.isResolved()) {
			status = fTargetDefinition.resolve(subMon.newChild(100));
		}
		subMon.setWorkRemaining(900);
		
		if (status != null && status.getSeverity() == IStatus.ERROR){
			// Log the status and assume there are no target bundles
			PDECore.log(status);
		} else {
			// Add target bundles to the state
			IResolvedBundle[] targetBundles = fTargetDefinition.getBundles();
			for (int i = 0; i < targetBundles.length; i++) {
				BundleInfo currentBundle = targetBundles[i].getBundleInfo();
				File bundleLocation = org.eclipse.core.runtime.URIUtil.toFile(currentBundle.getLocation());
				if (bundleLocation == null) {
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind("Could not open plug-in at: {0}", currentBundle.getLocation())));
				} else {
					targetURLs.add(bundleLocation);
				}
//			try {
//				Dictionary currentManifest = ManifestHelper.loadManifest(bundleLocation);
//				TargetWeaver.weaveManifest(currentManifest);
//				BundleDescription newBundle = stateFactory.createBundleDescription(state, currentManifest, bundleLocation.getAbsolutePath(), getNextId());
//				state.addBundle(newBundle);
//			} catch (IOException e) {
//				PDECore.log(e);
//			} catch (BundleException e) {
//				PDECore.log(e);
//			}
		}
			
		// Resolve workspace bundles
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

		// Resolve the state
		// Create a state that contains all bundles from the target and workspace
		// If a workspace bundle has the same symbolic name as a target bundle,
		// the target counterpart is subsequently removed from the state.

		fState = new PDEState(getPluginPaths(), (URL[]) targetURLs.toArray(new URL[targetURLs.size()]), true, true, subMon.newChild(100));

		fState.getWorkspaceModels();
		fState.getTargetModels();

		// Create plug-in models

		// Handle feature models
		IFeatureModel[] targetFeatures = target.getAllFeatures();

	}

	/**
	 * Returns the next available bundle id for bundles being added to a state.
	 * 
	 * @return next available bundle id
	 */
//	private long getNextId() {
//		return ++fBundleId;
//	}

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
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#findBundle(java.lang.String, java.lang.String)
	 */
	public IBundle findBundle(String name, String version) {
		if (fAllBundles == null || fAllBundles.length == 0) {
			return null;
		}
		return fAllBundles[0];
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

}
