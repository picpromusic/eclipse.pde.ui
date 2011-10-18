package org.eclipse.pde.internal.core.platform;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;

public class DevelopmentPlatform implements IDevelopmentPlatform {

	private State fState;
	private ITargetDefinition fTargetDefinition;
	private IBundle[] fAllBundles;

	public DevelopmentPlatform() {
	}

	public DevelopmentPlatform(ITargetDefinition targetDefinition) {
		fTargetDefinition = targetDefinition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.platform.IDevelopmentPlatform#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void resolve(IProgressMonitor monitor) throws CoreException {

		// TODO Support tracing statements here

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

		// Create the state
		// TODO Does this need to be a field?
		StateObjectFactory stateFactory = Platform.getPlatformAdmin().getFactory();
		State state = stateFactory.createState(true);

		// Add target bundles to the state
		IResolvedBundle[] targetBundles = target.getBundles();
		for (int i = 0; i < targetBundles.length; i++) {
			targetBundles[i].getBundleInfo().getLocation();
		}

		// TODO Need to collect platform properties for the state as MinimalState does

		// TODO Is it faster to always create the state or save/load it for restarts

		// Handle feature models
		IFeatureModel[] targetFeatures = target.getAllFeatures();

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
	public State getState() {
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

}
