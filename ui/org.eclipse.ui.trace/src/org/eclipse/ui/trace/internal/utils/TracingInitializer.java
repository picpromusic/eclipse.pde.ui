package org.eclipse.ui.trace.internal.utils;

import java.io.File;
import java.util.Map;
import org.eclipse.ui.IStartup;

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
