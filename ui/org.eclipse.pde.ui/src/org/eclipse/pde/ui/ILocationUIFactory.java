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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.core.target.ITargetDefinition;

/**
 * This interface represents a adapter factory which will provide the label and content providers 
 * and add/edit wizard for a given target location type. This interface will seed the factory with
 * the target definition. If the target location does not need the target definition, the facory
 * can be directly contributed using the org.eclipse.core.runtime.adapters extension point
 * 
 * @since 3.7
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILocationUIFactory extends IAdapterFactory {

	/**
	 * Supplies the target definition. The clients can use it for
	 * creating the wizards and content provider, if needed.
	 * 
	 * @param target the Target Definition
	 */
	public void setTargetDefinition(ITargetDefinition target);

}
