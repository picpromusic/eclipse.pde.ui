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
package org.eclipse.ui.trace.internal;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.trace.internal.datamodel.TracingCollections;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.eclipse.ui.trace.internal.utils.TracingUtils;

/**
 * An editing support object for the tracing UI viewer.
 * 
 * @since 3.6
 */
public class TracingComponentColumnEditingSupport extends EditingSupport {

	/**
	 * Construct a new {@link TracingComponentColumnEditingSupport} for the specified index.
	 * 
	 * @param viewer
	 *            The viewer to add this editing support
	 * @param index
	 *            The column index. One of either {@link TracingConstants#LABEL_COLUMN_INDEX} for the label column or
	 *            {@link TracingConstants#VALUE_COLUMN_INDEX} for the value column.
	 */
	public TracingComponentColumnEditingSupport(final ColumnViewer viewer, final int index) {

		super(viewer);
		this.columnIndex = index;
		switch (this.columnIndex) {
			case TracingConstants.VALUE_COLUMN_INDEX :
				this.editor = new TextCellEditor((Composite) viewer.getControl(), SWT.NONE);
//				this.editor = new ComboBoxCellEditor((Composite) viewer.getControl(), new String[] {String.valueOf(true), String.valueOf(false)});
				break;
			default :
				// do nothing - no other columns provide editing support
				this.editor = null;
		}
	}

	@Override
	protected boolean canEdit(final Object element) {

		boolean canEdit = false;
		switch (this.columnIndex) {
			case TracingConstants.VALUE_COLUMN_INDEX :
				return true;
//				if (element instanceof TracingComponentDebugOption) {
//					String optionPathValue = ((TracingComponentDebugOption) element).getOptionPathValue();
//					canEdit = !TracingUtils.isValueBoolean(optionPathValue);
//				} else {
//					canEdit = true;
//				}
//				break;
			default :
				// do nothing - no other columns provide editing support
				canEdit = false;
		}
		return canEdit;
	}

	@Override
	protected CellEditor getCellEditor(final Object element) {

		return this.editor;
	}

	@Override
	protected Object getValue(final Object element) {

		Object value = null;
		switch (this.columnIndex) {
			case 1 :
				if (element instanceof TracingComponentDebugOption) {
					String optionPathValue = ((TracingComponentDebugOption) element).getOptionPathValue();
					if (TracingUtils.isValueBoolean(optionPathValue)) {
						value = String.valueOf(Boolean.valueOf(optionPathValue));
					} else {
						value = optionPathValue;
					}
				} else if (element instanceof String) {
					value = element;
				}
				break;
			default :
				// do nothing - no other columns provide editing support
		}
		return value;
	}

	@Override
	protected void setValue(final Object element, final Object value) {

		switch (this.columnIndex) {
			case 1 :
				if (element instanceof TracingComponentDebugOption) {
					TracingComponentDebugOption option = (TracingComponentDebugOption) element;
					// find identical debug options and update them (this will include 'this' debug option that was
					// modified)
					TracingComponentDebugOption[] identicalOptions = TracingCollections.getInstance().getTracingDebugOptions(option.getOptionPath());
					for (int identicalOptionsIndex = 0; identicalOptionsIndex < identicalOptions.length; identicalOptionsIndex++) {
						identicalOptions[identicalOptionsIndex].setOptionPathValue(String.valueOf(value));
						this.getViewer().update(identicalOptions[identicalOptionsIndex], null);
					}
					if (!Boolean.parseBoolean(String.valueOf(value))) {
						TracingCollections.getInstance().getModifiedDebugOptions().removeDebugOption(option);
					} else {
						TracingCollections.getInstance().getModifiedDebugOptions().addDebugOption(option);
					}
				}
				break;
			default :
				// do nothing - no other columns provide editing support
		}
	}

	/**
	 * The column index of the editors. One of {@link TracingConstants#LABEL_COLUMN_INDEX} or
	 * {@link TracingConstants#VALUE_COLUMN_INDEX}
	 */
	private int columnIndex;

	/**
	 * The {@link CellEditor} for the value column
	 */
	private CellEditor editor;

}