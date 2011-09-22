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

import org.eclipse.core.runtime.CoreException;

/**
 * A factory to instantiate target locations from a serialized string of xml 
 * (see {@link ITargetLocation#serialize()}). A factory must be provided for
 * each {@link ITargetLocation} type using the <code>org.eclipse.pde.core.targetLocations</code>
 * extension point.
 * 
 * @since 3.8
 */
public interface ITargetLocationFactory {

	/**
	 * Returns an instance of an {@link ITargetLocation} from the provided serialized xml string
	 * or throws a {@link CoreException} if unable to do so.
	 * 
	 * @param type the string type describing the implementation of ITargetLocation expected, see {@link ITargetLocation#getType()}
	 * @param serializedXML	the xml string describing the location to create, see {@link ITargetLocation#serialize()}
	 * @return an instance of <code>ITargetLocation</code>
	 * @throws CoreException if this factory cannot create a location for the specified type or if the xml string is invalid 
	 */
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException;

	/**
	 * Returns a string description for the provided target location type or <code>null</code> if the
	 * type is not supported by this factory.
	 * 
	 * @param type the string type describing the implementation of ITargetLocation to get the description for, see {@link ITargetLocation#getType()}
	 * @return a description for the target location or <code>null</code>
	 */
	public String getDescription(String type);

}
