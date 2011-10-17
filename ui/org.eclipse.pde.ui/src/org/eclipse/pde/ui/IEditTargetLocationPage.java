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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.shared.target.EditBundleContainerWizard;

/**
 * Interface for wizard pages used to edit target locations.
 * 
 * @see EditBundleContainerWizard for reference implementation
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */
public interface IEditTargetLocationPage extends IWizardPage {

	/**
	 * Returns a target location containing edited values taken from the wizard page.
	 * @return target location
	 */
	public ITargetLocation getTargetLocation();

	/**
	 * Informs the wizard page that the wizard is closing and any settings/preferences
	 * should be stored.
	 */
	public void storeSettings();

}
