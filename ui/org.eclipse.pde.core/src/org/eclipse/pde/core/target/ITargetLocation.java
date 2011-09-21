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
package org.eclipse.pde.core.target;

import org.eclipse.core.runtime.*;

/**
 * A collection of bundles. A bundle location abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.8
 */
public interface ITargetLocation extends IAdaptable {

	/**
	 * Resolves all bundles in this location in the context of the specified
	 * target and returns a status describing the resolution.
	 * <p>
	 * If there is an error preventing the resolution a status detailing
	 * the error will be returned.  If resolution is successful an OK status is 
	 * returned. If the progress monitor is canceled a CANCEL status will be returned.
	 * </p><p>
	 * Note that the returned status may differ from the result of calling 
	 * {@link #getStatus()}
	 * </p>
	 * @param definition target being resolved for
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolution status
	 * @exception CoreException if unable to resolve this location
	 */
	public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor);

	/**
	 * Returns the status of the last bundle resolution or <code>null</code> if 
	 * this location has not been resolved.  If there was a problem during the 
	 * resolution, a status, possibly a multi-status explaining the problem will be 
	 * returned, see {@link #resolve(ITargetDefinition, IProgressMonitor)}. 
	 * 	 
	 * @see ITargetLocation#getBundles()
	 * @return single resolution status or <code>null</code>
	 */
	public IStatus getStatus();

	/**
	 * Returns whether this location has resolved all of its contained bundles.
	 * 
	 * @see #resolve(ITargetDefinition, IProgressMonitor)
	 * @return whether this location has resolved all of its contained bundles
	 */
	public boolean isResolved();

	/**
	 * Returns the bundles in this location or <code>null</code> if this location is not resolved
	 * <p>
	 * Some of the returned bundles may have non-OK statuses.  These bundles may be missing some
	 * information (location, version, source target).  To get a bundle's status call
	 * {@link TargetBundle#getStatus()}.  You can also use {@link #getStatus()} to
	 * get the complete set of problems.
	 * </p>
	 * @return resolved bundles or <code>null</code>
	 */
	public TargetBundle[] getBundles();

	/**
	 * Returns all features available in this location or <code>null</code> if this location is
	 * not resolved.
	 * <p>
	 * This method may return no features, even if the location has multiple bundles.  For all
	 * returned features, the bundles that the features reference should be returned in the list
	 * returned by {@link #getBundles()}
	 * </p>
	 * @return features or <code>null</code>
	 */
	public TargetFeature[] getFeatures();

	/**
	 * Returns VM Arguments that are specified in the bundle location or <code>null</code> if none.
	 * 
	 * @return list of VM Arguments or <code>null</code> if none available
	 */
	public String[] getVMArguments();

	/**
	 * Returns a string that identifies the target location.  This type is persisted to xml
	 * so that the correct target location is created when deserializing the xml.  This type is also
	 * used to alter how the containers are presented to the user in the UI.
	 * 
	 * @return string identifier for the type of target location.
	 */
	public String getType();

	/**
	 * Returns a path in the local file system to the root of the target location.
	 * <p>
	 * Currently the PDE target platform preferences are
	 * based on a home location and additional locations, so we need the information.
	 * </p>
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public String getLocation(boolean resolve) throws CoreException;

	/**
	 * Save the location attributes
	 * @return
	 * 		an XML string representing the location
	 */
	public String serialize();
}
