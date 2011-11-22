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
package org.eclipse.ui.trace.internal.listners;

import org.eclipse.ui.trace.internal.TracingUIActivator;

import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.datamodel.*;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.eclipse.ui.trace.internal.utils.TracingUtils;

/**
 * The check state listener.
 * 
 * @since 3.6
 */
public class TracingCheckStateListener implements ICheckStateListener {

	/**
	 * Construct a new {@link TracingCheckStateListener}
	 */
	public TracingCheckStateListener() {

		this.modifiedDebugOptions = new ModifiedDebugOptions();
	}

	public void checkStateChanged(final CheckStateChangedEvent event) {

		if (TracingUIActivator.DEBUG_UI_LISTENERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_LISTENERS_STRING, event);
		}
		CheckboxTreeViewer treeViewer = (CheckboxTreeViewer) event.getSource();
		final Object selectedItem = event.getElement();
		final boolean newCheckedState = event.getChecked();
		if (selectedItem instanceof TracingComponent) {
			// iterate over each child node and set its checked state
			TracingComponent component = (TracingComponent) selectedItem;
			if (TracingUIActivator.DEBUG_UI_LISTENERS) {
				TRACE.trace(TracingConstants.TRACE_UI_LISTENERS_STRING, "component '" + component + " was selected"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			TracingComponentDebugOption[] componentDebugOptions = component.getChildren();
			for (int childIndex = 0; childIndex < componentDebugOptions.length; childIndex++) {
				TracingComponentDebugOption[] debugOptions = TracingCaches.getInstance().getTracingDebugOptions(
						componentDebugOptions[childIndex].getLabel());
				for (int debugOptionsIndex = 0; debugOptionsIndex < debugOptions.length; debugOptionsIndex++) {
					this.checkDebugOption(treeViewer, debugOptions[debugOptionsIndex], newCheckedState);
				}
			}
		}
		else if (selectedItem instanceof TracingComponentDebugOption) {
			TracingComponentDebugOption debugOption = (TracingComponentDebugOption) selectedItem;
			if (TracingUIActivator.DEBUG_UI_LISTENERS) {
				TRACE.trace(TracingConstants.TRACE_UI_LISTENERS_STRING,
						"debug option '" + debugOption + " was selected"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			/*
			 * get a list of all the debug options that match this node and check them. This list also includes the
			 * currently selected debug option
			 */
			TracingComponentDebugOption[] debugOptions = TracingCaches.getInstance().getTracingDebugOptions(
					debugOption.getLabel());
			for (int debugOptionsIndex = 0; debugOptionsIndex < debugOptions.length; debugOptionsIndex++) {
				this.checkDebugOption(treeViewer, debugOptions[debugOptionsIndex], newCheckedState);
			}
		}
		if (TracingUIActivator.DEBUG_UI_LISTENERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_LISTENERS_STRING);
		}
	}

	/**
	 * TODO
	 * 
	 * @param treeViewer
	 * @param debugOption
	 * @param checkedState
	 */
	private void checkDebugOption(final CheckboxTreeViewer treeViewer, final TracingComponentDebugOption debugOption,
			final boolean checkedState) {

		if (TracingUIActivator.DEBUG_UI_LISTENERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_LISTENERS_STRING, new Object[] { treeViewer, debugOption,
					String.valueOf(checkedState) });
		}
		// add this debug options to the list of modified debug options
		if (checkedState) {
			if (TracingUIActivator.DEBUG_UI_LISTENERS) {
				TRACE.trace(TracingConstants.TRACE_UI_LISTENERS_STRING,
						"Adding '" + debugOption + "' to the list of added debug options"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.modifiedDebugOptions.addDebugOption(debugOption);
		}
		else {
			if (TracingUIActivator.DEBUG_UI_LISTENERS) {
				TRACE.trace(TracingConstants.TRACE_UI_LISTENERS_STRING,
						"Adding '" + debugOption + "' to the list of removed debug options"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.modifiedDebugOptions.removeDebugOption(debugOption);
		}
		// make sure this node is checked and update the option-path value if its a boolean debug option
		treeViewer.setChecked(debugOption, checkedState);
		if (TracingUtils.isValueBoolean(debugOption)) {
			if (TracingUIActivator.DEBUG_UI_LISTENERS) {
				TRACE.trace(TracingConstants.TRACE_UI_LISTENERS_STRING,
						"Changing the option-path value of '" + debugOption + "' to: " + checkedState); //$NON-NLS-1$ //$NON-NLS-2$
			}
			debugOption.setOptionPathValue(checkedState);
		}
		// now make sure all child nodes are checked
		TracingComponentDebugOption[] children = debugOption.getChildren();
		for (int childIndex = 0; childIndex < children.length; childIndex++) {
			this.checkDebugOption(treeViewer, children[childIndex], checkedState);
		}
		if (TracingUIActivator.DEBUG_UI_LISTENERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_LISTENERS_STRING);
		}
	}

	/**
	 * Accessor for the modified debug options
	 * 
	 * @return Returns a {@link ModifiedDebugOptions} object containing the list of debug options added (checked) and
	 *         removed (unchecked).
	 */
	public ModifiedDebugOptions getModifiedDebugOptions() {

		return this.modifiedDebugOptions;
	}

	/** The object containing the list of debug options checked or unchecked during this instance */
	private ModifiedDebugOptions modifiedDebugOptions = null;

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}