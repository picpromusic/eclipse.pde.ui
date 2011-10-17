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
package org.eclipse.pde.ui;

import org.eclipse.pde.core.target.ITargetLocation;

/**
 * This interface represents a wizard which will be used to add plug-ins to 
 * the Target Platform.  Typically, it maps to one wizard page, but more
 * complex sections may span several pages. Also note that in the very simple
 * cases it may not contribute any wizard pages.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */

public interface ILocationWizard extends IBasePluginWizard {

	/**
	 * Returns an array of target locations which contain plug-ins to be added to
	 * the Target Platform.
	 * 
	 * @return an array that represent the locations that will provide new plug-ins.
	 */
	public ITargetLocation[] getLocations();

}
