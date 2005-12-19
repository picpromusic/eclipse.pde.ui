/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;


import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class TargetPlatformPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final int PLUGINS_INDEX = 0;
	public static final int ENVIRONMENT_INDEX = 1;
	public static final int SOURCE_INDEX = 2;
	
	private Label fHomeLabel;
	private Combo fHomeText;
	private Combo fProfileCombo;
	private Button fBrowseButton;
	private TargetPluginsTab fPluginsTab;
	private TargetEnvironmentTab fEnvironmentTab;
	private TargetSourceTab fSourceTab;
	private JavaArgumentsTab fArgumentsTab;
	
	private Preferences fPreferences = null;
	private boolean fNeedsReload = false;
	private String fOriginalText;
	private int fIndex;
	private TabFolder fTabFolder;
	
	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		this(PLUGINS_INDEX);
	}
	
	public TargetPlatformPreferencePage(int index) {
		//setDescription(PDEUIMessages.Preferences_TargetPlatformPage_Description); 
		fPreferences = PDECore.getDefault().getPluginPreferences();
		fPluginsTab = new TargetPluginsTab(this);
		fIndex = index;
	}
	
	public void dispose() {
		fPluginsTab.dispose();
		fSourceTab.dispose();
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);

		Group profiles = new Group(container, SWT.NONE);
		profiles.setText(PDEUIMessages.TargetPlatformPreferencePage_TargetGroupTitle);
		layout = new GridLayout(5, false);
		profiles.setLayout(layout);
		profiles.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label currentProfile = new Label(profiles, SWT.NONE);
		currentProfile.setText(PDEUIMessages.TargetPlatformPreferencePage_CurrentProfileLabel);
		
		fProfileCombo = new Combo(profiles, SWT.BORDER | SWT.READ_ONLY);
		loadTargetCombo();
		fProfileCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button profileBrowse = new Button(profiles, SWT.PUSH);
		profileBrowse.setText(PDEUIMessages.TargetPlatformPreferencePage_BrowseButton);
		GridData gd = new GridData();
		gd.widthHint = 60;
		profileBrowse.setLayoutData(gd);
		profileBrowse.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				handleTargetBrowse();
			}
			
		});
		
		Button profilePreview = new Button(profiles, SWT.PUSH);
		profilePreview.setText(PDEUIMessages.TargetPlatformPreferencePage_PreviewButton);
		gd = new GridData();
		gd.widthHint = 60;
		profilePreview.setLayoutData(gd);
		
		Button profileApply = new Button(profiles, SWT.PUSH);
		profileApply.setText(PDEUIMessages.TargetPlatformPreferencePage_ApplyButton);
		gd = new GridData();
		gd.widthHint = 60;
		profileApply.setLayoutData(gd);
		
		Group target = new Group(container, SWT.NONE);
		target.setText(PDEUIMessages.TargetPlatformPreferencePage_ProfileGroupTitle);
		layout = new GridLayout(3, false);
		target.setLayout(layout);
		target.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fHomeLabel = new Label(target, SWT.NULL);
		fHomeLabel.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome); 
		
		fHomeText = new Combo(target, SWT.NONE);
		fHomeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ArrayList locations = new ArrayList();
		for (int i = 0; i < 5; i++) {
			String value = fPreferences.getString(ICoreConstants.SAVED_PLATFORM + i);
			if (value.equals(""))  //$NON-NLS-1$
				break;
			locations.add(value);
		}
		String homeLocation = fPreferences.getString(ICoreConstants.PLATFORM_PATH);
		if (!locations.contains(homeLocation))
			locations.add(0, homeLocation);
		fHomeText.setItems((String[])locations.toArray(new String[locations.size()]));
		fHomeText.setText(homeLocation);
		fOriginalText = fHomeText.getText();
		fHomeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fNeedsReload = true;
			}
		});
		fHomeText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginsTab.handleReload();
				fNeedsReload = false;
			}
		});
		
		fBrowseButton = new Button(target, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome_Button); 
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fBrowseButton);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		
		fTabFolder = new TabFolder(target, SWT.NONE);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		fTabFolder.setLayoutData(gd);
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {	
			createPluginsTab(fTabFolder);
			createEnvironmentTab(fTabFolder);
			createArgumentsTab(fTabFolder);
			createSourceTab(fTabFolder);
			fTabFolder.setSelection(fIndex);
			}
		});
		
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fTabFolder.getSelectionIndex() == ENVIRONMENT_INDEX) {
					fEnvironmentTab.updateChoices();
				}
			}
		});
		
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}
	
	private void createPluginsTab(TabFolder folder) {
		Control block = fPluginsTab.createContents(folder);
		block.setLayoutData(new GridData(GridData.FILL_BOTH));	
		fPluginsTab.initialize();

		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_pluginsTab); 
		tab.setControl(block);	
	}
	
	private void createEnvironmentTab(TabFolder folder) {
		fEnvironmentTab = new TargetEnvironmentTab();
		Control block = fEnvironmentTab.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_environmentTab); 
		tab.setControl(block);
	}
	
	private void createSourceTab(TabFolder folder) {
		fSourceTab = new TargetSourceTab();
		Control block = fSourceTab.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_sourceCode);  
		tab.setControl(block);
	}
	
	private void createArgumentsTab(TabFolder folder) {
		fArgumentsTab = new JavaArgumentsTab(this);
		Control block = fArgumentsTab.createControl(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_agrumentsTab);
		tab.setControl(block);
	}

	String getPlatformPath() {
		return fHomeText.getText();
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		if (fHomeText.getText().length() > 0)
			dialog.setFilterPath(fHomeText.getText());
		String newPath = dialog.open();
		if (newPath != null
				&& !ExternalModelManager.arePathsEqual(new Path(fHomeText.getText()), new Path(newPath))) {
			if (fHomeText.indexOf(newPath) == -1)
				fHomeText.add(newPath, 0);
			fHomeText.setText(newPath);
			fPluginsTab.handleReload();
			fNeedsReload = false;
		}
	}
	
	private void loadTargetCombo() {
//		String pref = fPreferences.getString(ICoreConstants.TARGET_PROFILE);
//		if (pref.indexOf('\\') > 0) {
//			IPath targetPath = new Path(pref);
//			IFile file = PDEPlugin.getWorkspace().getRoot().getFile(targetPath);
//			if (file != null) { 
//				TargetModel model = new TargetModel();
//				try {
//					model.load(file.getContents(), false);
//					String value = model.getTarget().getName();
//					value = value + " [" + file.getFullPath().toOSString() + "]";
//					if (fProfileCombo.indexOf(value) == -1)
//						fProfileCombo.add(value, 0);
//					fProfileCombo.setText(value);
//				} catch (CoreException e) {
//				}
//			}
//		} else {
//			prefId = pref.substring(3);
//		}
		
		IConfigurationElement[] elems = PDECore.getDefault().getTargetProfileManager().getValidTargets();
		for (int i = 0; i < elems.length; i++) {
			String name = elems[i].getAttribute("name"); //$NON-NLS-1$
//			String id = elems[i].getAttribute("id");
//			name = name + " [" + id + "]";
			if (fProfileCombo.indexOf(name) == -1)
				fProfileCombo.add(name);
//			if (id.equals(prefId))
//				fProfileCombo.setText(name);
		}
	}
	
	private void handleTargetBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.TargetPlatformPreferencePage_FileSelectionTitle); 
		dialog.setMessage(PDEUIMessages.TargetPlatformPreferencePage_FileSelectionMessage); 
		dialog.addFilter(new FileExtensionFilter("target"));  //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		IFile target = getTargetFile();
		if (target != null) dialog.setInitialSelection(target);

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			//String value = file.getFullPath().toOSString();
			TargetModel model = new TargetModel();
			try {
				model.load(file.getContents(), false);
				String value = model.getTarget().getName();
				value = value + " [" + file.getFullPath().toOSString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				if (fProfileCombo.indexOf(value) == -1)
					fProfileCombo.add(value, 0);
				fProfileCombo.setText(value);
			} catch (CoreException e) {
			}
		}
	}
	
	private IFile getTargetFile() {
		String target = fProfileCombo.getText().trim();
		if (target.equals("")) //$NON-NLS-1$
			return null;
		IPath targetPath = new Path(target);
		if (targetPath.segmentCount() < 2) 
			return null;
		return PDEPlugin.getWorkspace().getRoot().getFile(targetPath);
	}

	public void init(IWorkbench workbench) {
	}
	
	public void performDefaults() {
		fHomeText.setText(ExternalModelManager.computeDefaultPlatformPath());
		fPluginsTab.handleReload();
		fEnvironmentTab.performDefaults();
		fArgumentsTab.performDefaults();
		fSourceTab.performDefaults();
		super.performDefaults();
	}

	public boolean performOk() {
		fEnvironmentTab.performOk();
		if (fNeedsReload && !ExternalModelManager.arePathsEqual(new Path(fOriginalText), new Path(fHomeText.getText()))) {
			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					PDEUIMessages.Preferences_TargetPlatformPage_title, 
					null,
					PDEUIMessages.Preferences_TargetPlatformPage_question, 
					MessageDialog.QUESTION,
					new String[] {
						IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL},
					1);
			if (dialog.open() == 1) {
				getContainer().updateButtons();
				return false;
			}
			fPluginsTab.handleReload();
		} 
		fSourceTab.performOk();
		fPluginsTab.performOk();
		fArgumentsTab.performOk();
		saveTarget();
		return super.performOk();
	}
	
	private void saveTarget() {
//		String value = fProfileCombo.getText().trim();
//		int index = value.lastIndexOf('[');
//		value = value.substring(index, value.length() - 1);
//		fPreferences.setValue(ICoreConstants.TARGET_PROFILE, value);
	}
	
	public String[] getPlatformLocations() {
		return fHomeText.getItems();
	}
	 
	public void resetNeedsReload() {
		fNeedsReload = false;
		String location = fHomeText.getText();
		if (fHomeText.indexOf(location) == -1)
			fHomeText.add(location, 0);
	}
	
	public TargetSourceTab getSourceBlock() {
		return fSourceTab;
	}
}
