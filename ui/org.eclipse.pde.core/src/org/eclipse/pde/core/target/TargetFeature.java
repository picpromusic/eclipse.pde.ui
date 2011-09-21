/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import java.io.File;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * @since 3.8
 */
public class TargetFeature implements IAdaptable {

	private IFeatureModel featureModel;

	public TargetFeature(File featureLocation) {
		Assert.isNotNull(featureLocation);
		File manifest;
		if (ICoreConstants.FEATURE_FILENAME_DESCRIPTOR.equalsIgnoreCase(featureLocation.getName())) {
			manifest = featureLocation;
		} else {
			manifest = new File(featureLocation, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
		}
		featureModel = ExternalFeatureModelManager.createModel(manifest);
	}

	public String getId() {
		if (featureModel == null)
			return null;
		return featureModel.getFeature().getId();
	}

	public String getVersion() {
		if (featureModel == null)
			return null;
		return featureModel.getFeature().getVersion();
	}

	public boolean isLoaded() {
		if (featureModel == null)
			return false;
		return featureModel.isLoaded();
	}

	public Object getAdapter(Class adapter) {
		if (IFeatureModel.class == adapter) {
			return featureModel;
		}
		return null;
	}

	public String getInstallLocation() {
		if (featureModel == null)
			return null;
		return featureModel.getInstallLocation();
	}
}
