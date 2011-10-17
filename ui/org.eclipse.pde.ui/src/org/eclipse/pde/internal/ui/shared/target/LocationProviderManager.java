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
import org.eclipse.pde.ui.*;

/**
 * Gathers and provides the contributed target provisioner contributed to org.eclipse.pde.ui.targetProvisioner
 *
 */
public class LocationProviderManager {

	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_UIFACTORY = "uifactory"; //$NON-NLS-1$
	private static final String TARGET_PROVISIONER_EXTPT = "targetLocationProviders"; //$NON-NLS-1$

	private static Map fExtentionMap;
	private Map fUIFactoryMap;
	private ITargetDefinition fTarget;

	private static HashMap fManagerMap = new HashMap(4);

	private LocationProviderManager(ITargetDefinition target) {
		//singleton
		fExtentionMap = new HashMap(4);
		fUIFactoryMap = new HashMap(4);
		fTarget = target;
		readExtentions();
	}

	/**
	 * @return the singleton instanceof this manager for the given target
	 */
	public static LocationProviderManager getInstance(ITargetDefinition target) {
		Object manager = fManagerMap.get(target);
		if (manager == null) {
			manager = new LocationProviderManager(target);
			fManagerMap.put(target, manager);
		}
		return (LocationProviderManager) manager;
	}

	/**
	 * Returns the label provider contributed for the given type using the org.eclipse.pde.ui.targetLocationProvider
	 * extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return label provider contributed for the provided type
	 */
	public ILabelProvider getLabelProvider(String type) {
		ILocationUIFactory uiFactory = getUIFactory(type);
		if (uiFactory != null) {
			return (ILabelProvider) uiFactory.getAdapter(type, ILabelProvider.class);
		}
		return (ILabelProvider) Platform.getAdapterManager().getAdapter(type, ILabelProvider.class);
	}

	/**
	 * Returns the content provider contributed for the given type using the org.eclipse.pde.ui.targetLocationProvider
	 * extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return content provider contributed for the provided type
	 */
	public ITreeContentProvider getContentProvider(String type) {
		ILocationUIFactory uiFactory = getUIFactory(type);
		if (uiFactory != null) {
			return (ITreeContentProvider) uiFactory.getAdapter(type, ITreeContentProvider.class);
		}
		return (ITreeContentProvider) Platform.getAdapterManager().getAdapter(type, ITreeContentProvider.class);
	}

	/**
	 * Returns the wizard (to create and add target location) contributed for the given type 
	 * using the org.eclipse.pde.ui.targetLocationProvider extension point.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @return add location wizard contributed for the provided type
	 */
	public ILocationWizard getAddWizard(String type) {
		ILocationUIFactory uiFactory = getUIFactory(type);
		if (uiFactory != null) {
			return (ILocationWizard) uiFactory.getAdapter(type, ILocationWizard.class);
		}
		return (ILocationWizard) Platform.getAdapterManager().getAdapter(type, ILocationWizard.class);
	}

	/**
	 * Returns the wizard page (to edit the target location) contributed for the given target location 
	 * using the org.eclipse.pde.ui.targetLocationProvider extension point.
	 * 
	 * @param location the target location to be edited, see {@link ITargetLocation#getType()}
	 * @return edit location wizard page contributed for the provided type
	 */
	public IEditTargetLocationPage getEditWizardPage(ITargetLocation location) {
		ILocationUIFactory uiFactory = getUIFactory(location.getType());
		if (uiFactory != null) {
			return (IEditTargetLocationPage) uiFactory.getAdapter(location, IEditTargetLocationPage.class);
		}
		return (IEditTargetLocationPage) Platform.getAdapterManager().getAdapter(location, IEditTargetLocationPage.class);
	}

	private ILocationUIFactory getUIFactory(String type) {
		ILocationUIFactory uiFactory = (ILocationUIFactory) fUIFactoryMap.get(type);
		if (uiFactory == null) {
			try {
				IConfigurationElement element = (IConfigurationElement) fExtentionMap.get(type);
				if (element != null) {
					uiFactory = (ILocationUIFactory) element.createExecutableExtension(ATTR_UIFACTORY);
					uiFactory.setTargetDefinition(fTarget);
					fUIFactoryMap.put(type, uiFactory);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
		return uiFactory;
	}

	private static void readExtentions() {
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
