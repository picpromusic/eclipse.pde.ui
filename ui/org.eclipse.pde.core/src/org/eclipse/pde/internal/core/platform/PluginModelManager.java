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
package org.eclipse.pde.internal.core.platform;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.internal.core.*;

/**
 * The new and improved model manager stores as much information as possible in the PDEState. It acts
 * as an access point for the models, manages the various change listeners, and updates the models and
 * state as the workspace and target bundles change.
 *
 */

public class PluginModelManager {

	/**
	 * keeps the combined view of the target and workspace
	 */
	private PDEState fState;

	/**
	 * a list of listeners interested in changes to the plug-in models 
	 */
	private ListenerList/*<IPluginModelListener>*/fListeners;

	/**
	 * a list of listeners interested in changes to the PDE/resolver State
	 */
	private ListenerList/*<IStateDeltaListener>*/fStateListeners;

	/**
	 * a list of listeners interested in changes affecting known extension points
	 */
	private ListenerList/*<IExtensionDeltaListener>*/fExtensionListeners;

	/**
	 * Called as part of save participant. Save's PDE model state.
	 */
	public void saveState() {
		if (fState != null) {
			fState.saveState();
		}
	}

	/**
	 * Perform cleanup upon shutting down
	 */
	public void shutdown() {
		if (fListeners != null) {
			fListeners.clear();
			fListeners = null;
		}
		if (fStateListeners != null) {
			fStateListeners.clear();
			fStateListeners = null;
		}
		if (fExtensionListeners != null) {
			fExtensionListeners.clear();
			fExtensionListeners = null;
		}
	}

	/**
	 * Adds a plug-in model listener to the model manager
	 * 
	 * @param listener  the listener to be added
	 */
	public void addPluginModelListener(IPluginModelListener listener) {
		if (fListeners == null)
			fListeners = new ListenerList();
		fListeners.add(listener);
	}

	/**
	 * Adds a StateDelta listener to the model manager
	 * 
	 * @param listener	the listener to be added
	 */
	public void addStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners == null)
			fStateListeners = new ListenerList();
		fStateListeners.add(listener);
	}

	/**
	 * Adds an extension delta listener to the model manager
	 * 
	 * @param listener	the listener to be added
	 */
	public void addExtensionDeltaListener(IExtensionDeltaListener listener) {
		if (fExtensionListeners == null)
			fExtensionListeners = new ListenerList();
		fExtensionListeners.add(listener);
	}

	/**
	 * Notify all interested listeners that changes are made to the plug-in models
	 * 
	 * @param delta  the delta of model changes
	 */
	private void firePluginModelDelta(PluginModelDelta delta) {
		if (fListeners != null) {
			Object[] listeners = fListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IPluginModelListener) listeners[i]).modelsChanged(delta);
			}
		}
	}

	/**
	 * Notify all interested listeners in changes made to the resolver State
	 * 
	 * @param delta	the delta from the resolver State.
	 */
	private void fireStateDelta(StateDelta delta) {
		if (fStateListeners != null) {
			Object[] listeners = fStateListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IStateDeltaListener) listeners[i]).stateResolved(delta);
			}
		}
	}

	/**
	 * Notify all interested listeners that extension points have changed
	 * 
	 * @param delta the delta of extension changes
	 */
	private void fireExtensionDelta(IExtensionDeltaEvent delta) {
		if (fExtensionListeners != null) {
			Object[] listeners = fExtensionListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((IExtensionDeltaListener) listeners[i]).extensionsChanged(delta);
			}
		}
	}

}
