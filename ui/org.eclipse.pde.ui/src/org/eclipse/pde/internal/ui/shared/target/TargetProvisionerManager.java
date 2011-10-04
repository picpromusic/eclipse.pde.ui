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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.IProvisionerWizard;

/**
 * Gathers and provides the contributed target provisioner contributed to org.eclipse.pde.ui.targetProvisioner
 *
 */
public class TargetProvisionerManager {

	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String TARGET_PROVISIONER_EXTPT = "targetProvisioners"; //$NON-NLS-1$

	private Map fExtentionMap;
	private Map fWizardMap;

	private static TargetProvisionerManager INSTANCE;

	private TargetProvisionerManager() {
		//singleton
		fExtentionMap = new HashMap(4);
		fWizardMap = new HashMap(4);
		readExtentions();
	}

	/**
	 * @return the singleton instanceof this manager
	 */
	public static TargetProvisionerManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TargetProvisionerManager();
		}
		return INSTANCE;
	}

	/**
	 * Returns the label provider contributed for the given type using the org.eclipse.pde.ui.targetProvisioner
	 * extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return label provider contributed for the provided type
	 */
	public ILabelProvider getLabelProvider(String type) {
		IProvisionerWizard wizard = getWizard(type);
		if (wizard != null) {
			return wizard.getLabelProvider();
		}
		return null;
	}

	/**
	 * Returns the content provider contributed for the given type using the org.eclipse.pde.ui.targetProvisioner
	 * extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return content provider contributed for the provided type
	 */
	public ITreeContentProvider getContentProvider(String type) {
		IProvisionerWizard wizard = getWizard(type);
		if (wizard != null) {
			return wizard.getContentProvider();
		}
		return null;
	}

	private IProvisionerWizard getWizard(String type) {
		IProvisionerWizard wizard = (IProvisionerWizard) fWizardMap.get(type);
		if (wizard == null) {
			try {
				IConfigurationElement element = (IConfigurationElement) fExtentionMap.get(type);
				if (element != null) {
					wizard = (IProvisionerWizard) element.createExecutableExtension(ATTR_CLASS);
					fWizardMap.put(type, wizard);
				}
			} catch (CoreException e) {
			}
		}
		return wizard;
	}

	private void readExtentions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), TARGET_PROVISIONER_EXTPT);
		if (point == null)
			return;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				String type = elements[j].getAttribute(ATTR_TYPE);
				if (type != null) {
					fExtentionMap.put(type, elements[j]);
				}
			}
		}
	}

}
