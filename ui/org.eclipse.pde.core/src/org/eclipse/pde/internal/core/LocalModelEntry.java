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
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;

/**
 * Subclass of ModelEntry
 * It adds methods that add/remove model from the entry.
 * These methods must not be on ModelEntry itself because
 * ModelEntry is an API class and we do not want clients to manipulate
 * the ModelEntry
 * 
 */
class LocalModelEntry extends ModelEntry {

	/**
	 * Constructs a model entry that will keep track
	 * of all bundles in the workspace and target that share the same ID.
	 * 
	 * @param id  the bundle ID
	 */
	public LocalModelEntry(String id) {
		super(id);
	}

	/**
	 * Adds a model to the entry.  
	 * An entry keeps two lists: one for workspace models 
	 * and one for target (external) models.
	 * If the model being added is associated with a workspace resource,
	 * it is added to the workspace list; otherwise, it is added to the external list.
	 * 
	 * @param model  model to be added to the entry
	 */
	public void addModel(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null)
			fWorkspaceEntries.add(model);
		else
			fExternalEntries.add(model);
	}

	/**
	 * Removes the given model for the workspace list if the model is associated
	 * with workspace resource.  Otherwise, it is removed from the external list.
	 * 
	 * @param model  model to be removed from the model entry
	 */
	public void removeModel(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null)
			fWorkspaceEntries.remove(model);
		else
			fExternalEntries.remove(model);
	}
}