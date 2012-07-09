/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.util.*;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.util.HeaderMap;
import org.osgi.framework.Constants;

public class Bundle extends BundleObject implements IBundle {
	private static final long serialVersionUID = 1L;
	private Map fDocumentHeaders = new HeaderMap();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
		IManifestHeader header = (ManifestHeader) fDocumentHeaders.get(key);
		String old = null;
		if (header == null) {
			header = getModel().getFactory().createHeader(key, value);
			fDocumentHeaders.put(key, header);
			getModel().fireModelObjectChanged(header, key, old, value);
		} else {
			old = header.getValue();
			header.setValue(value);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getHeader(java.lang.String)
	 */
	public String getHeader(String key) {
		ManifestHeader header = (ManifestHeader) fDocumentHeaders.get(key);
		return (header != null) ? header.getValue() : null;
	}

	/**
	 * Load a map of String key value pairs into the list of known manifest headers.
	 * Empty value strings will create empty headers.  Null values will be ignored.
	 * @param headers map<String, String> of manifest key and values
	 */
	public void load(Map headers) {
		Iterator iter = headers.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			if (headers.get(key) != null) {
				String value = headers.get(key).toString();
				IManifestHeader header = getModel().getFactory().createHeader(key.toString(), value);
				header.update(); // Format the headers, unknown if this step is necessary for new header objects
				fDocumentHeaders.put(key.toString(), header);
			}
		}
	}

	public String getLocalization() {
		String localization = getHeader(Constants.BUNDLE_LOCALIZATION);
		return localization != null ? localization : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

	public void renameHeader(String key, String newKey) {
		ManifestHeader header = (ManifestHeader) getManifestHeader(key);
		if (header != null) {
			header.setName(newKey);
			fDocumentHeaders.put(newKey, fDocumentHeaders.remove(key));
		}
		getModel().fireModelObjectChanged(header, newKey, key, newKey);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getManifestHeader(java.lang.String)
	 */
	public IManifestHeader getManifestHeader(String key) {
		IManifestHeader header = getModel().getFactory().createHeader(key, getHeader(key));
		header.update(); // Format the headers, unknown if this step is necessary for new header objects
		fDocumentHeaders.put(key, header);
		return header;
	}

	/**
	 * @return a map containing all key/value pairs of manifest headers as strings, values may be empty strings, but not <code>null</code>
	 */
	protected Map getHeaders() {
		Map result = new HashMap(fDocumentHeaders.values().size());
		for (Iterator iterator = fDocumentHeaders.values().iterator(); iterator.hasNext();) {
			IManifestHeader currentHeader = (IManifestHeader) iterator.next();
			if (currentHeader.getValue() != null) {
				result.put(currentHeader.getKey(), currentHeader.getValue());
			}
		}
		return result;
	}
}
