/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.providers;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;
import org.eclipse.ui.trace.internal.utils.TracingConstants;

/**
 * The {@link ICheckStateProvider} implementation to ensure that the debug options that are enabled, or are a text
 * value, are automatically checked. If the element to check is
 * 
 * @since 3.6
 */
public class TracingComponentCheckStateProvider implements ICheckStateProvider {

	/**
	 * Constructor for a new TracingComponentCheckStateProvider
	 * 
	 * @param checkBoxTreeViewer
	 *            The {@link CheckboxTreeViewer} displaying the tracing content
	 */
	public TracingComponentCheckStateProvider(final CheckboxTreeViewer checkBoxTreeViewer) {

		this.viewer = checkBoxTreeViewer;
	}

	public boolean isChecked(final Object element) {

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_PROVIDERS_STRING, element);
		}
		// check to see if this item is currently checked. If it is then do nothing
		boolean checked = this.viewer.getChecked(element);
		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.trace(TracingConstants.TRACE_UI_PROVIDERS_STRING,
					"Element " + element + " was previously checked? " + checked); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!checked && element instanceof TracingNode) {
			checked = ((TracingNode) element).isEnabled();
		}
		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_PROVIDERS_STRING, Boolean.valueOf(checked));
		}
		return checked;
	}

	public boolean isGrayed(final Object element) {

		return false;
	}

	/** The checkbox viewer */
	private CheckboxTreeViewer viewer = null;

	/** Trace object for this bundle */
	private final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}