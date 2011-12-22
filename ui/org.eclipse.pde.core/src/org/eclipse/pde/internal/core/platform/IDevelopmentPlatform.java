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

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;

/**
 * Central location for all bundles, features and models required to develop, build 
 * and launch in PDE.
 * 
 */
public interface IDevelopmentPlatform {

	public SourceLocationManager getSourceLocationManager();

	public JavadocLocationManager getJavadocLocationManager();

	public TracingOptionsManager getTracingOptionsManager();

	public PDEExtensionRegistry getExtensionRegistry();

	public SchemaRegistry getSchemaRegistry();

	public PluginModelManager getPluginModelManager();

	public FeatureModelManager getFeatureModelManager();

	public PDEState getState();

	public ITargetDefinition getTargetDefinition();

	public boolean isInitialized();

}
