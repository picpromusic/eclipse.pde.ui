/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * This interface represents a wizard which will be used to add plug-ins to 
 * the Target Platform.  Typically, it maps to one wizard page, but more
 * complex sections may span several pages. Also note that in the very simple
 * cases it may not contribute any wizard pages.
 * 
 * @since 3.7
 */

public interface IProvisionerWizard extends IBasePluginWizard {

	/**
	 * Returns an array of locations which contain plug-ins to be added to
	 * the Target Platform.  If a location contains a "plugins" subdirectory,
	 * the subdirectory will be searched for plug-ins.  Otherwise, the location
	 * itself will be searched for new plug-ins.
	 * 
	 * @return an array of Files which represent the locations to search for 
	 * new plug-ins.
	 * 
	 * @since 3.7
	 */
	public ITargetLocation[] getLocations();

	/**
	 * Supplies the target definition to the provisioner. The clients can use it for
	 * creating target locations and content provider, if needed.
	 * 
	 * @param target the Target Definition
	 * 
	 * @since 3.7
	 */
	public void setTargetDefinition(ITargetDefinition target);

	/**
	 * Returns the label provider which will be used to display the name and icon for this target location.
	 * The client may extend {@link StyledCellLabelProvider} to provide styled labels
	 * 
	 * @return the label provider
	 * 
	 * @since 3.7
	 */
	public ILabelProvider getLabelProvider();

	/**
	 * Returns the content provider which will be used to display the target location and its children
	 * 
	 * @return the content provider
	 * 
	 * @since 3.7
	 */
	public ITreeContentProvider getContentProvider();

}
