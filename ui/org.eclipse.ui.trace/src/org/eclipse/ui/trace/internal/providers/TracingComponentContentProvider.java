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
package org.eclipse.ui.trace.internal.providers;

import java.util.Collection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;
import org.eclipse.ui.trace.internal.utils.TracingConstants;

/**
 * An {@link ITreeContentProvider} implementation for providing the content to display in the tracing viewer
 */
public class TracingComponentContentProvider implements ITreeContentProvider {

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();

	public TracingNode[] getChildren(final Object parentElement) {

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_PROVIDERS_STRING, parentElement);
		}

		TracingNode[] children = null;
		if (parentElement instanceof TracingNode) {
			final TracingNode node = (TracingNode) parentElement;
			children = node.getChildren();
		}

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_PROVIDERS_STRING, children);
		}
		return children;
	}

	public boolean hasChildren(final Object element) {

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_PROVIDERS_STRING, element);
		}

		boolean hasChildren = false;
		if ((element != null) && (element instanceof TracingNode)) {
			hasChildren = ((TracingNode) element).hasChildren();
		}

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_PROVIDERS_STRING, hasChildren);
		}
		return hasChildren;
	}

	public Object[] getElements(final Object inputElement) {

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_PROVIDERS_STRING, inputElement);
		}

		TracingNode results[] = null;
		if (inputElement instanceof TracingNode) {
			results = new TracingNode[] {(TracingNode) inputElement};
		} else if (inputElement instanceof TracingNode[]) {
			results = (TracingNode[]) inputElement;
		} else if (inputElement instanceof Collection<?>) {
			Collection<?> collectionElement = (Collection<?>) inputElement;
			results = collectionElement.toArray(new TracingNode[collectionElement.size()]);
		}
		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_PROVIDERS_STRING, results);
		}
		return results;
	}

	public Object getParent(final Object element) {

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_PROVIDERS_STRING, element);
		}

		TracingNode node = null;
		if ((element != null) && (element instanceof TracingNode)) {
			node = ((TracingNode) element).getParent();
		}

		if (TracingUIActivator.DEBUG_UI_PROVIDERS) {
			TRACE.traceExit(TracingConstants.TRACE_UI_PROVIDERS_STRING, node);
		}
		return node;
	}

	public void dispose() {

		// do nothing (for now)
	}

	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

		// do nothing (for now)
	}
}