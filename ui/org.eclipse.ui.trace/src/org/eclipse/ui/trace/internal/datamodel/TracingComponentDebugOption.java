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
package org.eclipse.ui.trace.internal.datamodel;

import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.utils.*;

/**
 * A debug option tracing component is a tree node that contains the option-path and value for a single debug option. A
 * debug option can have a {@link TracingComponent} or another {@link TracingComponentDebugOption} as a parent.
 * 
 * @since 3.6
 */
public class TracingComponentDebugOption extends AbstractTracingNode {

	/**
	 * Construct a new {@link TracingComponentDebugOption} that does not have a parent node set. A parent node is
	 * required for all {@link TracingComponentDebugOption} instances but can be set at a later time via
	 * {@link TracingComponentDebugOption#setParent(TracingNode)}.
	 * 
	 * @param path
	 *            A non-null path for this debug option
	 * @param value
	 *            A non-null value for this debug option
	 */
	public TracingComponentDebugOption(final String path, final String value) {

		this(null, path, value);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Constructor for a new {@link TracingComponentDebugOption} for a specific parent node.
	 * 
	 * @param parentNode
	 *            The parent {@link TracingNode} for this {@link TracingComponentDebugOption}
	 * @param path
	 *            A non-null path for this debug option
	 * @param value
	 *            A non-null value for this debug option
	 */
	public TracingComponentDebugOption(final TracingNode parentNode, final String path, final String value) {

		super();
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, new Object[] { parentNode, path, value });
		}
		assert (path != null);
		assert (value != null);
		this.optionPath = path;
		this.optionPathValue = value;
		this.setParent(parentNode);
		this.setLabel(path);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	@Override
	public String toString() {

		final StringBuilder builder = new StringBuilder();
		builder.append("TracingComponentDebugOption [optionPath="); //$NON-NLS-1$
		builder.append(this.optionPath);
		builder.append(", optionPathValue="); //$NON-NLS-1$
		builder.append(this.optionPathValue);
		builder.append(", parent="); //$NON-NLS-1$
		if (this.getParent() != null) {
			builder.append(this.getParent());
		}
		else {
			builder.append("<unset>"); //$NON-NLS-1$
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.optionPath == null) ? 0 : this.optionPath.hashCode());
		result = prime * result + ((this.getParent() == null) ? 0 : this.getParent().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TracingComponentDebugOption)) {
			return false;
		}
		TracingComponentDebugOption other = (TracingComponentDebugOption) obj;
		if (this.optionPath == null) {
			if (other.optionPath != null) {
				return false;
			}
		}
		else if (!this.optionPath.equals(other.optionPath)) {
			return false;
		}
		if (this.getParent() == null) {
			if (other.getParent() != null) {
				return false;
			}
		}
		else if (!this.getParent().equals(other.getParent())) {
			return false;
		}
		return true;
	}

	public boolean isEnabled() {

		boolean isEnabled = false;
		if (TracingUtils.isValueBoolean(this.optionPathValue)) {
			isEnabled = Boolean.parseBoolean(this.optionPathValue);
		}
		else {
			// a non-boolean debug option - enable it only if it exists in the DebugOptions
			String value = DebugOptionsHandler.getDebugOptions().getOption(this.optionPath);
			isEnabled = (value != null);
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, String.valueOf(isEnabled));
		}
		return isEnabled;
	}

	@Override
	protected void populateChildren() {

		// empty implementation - all work is done in TracingComponent#populateChildren()
	}

	@Override
	public final TracingComponentDebugOption[] getChildren() {

		return TracingComponentDebugOption.CHILDREN;
	}

	/**
	 * Accessor to the debug option path (i.e. bundle/option-path) of this {@link TracingComponentDebugOption}
	 * 
	 * @return the debug option path (i.e. bundle/option-path) of this {@link TracingComponentDebugOption}
	 */
	public final String getOptionPath() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.optionPath);
		}
		return this.optionPath;
	}

	/**
	 * Accessor to the debug option value of this {@link TracingComponentDebugOption}
	 * 
	 * @return the debug option value of this {@link TracingComponentDebugOption}
	 */
	public final String getOptionPathValue() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.optionPathValue);
		}
		return this.optionPathValue;
	}

	/**
	 * Set the new option-path value
	 * 
	 * @param newValue
	 *            A non-null new {@link String} value of the option-path
	 */
	public final void setOptionPathValue(final String newValue) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, newValue);
		}
		assert (newValue != null);
		this.optionPathValue = newValue;
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Set the new option-path value to the specified boolean value
	 * 
	 * @param newValue
	 *            A new boolean value of the option-path
	 */
	public final void setOptionPathValue(final boolean newValue) {

		String valueAsString = Boolean.toString(newValue);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, valueAsString);
		}
		this.optionPathValue = valueAsString;
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * The option-path - this value cannot change
	 */
	private final String optionPath;

	/**
	 * The value of the option-path - this value can change
	 */
	private String optionPathValue;

	/**
	 * A {@link TracingComponentDebugOption} has no children
	 */
	private final static TracingComponentDebugOption[] CHILDREN = new TracingComponentDebugOption[0];
}