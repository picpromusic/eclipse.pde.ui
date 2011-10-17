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

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.graphics.Image;

/**
 * Adaptor factory for providing label and content providers and Add/Edit Wizards for the IU Target Location type
 *
 */
public class IUFactory implements ILocationUIFactory {

	ITargetDefinition fTarget;

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof String) {
			if (IUBundleContainer.TYPE.equals(adaptableObject)) {
				if (adapterType == ILabelProvider.class) {
					return new IULabelProvider();
				} else if (adapterType == ITreeContentProvider.class) {
					if (fTarget != null) {
						return new IUContentProvider(fTarget);
					}
				} else if (adapterType == ILocationWizard.class) {
					return new InstallableUnitWizard(fTarget);
				}
			}
		} else if (adaptableObject instanceof IUBundleContainer) {
			if (adapterType == IEditTargetLocationPage.class) {
				return new EditIUContainerPage((IUBundleContainer) adaptableObject, fTarget);
			}
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] {ILabelProvider.class, ITreeContentProvider.class, ILocationWizard.class, IEditTargetLocationPage.class};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ILocationUIFactory#setTargetDefinition(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	public void setTargetDefinition(ITargetDefinition target) {
		fTarget = target;
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
