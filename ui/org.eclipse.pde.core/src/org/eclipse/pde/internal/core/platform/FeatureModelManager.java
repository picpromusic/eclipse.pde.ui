/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.platform;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.IFeatureModelListener;

public class FeatureModelManager {

	/**
	 * List of IFeatureModelListener
	 */
	private ListenerList fFeatureModelListeners;

	/**
	 * Creates a new feature model manager containing the given workspace and target feature models
	 * 
	 * @param workspace list of project paths in the workspaces representing features, possibly empty
	 * @param target list of file paths pointing to target (external) features, possibly empty
	 * @param monitor progress monitor, can be <code>null</code>
	 */
	public FeatureModelManager(String[] workspace, String[] target, IProgressMonitor monitor) {
		fFeatureModelListeners = new ListenerList();
		SubMonitor subMon = SubMonitor.convert(monitor, "Creating feature model manager", 200);
		try {
			addTargetFeatures(target, subMon.newChild(150));
			addWorkspaceFeatures(workspace, subMon.newChild(50));
		} finally {
			subMon.done();
			if (monitor != null) {
				monitor.done();
			}
		}

	}

	/**
	 * Registers a feature model listener to this manager.  Has no effect if
	 * the listener is already registered.
	 * 
	 * @param listener listener to add
	 */
	public void addFeatureModelListener(IFeatureModelListener listener) {
		fFeatureModelListeners.add(listener);
	}

	/**
	 * Unregisters a feature model listener from this manager.  Has no effect if
	 * the listener has not been registered with this manager.
	 * 
	 * @param listener listener to remove
	 */
	public void removeFeatureModelListener(IFeatureModelListener listener) {
		fFeatureModelListeners.remove(listener);
	}
}
