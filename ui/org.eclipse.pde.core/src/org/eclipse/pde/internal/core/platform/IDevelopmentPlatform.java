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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Central location for all bundles, features and models required to develop, build 
 * and launch in PDE.
 * 
 */
public interface IDevelopmentPlatform {

	public void resolve(IProgressMonitor monitor) throws CoreException;

	public ITargetDefinition getTargetDefinition();

	public State getState();

	public IBundle findBundle(String name, String version);

}
