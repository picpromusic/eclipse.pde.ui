package org.eclipse.pde.internal.core.platform;

import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * Represents a single OSGi bundle in PDE.  May be from the workspace or an external target.
 *
 */
public interface IBundle {

	/**
	 * Returns the OSGi bundle description backing this bundle.
	 * 
	 * @return OSGi bundle description for this bundle
	 */
	public BundleDescription getBundleDescription();
}
