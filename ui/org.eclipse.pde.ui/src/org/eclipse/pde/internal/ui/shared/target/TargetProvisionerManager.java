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
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.ITargetLocationProvider;

/**
 * Gathers and provides the contributed target provisioner contributed to org.eclipse.pde.ui.targetProvisioner
 *
 */
public class TargetProvisionerManager {

	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String TARGET_PROVISIONER_EXTPT = "targetLocationProviders"; //$NON-NLS-1$

	private Map fExtentionMap;
	private Map fProviderMap;
	private ITargetDefinition fTarget;

	private static HashMap fManagerMap = new HashMap(4);

	private TargetProvisionerManager(ITargetDefinition target) {
		//singleton
		fExtentionMap = new HashMap(4);
		fProviderMap = new HashMap(4);
		fTarget = target;
		readExtentions();
	}

	/**
	 * @return the singleton instanceof this manager
	 */
	public static TargetProvisionerManager getInstance(ITargetDefinition target) {
		Object manager = fManagerMap.get(target);
		if (manager == null) {
			manager = new TargetProvisionerManager(target);
			fManagerMap.put(target, manager);
		}
		return (TargetProvisionerManager) manager;
	}

	/**
	 * Returns the label provider contributed for the given type using the org.eclipse.pde.ui.targetProvisioner
	 * extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return label provider contributed for the provided type
	 */
	public ILabelProvider getLabelProvider(String type) {
		ITargetLocationProvider locationProvider = getLocationProvider(type);
		if (locationProvider != null) {
			return locationProvider.getLabelProvider();
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
		ITargetLocationProvider lcoationProvider = getLocationProvider(type);
		if (lcoationProvider != null) {
			return lcoationProvider.getContentProvider();
		}
		return null;
	}

	public ITargetLocationProvider getLocationProvider(String type) {
		ITargetLocationProvider locationProvider = (ITargetLocationProvider) fProviderMap.get(type);
		if (locationProvider == null) {
			try {
				IConfigurationElement element = (IConfigurationElement) fExtentionMap.get(type);
				if (element != null) {
					locationProvider = (ITargetLocationProvider) element.createExecutableExtension(ATTR_CLASS);
					locationProvider.setTargetDefinition(fTarget);
					fProviderMap.put(type, locationProvider);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
		return locationProvider;
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
