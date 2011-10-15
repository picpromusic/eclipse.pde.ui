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
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.ui.shared.target.TargetLocationsGroup.TargetLocationElement;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the tree, primary input is a ITargetDefinition, children are ITargetLocation
 */
public class TargetLocationLabelProvider extends StyledBundleLabelProvider {

	private ITargetDefinition fTarget;

	public TargetLocationLabelProvider(boolean showVersion, boolean appendResolvedVariables, ITargetDefinition target) {
		super(showVersion, appendResolvedVariables);
		fTarget = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (!(element instanceof AbstractBundleContainer) && (element instanceof ITargetLocation)) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider instanceof StyledCellLabelProvider) {
				((StyledCellLabelProvider) provider).update(cell);
			}
			cell.setText(getText(element));
			cell.setImage(getImage(element));
		} else {
			super.update(cell);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof AbstractBundleContainer) {
			return super.getImage(element);
		} else if (element instanceof ITargetLocation) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider != null) {
				return provider.getImage(element);
			}
		} else if (element instanceof TargetLocationElement) {
			ITargetLocation location = ((TargetLocationElement) element).getParent();
			ILabelProvider provider = getLabelProvider(location);
			if (provider != null) {
				return provider.getImage(element);
			}
		}
		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof AbstractBundleContainer) {
			return super.getText(element);
		} else if (element instanceof ITargetLocation) {
			ILabelProvider provider = getLabelProvider((ITargetLocation) element);
			if (provider != null) {
				return provider.getText(element);
			}
		}
		return super.getText(element);
	}

	private ILabelProvider getLabelProvider(ITargetLocation location) {
		return TargetProvisionerManager.getInstance(fTarget).getLabelProvider(location.getType());
	}
}