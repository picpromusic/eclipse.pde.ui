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

/**
 * Utility handler for storing the options that were changed
 */
public class ModifiedDebugOptions {

	/**
	 * Construct a new ModifiedDebugOptions object
	 */
	public ModifiedDebugOptions() {

		this.modifiedDebugOptions = new ArrayList<TracingComponentDebugOption>();
	}

	/**
	 * Accessor for an array of the {@link TracingComponentDebugOption} items that were modified on the
	 * tracing preference page.
	 * 
	 * @return An array of the {@link TracingComponentDebugOption} items that were modified on the tracing
	 *         preference page
	 */
	public final TracingComponentDebugOption[] getModifiedDebugOptions() {

		return this.modifiedDebugOptions.toArray(new TracingComponentDebugOption[this.modifiedDebugOptions.size()]);
	}

	/**
	 * Adds a {@link TracingComponentDebugOption} to the list of modified debug options
	 * 
	 * @param option
	 *            The {@link TracingComponentDebugOption} option that got modified
	 */
	public final void addModifiedDebugOption(final TracingComponentDebugOption option) {

		if (option != null) {
			this.modifiedDebugOptions.add(option);
		}
	}

	/**
	 * Purge the list of bundles to add and remove
	 */
	public final void clear() {

		this.modifiedDebugOptions.clear();
	}

	/**
	 * A list of the {@link TracingComponentDebugOption} instances that got modified.
	 */
	private List<TracingComponentDebugOption> modifiedDebugOptions = null;

}