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

import java.util.Hashtable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @since 3.6
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

		return trace;
	}

	/**
	 * This wrapper class helps to avoid the <code>java.lang.IllegalArgumentException</code>
	 * thrown by the various trace methods when they try to format the message and it has the
	 * class names followed by {}  
	 */
	class Trace implements DebugTrace {

		@SuppressWarnings("hiding")
		private DebugTrace trace;

		public Trace(DebugTrace trace) {
			this.trace = trace;
		}

		private String quotify(String option) {
			if (option != null) {
				option = "'" + option + "'"; //$NON-NLS-1$//$NON-NLS-2$
			}
			return option;
		}

		public void trace(String option, String message) {
			trace.trace(quotify(option), message);
		}

		public void trace(String option, String message, Throwable error) {
			trace.trace(quotify(option), message, error);
		}

		public void traceDumpStack(String option) {
			trace.traceDumpStack(quotify(option));
		}

		public void traceEntry(String option) {
			trace.traceEntry(quotify(option));
		}

		public void traceEntry(String option, Object methodArgument) {
			trace.traceEntry(quotify(option), quotify(String.valueOf(methodArgument)));
		}

		public void traceEntry(String option, Object[] methodArguments) {
			if (methodArguments != null && methodArguments.length > 0) {
				Object[] methodArgs = new Object[methodArguments.length];
				for (int i = 0; i < methodArgs.length; i++) {
					methodArgs[i] = quotify(String.valueOf(methodArguments[i]));
				}
				trace.traceEntry(quotify(option), methodArgs);
			}
		}

		public void traceExit(String option) {
			trace.traceExit(quotify(option));
		}

		public void traceExit(String option, Object result) {
			trace.traceExit(quotify(option), quotify(String.valueOf(result)));
		}

	}

	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);
		TracingUIActivator.plugin = this;
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

	/**
	 * Accessor for the {@link DebugOptions} service.
	 * 
	 * @return The {@link DebugOptions} service
	 */
	public final DebugOptions getDebugOptions() {

		DebugOptions options = null;
		if (this.debugTracker == null) {
			BundleContext context = this.getBundle().getBundleContext();
			if (context != null) {
				this.debugTracker = new ServiceTracker<Object, Object>(context, DebugOptions.class.getName(), null);
				this.debugTracker.open();
			}
		}
		if (this.debugTracker != null) {
			options = (DebugOptions) this.debugTracker.getService();
		}
		return options;
	}

	public void optionsChanged(final DebugOptions options) {

		if (trace == null) {
			trace = new Trace(options.newDebugTrace(TracingConstants.BUNDLE_ID));
		}
		DEBUG = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_DEBUG_STRING, false);
		DEBUG_PREFERENCES = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_PREFERENCES_STRING, false);
		DEBUG_MODEL = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_MODEL_STRING, false);
		DEBUG_UI = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_UI_STRING, false);
		DEBUG_UI_LISTENERS = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_UI_LISTENERS_STRING, false);
		DEBUG_UI_PROVIDERS = options.getBooleanOption(TracingConstants.BUNDLE_ID + TracingConstants.TRACE_UI_PROVIDERS_STRING, false);
	}

	/** Is generic tracing enabled for this bundle? */
	public static boolean DEBUG = false;

	/** Is tracing enable for this bundles preference handling? */
	public static boolean DEBUG_PREFERENCES = false;

	/** Is tracing enabled for this bundles model handling usage? */
	public static boolean DEBUG_MODEL = false;

	/** Is tracing enabled for this bundles model handling usage? */
	public static boolean DEBUG_UI = false;

	/** Is tracing enabled for this bundles model handling usage? */
	public static boolean DEBUG_UI_LISTENERS = false;

	/** Is tracing enabled for this bundles model handling usage? */
	public static boolean DEBUG_UI_PROVIDERS = false;

	/** The shared instance */
	private static TracingUIActivator plugin = null;

	/** the tracing object */
	private static DebugTrace trace = null;

	/** DebugOptions service tracker */
	private ServiceTracker<Object, Object> debugTracker = null;
}