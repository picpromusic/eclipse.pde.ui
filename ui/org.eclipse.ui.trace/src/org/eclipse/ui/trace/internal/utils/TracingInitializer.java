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
package org.eclipse.ui.trace.internal.utils;

import java.io.File;
import java.util.Map;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.ui.IStartup;

/**
 * Reads the preferences and initialises the {@link DebugOptions} options
 * 
 * @since 3.6
 */
public class TracingInitializer implements IStartup {

	public void earlyStartup() {
		if (PreferenceHandler.isTracingEnabled()) {
			DebugOptionsHandler.setDebugEnabled(true);
			DebugOptionsHandler.getDebugOptions().setFile(new File(PreferenceHandler.getFilePath()));
			Map<String, String> prefs = PreferenceHandler.getPreferenceProperties();
			DebugOptionsHandler.getDebugOptions().setOptions(prefs);
		}
	}

}
