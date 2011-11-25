/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev (ProSyst) - bug 205777
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.target.LoadTargetDefinitionJob;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class PDEState extends MinimalState {

	private PDEAuxiliaryState fAuxiliaryState;

	/**
	 * Table mapping plug-in ID to a {@link ModelEntry}.  Each model entry contains all
	 * workspace and external plug-in models with that ID that belong to this state.  A
	 * subset of the models will be part of the OSGi state at any given time.
	 * <p>
	 * In the future this may be replaced with a smarter caching scheme
	 * </p>
	 */
	private Map fEntries;

	/**
	 * Map of {@link IProject} to {@link IPluginModelBase}. Improves performance of model lookups
	 * for the workspace.
	 */
	private Map fWorkspaceModels;

	/**
	 * Creates a new PDE state containing the given workspace and target models
	 * @param workspace list of projects in the workspaces to add to the state, possibly empty
	 * @param target list of file urls pointing to target (external) plug-ins, possibly empty
	 * @param monitor progress monitor, can be <code>null</code>
	 */
	public PDEState(URL[] workspace, URL[] target, IProgressMonitor monitor) {

		// TODO Do we need a way to prevent caching states? This was supported for workspace plug-ins using System.getProperty("pde.nocache")

		long start = System.currentTimeMillis();
		fAuxiliaryState = new PDEAuxiliaryState();

		SubMonitor subMon = SubMonitor.convert(monitor, "Creating PDE State", 300);

		fEntries = new HashMap();
		addTargetModels(target, subMon.newChild(150));
		addWorkspaceBundles(workspace, subMon.newChild(150));

		subMon.done();

		if (DEBUG)
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates PDE models for the given bundles and adds them to this state. If the saved
	 * state contains the same models, the previous state will be loaded instead of
	 * recreating it from scratch.
	 * 
	 * @param targetBundles list of file URLs pointing to bundles
	 * @param monitor progress monitor
	 */
	private void addTargetModels(URL[] targetBundles, IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 150);

		// Load the state from the cache or create a new one
		long timestamp = computeTimestamp(targetBundles);
		if (DEBUG) {
			System.out.println("Timestamp of " + targetBundles.length + " target URLS: " + timestamp); //$NON-NLS-1$ //$NON-NLS-2$
		}

		File dir = getTargetCacheDirectory(timestamp);
		if ((fState = readStateCache(dir)) == null || !fAuxiliaryState.readPluginInfoCache(dir)) {
			if (DEBUG) {
				System.out.println("Creating new state, persisted target state did not exist"); //$NON-NLS-1$
			}
			createNewState(true, targetBundles, subMon.newChild(50));
			resolveState(false);
			subMon.setWorkRemaining(50);
		} else {
			if (DEBUG) {
				System.out.println("Restoring previously persisted target state"); //$NON-NLS-1$
			}
			subMon.setWorkRemaining(100); // Already read the state
			// get the system bundle from the State
			if (fState.getPlatformProperties() != null && fState.getPlatformProperties().length > 0) {
				String systemBundle = (String) fState.getPlatformProperties()[0].get(ICoreConstants.OSGI_SYSTEM_BUNDLE);
				if (systemBundle != null)
					fSystemBundle = systemBundle;
			}

			boolean propertiesChanged = initializePlatformProperties();
			fState.setResolver(Platform.getPlatformAdmin().createResolver());
			if (propertiesChanged)
				fState.resolve(false);
			fId = fState.getHighestBundleId();
			subMon.setWorkRemaining(50); // State is resolved
		}

		if (subMon.isCanceled()) {
			return;
		}

		// Create the models
		BundleDescription[] bundles = fState.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			addModelToTable(createExternalModel(bundles[i]));
		}
		subMon.setWorkRemaining(0);

		if (DEBUG) {
			System.out.println(bundles.length + " target plug-in models added to the table and state"); //$NON-NLS-1$
		}

		subMon.done();
	}

	/**
	 * Creates a new OSGi state, setting fState and adds the plug-ins to it from the file urls
	 * @param resolve whether the state should have a resolver set
	 * @param urls file urls for bundles to add to the state
	 * @param monitor progress monitor, must not be <code>null</code>
	 */
	private void createNewState(boolean resolve, URL[] urls, IProgressMonitor monitor) {
		fState = stateObjectFactory.createState(resolve);
		monitor.beginTask("", urls.length); //$NON-NLS-1$
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			try {
				if (monitor.isCanceled())
					// if cancelled, stop loading bundles
					return;
				monitor.subTask(file.getName());
				addBundle(file, -1);
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Invalid manifest format at " + file.getAbsolutePath(), //$NON-NLS-1$
						null));
			} finally {
				monitor.worked(1);
			}
		}
		monitor.done();
	}

	/**
	 * Adds workspace plug-ins to the state, either reading them from the cached state or creating them
	 * 
	 * @param workspaceBundles list of file urls pointing to workspace bundles
	 * @param monitor progress monitor, may be <code>null</code>
	 */
	private void addWorkspaceBundles(URL[] workspaceBundles, IProgressMonitor monitor) {
		if (workspaceBundles.length == 0) {
			return;
		}
		SubMonitor subMon = SubMonitor.convert(monitor, (workspaceBundles.length * 2) + 25);
		int addedCount = 0;

		long timestamp = computeTimestamp(workspaceBundles);
		File dir = getWorkspaceCacheDirectory(timestamp);
		State localState = readStateCache(dir);
		if (localState == null || !fAuxiliaryState.readPluginInfoCache(dir)) {
			if (DEBUG) {
				System.out.println("Creating new state, persisted workspace state did not exist"); //$NON-NLS-1$
			}

			for (int i = 0; i < workspaceBundles.length; i++) {
				File file = new File(workspaceBundles[i].getFile());
				try {
					if (monitor.isCanceled())
						return;
					monitor.subTask(file.getName());
					BundleDescription desc = addBundle(file, -1);

					if (desc != null) {
						IPluginModelBase model = createWorkspaceModel(desc);
						if (model != null) {
							addedCount++;
							LocalModelEntry entry = addModelToTable(model);
							IPluginModelBase[] conflicts = entry.getExternalModels();
							for (int j = 0; j < conflicts.length; j++) {
								fState.removeBundle(conflicts[i].getBundleDescription());
							}
						}
					}
				} catch (PluginConversionException e) {
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Problems reading bundle at " + file.getAbsolutePath(), //$NON-NLS-1$
							e));
				} catch (CoreException e) {
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Problems reading bundle at " + file.getAbsolutePath(), //$NON-NLS-1$
							e));
				} catch (IOException e) {
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Problems reading bundle at " + file.getAbsolutePath(), //$NON-NLS-1$
							e));
				} finally {
					monitor.worked(2);
				}
			}
		} else {
			if (DEBUG) {
				System.out.println("Restoring previously persisted workspace state"); //$NON-NLS-1$
			}

			BundleDescription[] cachedBundles = localState.getBundles();
			for (int i = 0; i < cachedBundles.length; i++) {
				if (monitor.isCanceled())
					return;
				monitor.subTask(cachedBundles[i].getSymbolicName());
				BundleDescription newbundle = stateObjectFactory.createBundleDescription(cachedBundles[i]);
				IPluginModelBase model = createWorkspaceModel(newbundle);
				if (model != null && fState.addBundle(newbundle)) {
					addedCount++;
					LocalModelEntry entry = addModelToTable(model);
					IPluginModelBase[] conflicts = entry.getExternalModels();
					for (int j = 0; j < conflicts.length; j++) {
						fState.removeBundle(conflicts[i].getBundleDescription());
					}
				}
				monitor.worked(2);
			}
			fId = fState.getHighestBundleId(); // Adding copied bundles may mean the next id isn't unique
		}

		subMon.setWorkRemaining(25);
		fState.resolve(false);
		subMon.setWorkRemaining(0);
		subMon.done();

		if (DEBUG) {
			System.out.println(addedCount + " workspace plug-in models added"); //$NON-NLS-1$
		}
	}

	private State readStateCache(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			try {
				return stateObjectFactory.readState(dir);
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
			}
		}
		return null;
	}

	private long computeTimestamp(URL[] urls) {
		return computeTimestamp(urls, 0);
	}

	private long computeTimestamp(URL[] urls, long timestamp) {
		List sorted = new ArrayList(urls.length);
		for (int i = 0; i < urls.length; i++) {
			sorted.add(urls[i]);
		}
		Collections.sort(sorted, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((URL) o1).toExternalForm().compareTo(((URL) o2).toExternalForm());
			}
		});
		URL[] sortedURLs = (URL[]) sorted.toArray(new URL[sorted.size()]);
		for (int i = 0; i < sortedURLs.length; i++) {
			File file = new File(sortedURLs[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
				timestamp ^= file.getAbsolutePath().toLowerCase().hashCode();
			}
		}
		return timestamp;
	}

	/**
	 * Creates a new workspace model using data from the auxiliary state. Will
	 * return <code>null</code> if the project the bundle describes does not exist.
	 * If a model is returned, it will also be added to the map of workspace models.
	 * If the returned model has been added to the state, the model should be added
	 * to the model entry table using {@link #addModelToTable(IPluginModelBase)}.
	 * 
	 * @param desc bundle description to create a model for
	 * @return the creates workspace model or <code>null</code>
	 */
	private IPluginModelBase createWorkspaceModel(BundleDescription desc) {
		String projectName = fAuxiliaryState.getProject(desc.getBundleId());
		if (projectName == null)
			return null;
		IProject project = PDECore.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists())
			return null;
		IFile manifest = PDEProject.getManifest(project);
		IFile pluginXml = PDEProject.getPluginXml(project);
		IFile fragmentXml = PDEProject.getFragmentXml(project);
		if (manifest.exists()) {
			BundlePluginModelBase model = null;
			if (desc.getHost() == null)
				model = new BundlePluginModel();
			else
				model = new BundleFragmentModel();
			model.setEnabled(true);
			WorkspaceBundleModel bundle = new WorkspaceBundleModel(manifest);
			bundle.load(desc, this);
			model.setBundleDescription(desc);
			model.setBundleModel(bundle);
			bundle.setEditable(false);

			IFile file = (desc.getHost() == null) ? pluginXml : fragmentXml;
			if (file.exists()) {
				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
				extensions.setEditable(false);
				extensions.setBundleModel(model);
				extensions.load(desc, this);
				model.setExtensionsModel(extensions);
			}
			fWorkspaceModels.put(project, model);
			return model;
		}

		WorkspacePluginModelBase model = null;
		if (desc.getHost() == null)
			model = new WorkspacePluginModel(pluginXml, true);
		else
			model = new WorkspaceFragmentModel(fragmentXml, true);
		model.load(desc, this);
		model.setBundleDescription(desc);
		fWorkspaceModels.put(project, model);
		return model;
	}

	/**
	 * Creates and returns an external model for the given bundle description.
	 * 
	 * @param desc description to create a model for
	 * @return new plug-in model
	 */
	private IPluginModelBase createExternalModel(BundleDescription desc) {
		ExternalPluginModelBase model = null;
		if (desc.getHost() == null)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(desc, this);
		model.setBundleDescription(desc);
		return model;
	}

	/**
	 * Adds a model (workspace or external) to the table of model entries.
	 * Does not modify the state.  Returns the model entry containing the model.
	 * 
	 * @param model model to add
	 * @return {@link LocalModelEntry} that was created or edited to store this model
	 */
	private LocalModelEntry addModelToTable(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		LocalModelEntry entry = (LocalModelEntry) fEntries.get(id);
		if (entry == null) {
			entry = new LocalModelEntry(id);
			fEntries.put(id, entry);
		}
		entry.addModel(model);
		return entry;
	}

	/**
	 * Returns the table mapping plug-in ID to a {@link LocalModelEntry}.
	 * 
	 * @return map of String id to {@link LocalModelEntry}
	 */
	public Map getModelEntryTable() {
		return fEntries;
	}

	/**
	 * Returns the {@link IPluginModelBase} associated with the given project or
	 * <code>null</code> if a model for that project does not exist in this state.
	 * 
	 * @param project project to get the model for
	 * @return model for the given project from this state or <code>null</code>
	 */
	public IPluginModelBase getModel(IProject project) {
		return (IPluginModelBase) fWorkspaceModels.get(project);
	}

	/**
	 * Persists the contents of the state to disk, garbage collects old persisted states
	 */
	public void saveState() {
		long targetTimestamp = saveExternalState();
		long workspaceTimestamp = saveWorkspaceState();
		clearStaleStates(".target", targetTimestamp); //$NON-NLS-1$
		clearStaleStates(".workspace", workspaceTimestamp); //$NON-NLS-1$
		clearStaleStates(".cache", 0); //$NON-NLS-1$
	}

	/**
	 * Saves state associated with the external PDE target.
	 * @return timestamp used to create cache or 0
	 */
	private long saveExternalState() {
		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		URL[] urls = new URL[models.length];
		for (int i = 0; i < urls.length; i++) {
			try {
				urls[i] = new File(models[i].getInstallLocation()).toURL();
			} catch (MalformedURLException e) {
				if (DEBUG) {
					System.out.println("FAILED to save external state due to MalformedURLException"); //$NON-NLS-1$
				}
				return 0;
			}
		}
		long timestamp = computeTimestamp(urls);
		File dir = getTargetCacheDirectory(timestamp);

		boolean osgiStateExists = dir.isDirectory();
		boolean auxStateExists = fAuxiliaryState.exists(dir);
		if (!osgiStateExists || !auxStateExists) {
			if (!dir.exists())
				dir.mkdirs();
			if (DEBUG) {
				System.out.println("Saving external state of " + urls.length + " bundles to: " + dir.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			State state = stateObjectFactory.createState(false);
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = models[i].getBundleDescription();
				if (desc != null)
					state.addBundle(state.getFactory().createBundleDescription(desc));
			}
			fAuxiliaryState.savePluginInfo(dir);
			saveState(state, dir);
		} else if (DEBUG) {
			System.out.println("External state unchanged, save skipped."); //$NON-NLS-1$
		}

		return timestamp;
	}

	/**
	 * Save state associated with workspace models
	 * @return timestamp used to create cache or 0
	 */
	private long saveWorkspaceState() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		long timestamp = 0;
		if (shouldSaveState(models)) {
			URL[] urls = new URL[models.length];
			for (int i = 0; i < models.length; i++) {
				try {
					IProject project = models[i].getUnderlyingResource().getProject();
					urls[i] = new File(project.getLocation().toString()).toURL();
				} catch (MalformedURLException e) {
				}
			}
			timestamp = computeTimestamp(urls);
			File dir = getWorkspaceCacheDirectory(timestamp);
			if (DEBUG) {
				System.out.println("Saving workspace state to: " + dir.getAbsolutePath()); //$NON-NLS-1$
			}
			State state = stateObjectFactory.createState(false);
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = models[i].getBundleDescription();
				if (desc != null)
					state.addBundle(state.getFactory().createBundleDescription(desc));
			}
			saveState(state, dir);
			PDEAuxiliaryState.writePluginInfo(models, dir);
		}
		return timestamp;

	}

	private boolean shouldSaveState(IPluginModelBase[] models) {
		int nonOSGiModels = 0;
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null) {
				// not an OSGi bundle
				++nonOSGiModels;
				continue;
			}
			if (id.trim().length() == 0 || !models[i].isLoaded() || !models[i].isInSync() || models[i].getBundleDescription() == null)
				return false;
		}
		return models.length - nonOSGiModels > 0;
	}

	private void clearStaleStates(String extension, long latest) {
		File dir = new File(DIR);
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(extension) && name.length() > extension.length() && !name.equals(Long.toString(latest) + extension)) {
						CoreUtility.deleteContent(child);
					}
				}
			}
		}
	}

	public String getClassName(long bundleID) {
		return fAuxiliaryState.getClassName(bundleID);
	}

	public boolean hasExtensibleAPI(long bundleID) {
		return fAuxiliaryState.hasExtensibleAPI(bundleID);
	}

	public boolean isPatchFragment(long bundleID) {
		return fAuxiliaryState.isPatchFragment(bundleID);
	}

	public boolean hasBundleStructure(long bundleID) {
		return fAuxiliaryState.hasBundleStructure(bundleID);
	}

	public String getPluginName(long bundleID) {
		return fAuxiliaryState.getPluginName(bundleID);
	}

	public String getProviderName(long bundleID) {
		return fAuxiliaryState.getProviderName(bundleID);
	}

	public String[] getLibraryNames(long bundleID) {
		return fAuxiliaryState.getLibraryNames(bundleID);
	}

	public String getBundleLocalization(long bundleID) {
		return fAuxiliaryState.getBundleLocalization(bundleID);
	}

	public String getProject(long bundleID) {
		return fAuxiliaryState.getProject(bundleID);
	}

	public String getBundleSourceEntry(long bundleID) {
		return fAuxiliaryState.getBundleSourceEntry(bundleID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.MinimalState#addAuxiliaryData(org.eclipse.osgi.service.resolver.BundleDescription, java.util.Dictionary, boolean)
	 */
	protected void addAuxiliaryData(BundleDescription desc, Dictionary manifest, boolean hasBundleStructure) {
		fAuxiliaryState.addAuxiliaryData(desc, manifest, hasBundleStructure);
	}

	/**
	 * Returns the file directory where the state's target plug-ins are cached based on this state's timestamp
	 * @param timestamp the timestamp to lookup, see {@link #computeTimestamp(URL[])}
	 * @return the target plug-in cache directory for this state
	 */
	private File getTargetCacheDirectory(long timestamp) {
		return new File(DIR, Long.toString(timestamp) + ".target"); //$NON-NLS-1$
	}

	/**
	 * Returns the file directory where the state's workspace plug-ins are cached based on this state's timestamp
	 * @param timestamp the timestamp to lookup, see {@link #computeTimestamp(URL[])}
	 * @return the workspace plug-in cache directory for this state
	 */
	private File getWorkspaceCacheDirectory(long timestamp) {
		return new File(DIR, Long.toString(timestamp) + ".workspace"); //$NON-NLS-1$
	}

	public IPluginModelBase addWorkspaceBundle(IProject next, PluginModelDelta delta) {
		// TODO Must handle cases where the project is not actually a plug-in project anymore

		LocalModelEntry entry = (LocalModelEntry) getEntryTable().get(id);

		// add model to the corresponding ModelEntry.  Create a new entry if necessary
		if (entry == null) {
			entry = new LocalModelEntry(id);
			getEntryTable().put(id, entry);
			delta.addEntry(entry, PluginModelDelta.ADDED);
		} else {
			delta.addEntry(entry, PluginModelDelta.CHANGED);
		}
		entry.addModel(model);

		// if the model added is a workspace model, add it to the state and
		// remove all its external counterparts
		if (model.getUnderlyingResource() != null) {
			addWorkspaceBundleToState(model);
		} else if (model.isEnabled() && !entry.hasWorkspaceModels()) {
			// if a target model has went from an unchecked state to a checked state
			// on the target platform preference page, re-add its bundle description
			// to the state
			BundleDescription desc = model.getBundleDescription();
			if (desc.getContainingState().equals(fState))
				fState.addBundleDescription(desc);
		}
	}

	public IPluginModelBase removeWorkspaceBundle(IProject next, PluginModelDelta delta) {
		LocalModelEntry entry = (LocalModelEntry) getEntryTable().get(id);
		if (entry != null) {
			// remove model from the entry
			entry.removeModel(model);
			// remove corresponding bundle description from the state
			fState.removeBundleDescription(model.getBundleDescription());
			if (!entry.hasExternalModels() && !entry.hasWorkspaceModels()) {
				// remove entire entry if it has no models left
				getEntryTable().remove(id);
				delta.addEntry(entry, PluginModelDelta.REMOVED);
				return;
			} else if (model.getUnderlyingResource() != null && !entry.hasWorkspaceModels()) {
				// re-add enabled external counterparts to the state, if the last workspace
				// plug-in with a particular symbolic name is removed
				IPluginModelBase[] external = entry.getExternalModels();
				for (int i = 0; i < external.length; i++) {
					if (external[i].isEnabled())
						fState.addBundleDescription(external[i].getBundleDescription());
				}
			}
			delta.addEntry(entry, PluginModelDelta.CHANGED);
		}
	}

	public void updateWorkspaceBundle(IProject project, PluginModelDelta delta) {
		BundleDescription desc = model.getBundleDescription();
		String oldID = desc == null ? null : desc.getSymbolicName();
		String newID = model.getPluginBase().getId();

		// if the model still has no symbolic name (ie. a MANIFEST.MF without the
		// Bundle-SymbolicName header), keep ignoring it
		if (oldID == null && newID == null)
			return;

		// if the model used to lack a Bundle-SymbolicName header and now it has one,
		// treat it as a regular model addition
		if (oldID == null && newID != null) {
			handleAdd(newID, model, delta);
		} else if (oldID != null && newID == null) {
			// if the model used to have a Bundle-SymbolicName header and now it lost it,
			// treat it as a regular model removal
			handleRemove(oldID, model, delta);
			model.setBundleDescription(null);
		} else if (oldID.equals(newID)) {
			// if the workspace bundle's MANIFEST.MF was touched or
			// if the a target plug-in has now become enabled/checked, update the model
			// in the state
			if (model.isEnabled()) {
				// if the state of an inactive bundle changes (external model un/checked that has an 
				// equivalent workspace bundle), then take no action.  We don't want to add the external
				// model to the state when it is enabled if we have a workspace bundle already in the state.
				ModelEntry entry = (ModelEntry) getEntryTable().get(oldID);
				IPluginModelBase[] activeModels = entry.getActiveModels();
				boolean isActive = false;
				for (int i = 0; i < activeModels.length; i++) {
					if (activeModels[i] == model) {
						isActive = true;
						break;
					}
				}
				if (isActive)
					fState.addBundle(model, true);
			} else
				// if the target plug-in has become disabled/unchecked, remove its bundle
				// description from the state
				fState.removeBundleDescription(model.getBundleDescription());
			delta.addEntry(findEntry(oldID), PluginModelDelta.CHANGED);
		} else {
			// if the symbolic name of the bundle has completely changed,
			// remove the model from the old entry, and add the model to the new entry
			handleRemove(oldID, model, delta);
			handleAdd(newID, model, delta);
		}

	}

}
