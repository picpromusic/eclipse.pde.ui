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
package org.eclipse.ui.trace.internal;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.trace.internal.utils.*;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class TracingUIActivator extends AbstractUIPlugin implements DebugOptionsListener {

	/**
	 * The constructor
	 */
	public TracingUIActivator() {

		// empty constructor
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static TracingUIActivator getDefault() {

		return TracingUIActivator.plugin;
	}

	/**
	 * Accessor for the tracing object
	 * 
	 * @return The tracing object
	 */
	public DebugTrace getTrace() {

		if (trace == null) {
			trace = new Trace();
		}
		return trace;
	}

	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);
		TracingUIActivator.plugin = this;

		if (PreferenceHandler.isTracingEnabled()) {
			DebugOptionsHandler.setDebugEnabled(true);
			DebugOptionsHandler.getDebugOptions().setFile(new File(PreferenceHandler.getFilePath()));
			System.setProperty(TracingConstants.PROP_TRACE_SIZE_MAX, String.valueOf(PreferenceHandler.getMaxFileSize()));
			System.setProperty(TracingConstants.PROP_TRACE_FILE_MAX, String.valueOf(PreferenceHandler.getMaxFileCount()));

			Map<String, String> prefs = PreferenceHandler.getPreferenceProperties();
			Map<String, String> options = DebugOptionsHandler.getDebugOptions().getOptions();
			options.putAll(prefs);
			DebugOptionsHandler.getDebugOptions().setOptions(options);
		}

		final Hashtable<String, String> props = new Hashtable<String, String>(4);
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, TracingConstants.BUNDLE_ID);
		context.registerService(DebugOptionsListener.class.getName(), this, props);

	}

	@Override
	public void stop(final BundleContext context) throws Exception {

		TracingUIActivator.plugin = null;
		super.stop(context);
	}

	/**
	 * Log the specified {@link Exception} to the workspace logging file.
	 * 
	 * @param ex
	 *            The {@link Exception} to log
	 */
	public final void logException(final Exception ex) {

		if (ex != null) {
			final IStatus errorStatus = new Status(IStatus.ERROR, TracingConstants.BUNDLE_ID, ex.getMessage(), ex);
			this.getLog().log(errorStatus);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptionsListener#optionsChanged(org.eclipse.osgi.service.debug.DebugOptions)
	 */
	public void optionsChanged(final DebugOptions options) {

		// refresh the trace with new options
		((Trace) trace).setDebugTrace(options.newDebugTrace(TracingConstants.BUNDLE_ID));
	}

	/** The shared instance */
	private static TracingUIActivator plugin = null;

	/** the tracing object */
	private static DebugTrace trace = null;
}