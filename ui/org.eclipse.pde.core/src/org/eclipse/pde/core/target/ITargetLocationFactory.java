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

/**
 * The extension point org.eclipse.pde.core.targetLocations asks for a 
 * target location factory that would build the location objects for the 
 * associated target location type
 * @since 3.8
 *
 */
public interface ITargetLocationFactory {

	/**
	 * Prepares the instance of <code>ITargetLocation</code> for the contributing target location type
	 * @param type	target location identifier
	 * @param location	location tag from the target definition XML
	 * @return
	 * 		instance of <code>ITargetLocation</code> 
	 */
	public ITargetLocation getTargetLocation(String location);

	/**
	 * Provides the description for the target location
	 * @param type	target location identifier
	 * @return	description for the target location
	 */
	public String getDescription();

}
