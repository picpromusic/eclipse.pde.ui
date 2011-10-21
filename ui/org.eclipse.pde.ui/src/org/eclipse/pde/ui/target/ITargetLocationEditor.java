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
package org.eclipse.pde.ui.target;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * Target contents that implement or adapt to this interface can be Target contents such as {@link ITargetLocation} implementations The Target Locations and their children can adapt to this interface to convey if they 
 * support removal and updation
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */
public interface ITargetLocationEditor {

	public boolean canEdit(ITargetDefinition target, ITargetLocation targetLocation);

	public IWizard getEditWizard(ITargetDefinition target, ITargetLocation targetLocation);

}
