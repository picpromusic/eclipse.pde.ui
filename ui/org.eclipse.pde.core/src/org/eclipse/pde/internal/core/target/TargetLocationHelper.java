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
package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.Element;

/**
 * Keeps a track of the contributed Target Locations and provides helper functions to 
 * access them
 *
 */
public class TargetLocationHelper {

	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_LOCFACTORY = "locationFactory"; //$NON-NLS-1$
	private static final String TARGET_LOC_EXTPT = "targetLocations"; //$NON-NLS-1$

	static Map fExtentionMap;
	static Map fFactoryMap;
	static TargetLocationHelper INSTANCE;

	private TargetLocationHelper() {
		//singleton
	}

	public static TargetLocationHelper getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TargetLocationHelper();
			fExtentionMap = new HashMap();
			fFactoryMap = new HashMap();

			readExtentions();
		}
		return INSTANCE;
	}

	/**
	 * Prepares the instance of <code>ITargetLocation</code> using the contributed factory for the given type
	 * @param type	target location identifier
	 * @param locationElement	location tag from the target definition XML
	 * @return
	 * 		instance of <code>ITargetLocation</code> 
	 */
	public ITargetLocation getTargetLocation(String type, Element locationElement) {
		ITargetLocationFactory factory = getFactory(type);
		if (factory != null) {
			return factory.getTargetLocation(locationElement.toString());
		}
		return null;
	}

	private ITargetLocationFactory getFactory(String type) {
		ITargetLocationFactory factory = (ITargetLocationFactory) fFactoryMap.get(type);
		if (factory == null) {
			IConfigurationElement extension = (IConfigurationElement) fExtentionMap.get(type);
			if (extension != null) {
				factory = (ITargetLocationFactory) createExecutableExtension(extension);
				if (factory != null) {
					fFactoryMap.put(type, factory);
					return factory;
				}
			}
		}
		return factory;
	}

	private Object createExecutableExtension(IConfigurationElement element) {
		try {
			return element.createExecutableExtension(ATTR_LOCFACTORY);
		} catch (CoreException e) {
			return null;
		}
	}

	private static void readExtentions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDECore.PLUGIN_ID, TARGET_LOC_EXTPT);
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

	/**
	 * Provides the description for the target location
	 * @param type	target location identifier
	 * @return	description for the target location
	 */
	public String getDescription(String type) {
		ITargetLocationFactory factory = getFactory(type);
		return factory.getDescription();
	}
}
