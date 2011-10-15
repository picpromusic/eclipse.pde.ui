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
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.ITargetLocationProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Wizard for selecting Installable Units. 
 * 
 * Contributed to provide UI for <code>IUBundleContainer</code> target location through extension to 
 * org.eclipse.pde.ui.targetProvisioner 
 *
 */
public class InstallableUnitWizard extends Wizard implements ITargetLocationProvider {

	private ITargetDefinition fTarget;

	private ITargetLocation fLocation;

	private IULabelProvider fLabelProvider;

	private IUContentProvider fContentProvider;

	/**
	 * Section in the dialog settings for this wizard and the wizards created with selection
	 * Shared with the EditBundleContainerWizard
	 */
	static final String SETTINGS_SECTION = "editBundleContainerWizard"; //$NON-NLS-1$

	public InstallableUnitWizard() {
		setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		}
		setDialogSettings(settings);
		addPage(new EditIUContainerPage(fTarget));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		fLocation = ((EditIUContainerPage) getPages()[0]).getBundleContainer();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ITargetLocationProvider#setTargetDefinition(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	public void setTargetDefinition(ITargetDefinition target) {
		fTarget = target;
		fContentProvider = null; // nullify content provider so that it does not contain stale target	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ITargetLocationProvider#getLocations()
	 */
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] {fLocation};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ITargetLocationProvider#getLabelProvider()
	 */
	public ILabelProvider getLabelProvider() {
		if (fLabelProvider == null) {
			fLabelProvider = new IULabelProvider();
		}
		return fLabelProvider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ITargetLocationProvider#getContentProvider()
	 */
	public ITreeContentProvider getContentProvider() {
		if (fContentProvider == null) {
			fContentProvider = new IUContentProvider(fTarget);
		}
		return fContentProvider;
	}

	/**
	 * Label Provider for the  {@link IUBundleContainer} target location
	 *
	 */
	class IULabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		StyledBundleLabelProvider provider;

		IULabelProvider() {
			provider = new StyledBundleLabelProvider(true, false);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return provider.getImage(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return provider.getText(element);
		}

	}
}
