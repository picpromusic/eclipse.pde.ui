/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.PropertyDialogAction;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.impl.IUBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.provisioner.p2.ProvisionerMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page allowing users to select which IUs they would like to download
 * 
 * @since 3.5
 */
public class EditIUContainerPage extends WizardPage implements IEditBundleContainerPage {

	// Errors on the page
	private static final IStatus BAD_IU_SELECTION = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), ProvisionerMessages.P2TargetProvisionerWizardPage_1);
	private IStatus fSelectedIUStatus = Status.OK_STATUS;

	private static final String SETTINGS_GROUP_BY_CATEGORY = "groupByCategory";
	private static final String SETTINGS_SHOW_OLD_VERSIONS = "showVersions";
	private static final String SETTINGS_SELECTED_REPOSITORY = "selectedRepository";

	/**
	 * If the user is only downloading from a specific repository location, we store it here so it can be persisted in the target
	 */
	private URI fRepoLocation;

	/**
	 * If not null, this wizard is editing an existing bundle container instead of creating a new one from scratch
	 */
	private IUBundleContainer fEditContainer;

	private IProfile fProfile;
	private IUViewQueryContext fQueryContext;

	private RepositorySelectionGroup fRepoSelector;
	private AvailableIUGroup fAvailableIUGroup;
	private Button fPropertiesButton;
	private IAction fPropertyAction;
	private Button fShowCategoriesButton;
	private Button fShowOldVersionsButton;
	private Text fDetailsText;

	protected EditIUContainerPage(IProfile profile) {
		super("AddP2Container"); //$NON-NLS-1$
		fProfile = profile;
	}

	protected EditIUContainerPage(IUBundleContainer container, IProfile profile) {
		this(profile);
		fEditContainer = container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditBundleContainerPage#getBundleContainer()
	 */
	public IBundleContainer getBundleContainer() {
		// TODO Do we need to throw away the profile in case they have edited and removed IUs?
		ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
		if (service == null) {
			PDEPlugin.log(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "The target platform service could not be acquired."));
		}
		return service.newIUContainer(fAvailableIUGroup.getCheckedLeafIUs(), fRepoLocation != null ? new URI[] {fRepoLocation} : null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.IEditBundleContainerPage#storeSettings()
	 */
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(SETTINGS_GROUP_BY_CATEGORY, fShowCategoriesButton.getSelection());
			settings.put(SETTINGS_SHOW_OLD_VERSIONS, fShowOldVersionsButton.getSelection());
			settings.put(SETTINGS_SELECTED_REPOSITORY, fRepoLocation != null ? fRepoLocation.toString() : null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		setMessage("Select content from a repository to be downloaded and added to your target");
		setTitle("Add Repository or Update Site");
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		createQueryContext();
		createRepositoryComboArea(composite);
		createAvailableIUArea(composite);
		createDetailsArea(composite);
		createCheckboxArea(composite);

		setPageComplete(false);
		restoreWidgetState();
		updateViewContext();
		setControl(composite);
		setPageComplete(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.P2_PROVISIONING_PAGE);
	}

	private void createRepositoryComboArea(Composite parent) {
		fRepoSelector = new RepositorySelectionGroup(getContainer(), parent, Policy.getDefault(), fQueryContext);
		fRepoSelector.addRepositorySelectionListener(new IRepositorySelectionListener() {
			public void repositorySelectionChanged(int repoChoice, URI repoLocation) {
				fAvailableIUGroup.setRepositoryFilter(repoChoice, repoLocation);
				fRepoLocation = repoChoice == AvailableIUGroup.AVAILABLE_SPECIFIED ? repoLocation : null;
				if (repoChoice == AvailableIUGroup.AVAILABLE_NONE) {
					setDescription("Select a site or enter the location of a site.");
				} else {
					setDescription("Check the items that you wish to add to your target platform");
				}
				pageChanged();
			}
		});
	}

	/**
	 * Create the UI area where the user will be able to select which IUs they
	 * would like to download.  There will also be buttons to see properties for
	 * the selection and open the manage sites dialog.
	 * 
	 * @param parent parent composite
	 */
	private void createAvailableIUArea(Composite parent) {
		fAvailableIUGroup = new AvailableIUGroup(parent);
		fAvailableIUGroup.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
				if (units.length > 0) {
					fSelectedIUStatus = Status.OK_STATUS;
				} else {
					fSelectedIUStatus = BAD_IU_SELECTION;
				}
				pageChanged();
			}
		});
		fAvailableIUGroup.getCheckboxTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetails();
				fPropertiesButton.setEnabled(fAvailableIUGroup.getSelectedIUElements().length == 1);
			}
		});

		fAvailableIUGroup.setUseBoldFontForFilteredItems(true);
		GridData data = (GridData) fAvailableIUGroup.getStructuredViewer().getControl().getLayoutData();
		data.heightHint = 200;
	}

	private void createDetailsArea(Composite parent) {
		Group detailsGroup = SWTFactory.createGroup(parent, "Details", 1, 1, GridData.FILL_HORIZONTAL);

		fDetailsText = SWTFactory.createText(detailsGroup, SWT.WRAP | SWT.READ_ONLY, 1, GridData.FILL_HORIZONTAL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		// TODO Use a height based on the font
//		gd.heightHint = Dialog.convertHeightInCharsToPixels(, ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.heightHint = 50;
		fDetailsText.setLayoutData(gd);

		// TODO Use a link instead of a button?
		fPropertiesButton = SWTFactory.createPushButton(detailsGroup, ProvisionerMessages.P2TargetProvisionerWizardPage_10, null);
		((GridData) fPropertiesButton.getLayoutData()).horizontalAlignment = SWT.RIGHT;
		fPropertiesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fPropertyAction.run();
			}
		});
		fPropertyAction = new PropertyDialogAction(new SameShellProvider(getShell()), fAvailableIUGroup.getStructuredViewer());
		fPropertiesButton.setEnabled(false);
	}

	private void createCheckboxArea(Composite parent) {
		Composite checkComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);
		fShowCategoriesButton = SWTFactory.createCheckButton(checkComp, "&Group by Category", null, true, 1);
		fShowCategoriesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateViewContext();
			}
		});
		fShowOldVersionsButton = SWTFactory.createCheckButton(checkComp, "Show only the latest &version", null, true, 1);
		fShowOldVersionsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateViewContext();
			}
		});
	}

	private void createQueryContext() {
		fQueryContext = Policy.getDefault().getQueryContext();
		fQueryContext.setInstalledProfileId(fProfile.getProfileId());
		fQueryContext.showAlreadyInstalled();
	}

	private void updateViewContext() {
		if (fShowCategoriesButton.getSelection()) {
			fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		} else {
			fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		}
		fQueryContext.setShowLatestVersionsOnly(fShowOldVersionsButton.getSelection());
		fAvailableIUGroup.updateAvailableViewState();
	}

	/**
	 * Update the details section of the page using the currently selected IU
	 */
	private void updateDetails() {
		IInstallableUnit[] selected = fAvailableIUGroup.getSelectedIUs();
		if (selected.length == 1) {
			StringBuffer result = new StringBuffer();
			String description = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_DESCRIPTION);
			if (description != null) {
				result.append(description);
			} else {
				String name = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_NAME);
				if (name != null)
					result.append(name);
				else
					result.append(selected[0].getId());
				result.append(" "); //$NON-NLS-1$
				result.append(selected[0].getVersion().toString());
			}

			fDetailsText.setText(result.toString());
			return;
		}
		fDetailsText.setText(""); //$NON-NLS-1$
	}

//	private Link createLink(Composite parent, IAction action, String text) {
//		Link link = new Link(parent, SWT.PUSH);
//		link.setText(text);
//
//		link.addListener(SWT.Selection, new Listener() {
//			public void handleEvent(Event event) {
//				IAction linkAction = getLinkAction(event.widget);
//				if (linkAction != null) {
//					linkAction.runWithEvent(event);
//				}
//			}
//		});
//		link.setToolTipText(action.getToolTipText());
//		link.setData(LINKACTION, action);
//		return link;
//	}

	/**
	 * Checks if the page is complete, updating messages and finish button.
	 */
	void pageChanged() {
		if (fSelectedIUStatus.getSeverity() == IStatus.ERROR) {
			setErrorMessage(fSelectedIUStatus.getMessage());
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * Restores the state of the wizard from previous invocations
	 */
	private void restoreWidgetState() {
		IDialogSettings settings = getDialogSettings();
		URI uri = null;
		boolean showCategories = fQueryContext.shouldGroupByCategories();
		boolean showOldVersions = fQueryContext.getShowLatestVersionsOnly();

		// Init the checkboxes and repo selector combo
		if (fEditContainer != null) {
			if (fEditContainer.getRepositories() != null) {
				uri = fEditContainer.getRepositories()[0];
			}
		} else if (settings != null) {
			String stringURI = settings.get(SETTINGS_SELECTED_REPOSITORY);
			if (stringURI != null) {
				try {
					uri = new URI(stringURI);
				} catch (URISyntaxException e) {
					PDEPlugin.log(e);
				}
			}
			if (settings.get(SETTINGS_GROUP_BY_CATEGORY) != null) {
				showCategories = settings.getBoolean(SETTINGS_GROUP_BY_CATEGORY);
			}
			if (settings.get(SETTINGS_SHOW_OLD_VERSIONS) != null) {
				showOldVersions = settings.getBoolean(SETTINGS_SHOW_OLD_VERSIONS);
			}
		}

		if (uri != null) {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_SPECIFIED, uri);
		} else if (fEditContainer != null) {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_ALL, null);
		} else {
			fRepoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_NONE, null);
		}

		fShowCategoriesButton.setSelection(showCategories);
		fShowOldVersionsButton.setSelection(showOldVersions);

		updateViewContext();
		fRepoSelector.getDefaultFocusControl().setFocus();
		updateDetails();

		// If we are editing a bundle check any installable units
		if (fEditContainer != null) {
			try {
				// TODO This code does not do a good job, selecting, revealing, and collapsing all
				// Only able to check items if we don't have categories
				fQueryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
				fAvailableIUGroup.updateAvailableViewState();
				fAvailableIUGroup.setChecked(fEditContainer.getInstallableUnits(fProfile));
				IInstallableUnit[] units = fAvailableIUGroup.getCheckedLeafIUs();
				if (units.length > 0) {
					fAvailableIUGroup.getCheckboxTreeViewer().setSelection(new StructuredSelection(units[0]), true);
				}
				// Make sure view is back in proper state
				updateViewContext();
				fAvailableIUGroup.getCheckboxTreeViewer().collapseAll();
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}

}
