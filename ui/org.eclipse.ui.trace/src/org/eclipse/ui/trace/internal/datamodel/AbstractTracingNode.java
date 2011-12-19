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
package org.eclipse.ui.trace.internal.datamodel;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.utils.TracingConstants;

/**
 * A abstract base class implementation of the {@link TracingNode} interface.
 */
public abstract class AbstractTracingNode implements TracingNode {

	/**
	 * Constructor to create the empty list of children
	 */
	public AbstractTracingNode() {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);

		this.children = new ArrayList<TracingNode>();

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
	}

	/**
	 * TODO
	 */
	protected abstract void populateChildren();

	public String getLabel() {

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.label);

		return this.label;
	}

	public TracingNode getParent() {

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.parent);

		return this.parent;
	}

	public TracingNode[] getChildren() {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);

		this.initialize();
		TracingNode[] results = this.children.toArray(new TracingNode[this.children.size()]);

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.children);

		return results;
	}

	public boolean hasChildren() {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);

		boolean hasChildren = false;
		this.initialize();
		if (this.children != null) {

			TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "There are no children for this node: " + this); //$NON-NLS-1$

			hasChildren = this.children.size() > 0;
		}

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, String.valueOf(hasChildren));

		return hasChildren;
	}

	public void addChild(final TracingNode childNode) {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, childNode);

		if (!this.children.contains(childNode)) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, "Adding child node: " + childNode); //$NON-NLS-1$

			this.children.add(childNode);
		}
		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
	}

	/**
	 * Populate the list of children for this node if it has not been initialized yet.
	 */
	public void initialize() {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);

		if (!this.childrenInitialized) {

			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, "First time population of the child nodes for '" + this); //$NON-NLS-1$

			this.populateChildren();
			this.childrenInitialized = true;
		}

		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
	}

	public void setLabel(final String label) {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, label);
		this.label = label;
		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
	}

	public void setParent(final TracingNode parent) {

		TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, parent);

		if (this.parent == null) {
			this.parent = parent;
			if (this.parent != null) {
				// since a parent is being set then it should also be added as a child
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Adding '" + this + "' to the parent node '" + this.parent + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				this.parent.addChild(this);
			}
		}
		TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
	}

	/** This nodes parent node */
	protected TracingNode parent = null;

	/** The label for this node */
	protected String label = null;

	/** The list of child nodes for this node */
	protected List<TracingNode> children = null;

	/** A flag to determine if the children have been initialized for this node */
	private boolean childrenInitialized = false;

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}