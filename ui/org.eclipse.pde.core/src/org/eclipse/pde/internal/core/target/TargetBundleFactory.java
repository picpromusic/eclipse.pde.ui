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
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class TargetBundleFactory {

	private static TargetBundleFactory INSTANCE;

	private TargetBundleFactory() {
	}

	public static TargetBundleFactory getInstance() {
		if (INSTANCE == null)
			INSTANCE = new TargetBundleFactory();
		return INSTANCE;
	}

	/**
	 * A registry can be built to identify old school source bundles.
	 */
	private IExtensionRegistry fRegistry;

	/**
	 * Most recent source path detected from an old-style source bundle extension.
	 */
	private String fSourcePath;

	/**
	 * Returns a resolved bundle for the given file or <code>null</code> if none.
	 * @param file root jar or folder that contains a bundle
	 * @param location the target location providing this bundle
	 * @return resolved bundle or <code>null</code>
	 * @exception CoreException if not a valid bundle
	 */
	public TargetBundle createTargetBundle(File file, ITargetLocation location) throws CoreException {
		Map manifest = loadManifest(file);
		try {
			String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (header != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
				if (elements != null) {
					String name = elements[0].getValue();
					if (name != null) {
						BundleInfo info = new BundleInfo();
						info.setSymbolicName(name);
						info.setLocation(file.toURI());
						header = (String) manifest.get(Constants.BUNDLE_VERSION);
						if (header != null) {
							elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
							if (elements != null) {
								info.setVersion(elements[0].getValue());
							}
						}
						BundleInfo source = getProvidedSource(file, name, manifest);
						boolean fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
						TargetBundle rb = new TargetBundle(info, location, null, source, false, fragment);
						rb.setSourcePath(fSourcePath);
						return rb;
					}
				}
			}
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, file.getAbsolutePath()), e));
		}
		return null;
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary
	 * @throws CoreException if manifest has invalid syntax or is missing
	 */
	private Map loadManifest(File bundleLocation) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					manifestStream = new FileInputStream(file);
				} else {
					Map map = loadPluginXML(bundleLocation);
					if (map != null) {
						return map; // else fall through to invalid manifest
					}
				}
			}
			if (manifestStream == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			Map map = ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
			// Validate manifest - BSN must be present.
			// Else look for plugin.xml in case it's an old style plug-in
			String bsn = (String) map.get(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn == null && bundleLocation.isDirectory()) {
				map = loadPluginXML(bundleLocation); // not a bundle manifest, try plugin.xml
			}
			if (map == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			return map;
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

	/**
	 * If the given bundle is a source bundle, the bundle that this bundle provides source for will be returned.
	 * If the given bundle is not a source bundle or there was a problem getting the source target, <code>null</code>
	 * will be returned.
	 * 
	 * @param bundle location of the bundle in the file system, can be <code>null</code> to skip searching plugin.xml
	 * @param symbolicName symbolic name of the bundle, can be <code>null</code> to skip searching of plugin.xml
	 * @param manifest the bundle's manifest, can be <code>null</code> to skip searching of manifest entries
	 * @return bundle for provided source or <code>null</code> if not a source bundle
	 */
	private BundleInfo getProvidedSource(File bundle, String symbolicName, Map manifest) {
		fSourcePath = null;
		if (manifest != null) {
			if (manifest.containsKey(ICoreConstants.ECLIPSE_SOURCE_BUNDLE)) {
				try {
					ManifestElement[] manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, (String) manifest.get(ICoreConstants.ECLIPSE_SOURCE_BUNDLE));
					if (manifestElements != null) {
						for (int j = 0; j < manifestElements.length; j++) {
							ManifestElement currentElement = manifestElements[j];
							String binaryPluginName = currentElement.getValue();
							String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
							// Currently the version attribute is required
							if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0) {
								return new BundleInfo(binaryPluginName, versionEntry, null, BundleInfo.NO_LEVEL, false);
							}
						}
					}
				} catch (BundleException e) {
					PDECore.log(e);
					return null;
				}
			}
			// source bundles never have a class path
			if (manifest.containsKey(Constants.BUNDLE_CLASSPATH)) {
				return null;
			}
		}

		if (bundle != null && symbolicName != null) {
			// old source bundles were never jar'd
			if (bundle.isFile()) {
				return null;
			}

			// check for an "org.eclipse.pde.core.source" extension 
			File pxml = new File(bundle, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			if (!pxml.exists()) {
				pxml = new File(bundle, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			}
			if (pxml.exists()) {
				IExtensionRegistry registry = getRegistry();
				RegistryContributor contributor = new RegistryContributor(symbolicName, symbolicName, null, null);
				try {
					registry.addContribution(new BufferedInputStream(new FileInputStream(pxml)), contributor, false, null, null, this);
					IExtension[] extensions = registry.getExtensions(contributor);
					for (int i = 0; i < extensions.length; i++) {
						IExtension extension = extensions[i];
						if (ICoreConstants.EXTENSION_POINT_SOURCE.equals(extension.getExtensionPointUniqueIdentifier())) {
							IConfigurationElement[] elements = extension.getConfigurationElements();
							if (elements.length == 1) {
								fSourcePath = elements[0].getAttribute("path"); //$NON-NLS-1$
							}
							return new BundleInfo(null, null, bundle.toURI(), BundleInfo.NO_LEVEL, false);
						}
					}
				} catch (FileNotFoundException e) {
				}
			}
		}
		return null;
	}

	/**
	 * Returns an extension registry used to identify source bundles.
	 * 
	 * @return extension registry
	 */
	private IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry = RegistryFactory.createRegistry(null, this, this);
			// contribute PDE source extension point
			String bogusDef = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?eclipse version=\"3.2\"?>\n<plugin><extension-point id=\"source\" name=\"source\"/>\n</plugin>"; //$NON-NLS-1$
			RegistryContributor contributor = new RegistryContributor(PDECore.PLUGIN_ID, PDECore.PLUGIN_ID, null, null);
			fRegistry.addContribution(new ByteArrayInputStream(bogusDef.getBytes()), contributor, false, null, null, this);
		}
		return fRegistry;
	}

	/**
	 * Parses an old style plug-in's (or fragment's) XML definition file into a dictionary.
	 * The plug-in must be in a directory at the specified location.
	 * 
	 * @param pluginDir root location of the plug-in
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	private Map loadPluginXML(File pluginDir) throws CoreException {
		File pxml = new File(pluginDir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		File fxml = new File(pluginDir, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		if (pxml.exists() || fxml.exists()) {
			// support classic non-OSGi plug-in
			PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
			if (converter != null) {
				try {
					Dictionary convert = converter.convertManifest(pluginDir, false, null, false, null);
					if (convert != null) {
						Map map = new HashMap(convert.size(), 1.0f);
						Enumeration keys = convert.keys();
						while (keys.hasMoreElements()) {
							Object key = keys.nextElement();
							map.put(key, convert.get(key));
						}
						return map;
					}
				} catch (PluginConversionException e) {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_2, pluginDir.getAbsolutePath()), e));
				}
			}
		}
		return null;
	}

}
