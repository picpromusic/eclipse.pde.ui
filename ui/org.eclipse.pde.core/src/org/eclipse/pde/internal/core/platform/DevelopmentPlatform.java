package org.eclipse.pde.internal.core.platform;

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
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;

public class DevelopmentPlatform implements IDevelopmentPlatform {

	private PDEState fState;
	private ITargetDefinition fTargetDefinition;
	private IBundle[] fAllBundles;

//	private long fBundleId;

	public DevelopmentPlatform() {
	}

	public DevelopmentPlatform(ITargetDefinition targetDefinition) {
		fTargetDefinition = targetDefinition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void resolve(IProgressMonitor monitor) throws CoreException {
		// TODO Should we return a multi status instead of handling it ourselves
		// TODO Support tracing statements here

		// Create the state
		fState = null;
		// TODO Does this need to be a field?
//		StateObjectFactory stateFactory = Platform.getPlatformAdmin().getFactory();
//		State state = stateFactory.createState(true);

		ITargetDefinition target = fTargetDefinition;
		SubMonitor subMon = SubMonitor.convert(monitor, "Resolving development platform", 1000);

		// If using the default target, load a target definition
		if (target == null) {
			ITargetPlatformService targetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (targetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Unable to acquire target platform service"));
			}
			target = ((TargetPlatformService) targetService).newDefaultTargetDefinition();
		}

		if (subMon.isCanceled()) {
			return;
		}

		// Resolve the target
		if (!target.isResolved()) {
			target.resolve(subMon.newChild(100));
		}
		subMon.setWorkRemaining(900);

		// Add target bundles to the state
		List targetURLs = new ArrayList();
		IResolvedBundle[] targetBundles = target.getBundles();
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

		// TODO Need to collect platform properties for the state as MinimalState does

		// TODO Is it faster to always create the state or save/load it for restarts

		// TODO We were caching the workspace models in the state, see PluginModelManager#initializeTable()

		// Add workspace bundles to the state

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
