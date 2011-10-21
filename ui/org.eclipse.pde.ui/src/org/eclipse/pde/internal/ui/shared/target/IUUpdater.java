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
package org.eclipse.pde.internal.ui.shared.target;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.UpdateTargetJob;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

/**
 * Updater class for the IU target location 
 *
 */
public class IUUpdater implements ITargetLocationUpdater {

	IUBundleContainer fLocation = null;
	IUWrapper fWrapper = null;

	public IUUpdater(Object adaptableObject) {
		if (adaptableObject instanceof IUBundleContainer) {
			fLocation = (IUBundleContainer) adaptableObject;
		} else if (adaptableObject instanceof IUWrapper) {
			fWrapper = (IUWrapper) adaptableObject;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IEditable#canUpdate()
	 */
	public boolean canUpdate() {
		return fLocation != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IEditable#canRemove()
	 */
	public boolean canRemove() {
		return true;
	}

	private IStatus status = Status.CANCEL_STATUS;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IEditable#update(java.util.List)
	 */
	public IStatus update(List toUpdate) {
		if (fLocation == null) {
			return Status.CANCEL_STATUS;
		}
		Map locationMap = new HashMap();
		Set children = new HashSet();
		children.addAll(toUpdate);
		locationMap.put(fLocation, children);

		JobChangeAdapter listener = new JobChangeAdapter() {
			public void done(final IJobChangeEvent event) {
				UpdateTargetJob job = (UpdateTargetJob) event.getJob();
				setStatus(job.getResult());
			}
		};

		UpdateTargetJob.update(locationMap, listener);
		try {
			Job.getJobManager().join(UpdateTargetJob.JOB_FAMILY_ID, null);
		} catch (OperationCanceledException e) {
		} catch (InterruptedException e) {
		}

		return status;
	}

	private void setStatus(IStatus status) {
		this.status = status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IEditable#getLocation()
	 */
	public ITargetLocation getLocation() {
		return fLocation;
	}

	public boolean remove(List toRemove) {
		for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
			Object object = iter.next();
			if (object instanceof IUWrapper) {
				IUWrapper iuWrapper = (IUWrapper) object;
				iuWrapper.getParent().removeInstallableUnit(iuWrapper.getIU());
			}
		}
		return true;
	}
}
