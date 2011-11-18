/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.platform;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.PluginModelManager;

/**
 * Internal singleton manager to keep track of all {@link IDevelopmentPlatform}s
 *
 */
public class PlatformManager implements IDevelopmentPlatformService {

	private Map/*<String, IDevelopmentPlatform>*/fPlatforms;

	/**
	 * Service instance
	 */
	private static IDevelopmentPlatformService fgDefault;

	/**
	 * The dev platform service should be obtained by requesting the {@link IDevelopmentPlatformService} from OSGi. This
	 * method should only be used internally be PDE.
	 * 
	 * @return The singleton implementation of this service
	 */
	public synchronized static IDevelopmentPlatformService getDefault() {
		if (fgDefault == null) {
			fgDefault = new PlatformManager();
		}
		return fgDefault;
	}

	private PlatformManager() {
		fPlatforms = new HashMap();
	}

	public void addPlatform(PDEState state) {

	}

	public PluginModelManager getPluginModelManager(String platformId) {
		return ((IDevelopmentPlatform) fPlatforms.get(platformId)).getPluginModelManager();
	}
}
