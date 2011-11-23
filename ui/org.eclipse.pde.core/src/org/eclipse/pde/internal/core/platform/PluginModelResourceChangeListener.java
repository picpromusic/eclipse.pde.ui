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

import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;

public class PluginModelResourceChangeListener implements IResourceChangeListener, IResourceDeltaVisitor
	private PluginModelManager fModelManager;
	
	public PluginModelResourceChangeListener(PluginModelManager manager){
		fModelManager = manager;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE :
				if (event.getDelta() != null){
					try {
						event.getDelta().accept(this);
					} catch (CoreException e) {
						PDECore.logException(e);
					}
				}
				break;
			case IResourceChangeEvent.PRE_CLOSE :
				if (event.getResource() instanceof IProject){
					// TODO Handle model removal
					fModelManager.removeModel((IMMevent.getResource())
				}
				break;
		}
	}

		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			final IResource resource = delta.getResource();
			if (!resource.isDerived()) {
				switch (resource.getType()) {
					case IResource.ROOT :
						return true;
					case IResource.PROJECT : {
						IProject project = (IProject) resource;
						if (isInterestingProject(project) && (delta.getKind() == IResourceDelta.ADDED || (delta.getFlags() & IResourceDelta.OPEN) != 0)) {
							createModel(project, true);
							return false;
						} else if (delta.getKind() == IResourceDelta.REMOVED) {
							removeModel(project);
							return false;
						}
						return true;
					}
					case IResource.FOLDER :
						return isInterestingFolder((IFolder) resource);
					case IResource.FILE :
						// do not process 
						if (isContentChange(delta)) {
							handleFileDelta(delta);
							return false;
						}
				}
			}
			return false;
		}

}
