package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;

public class BuildpathPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	private static final String KEY_PLUGIN_PROJECT_UPDATE =
		"Preferences.BuildpathPage.pluginProjectUpdate";
	private static final String KEY_FRAGMENT_PROJECT_UPDATE =
		"Preferences.BuildpathPage.fragmentProjectUpdate";
	private static final String KEY_MANIFEST_UPDATE =
		"Preferences.BuildpathPage.manifestUpdate";
	private static final String KEY_CONVERSION_UPDATE =
		"Preferences.BuildpathPage.conversionUpdate";
	private static final String KEY_DESCRIPTION =
		"Preferences.BuildpathPage.description";
	private static final String KEY_CLASSPATH_CONTAINERS =
		"Preferences.BuildpathPage.classpathContainers";

	private static final String PROP_PLUGIN_PROJECT_UPDATE =
		"Preferences.BuildpathPage.pluginProjectUpdate";
	private static final String PROP_FRAGMENT_PROJECT_UPDATE =
		"Preferences.BuildpathPage.fragmentProjectUpdate";
	private static final String PROP_MANIFEST_UPDATE =
		"Preferences.BuildpathPage.manifestUpdate";
	private static final String PROP_CONVERSION_UPDATE =
		"Preferences.BuildpathPage.conversionUpdate";
		
	private static final String PROP_CLASSPATH_CONTAINERS =
		"Preferences.BuildpathPage.useClasspathContainers";


	private static final boolean D_PLUGIN_PROJECT_UPDATE = true;
	private static final boolean D_FRAGMENT_PROJECT_UPDATE = true;
	private static final boolean D_MANIFEST_UPDATE = true;
	private static final boolean D_CONVERSION_UPDATE = true;
	private static final boolean D_CLASSPATH_CONTAINERS = false;

	public BuildpathPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
		initializeDefaults();
	}

	private static void initializeDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(PROP_PLUGIN_PROJECT_UPDATE, D_PLUGIN_PROJECT_UPDATE);
		store.setDefault(
			PROP_FRAGMENT_PROJECT_UPDATE,
			D_FRAGMENT_PROJECT_UPDATE);
		store.setDefault(PROP_MANIFEST_UPDATE, D_MANIFEST_UPDATE);
		store.setDefault(PROP_CONVERSION_UPDATE, D_CONVERSION_UPDATE);
		store.setDefault(PROP_CLASSPATH_CONTAINERS, D_CLASSPATH_CONTAINERS);
	}

	protected void createFieldEditors() {
		BooleanFieldEditor editor =
			new BooleanFieldEditor(
				PROP_PLUGIN_PROJECT_UPDATE,
				PDEPlugin.getResourceString(KEY_PLUGIN_PROJECT_UPDATE),
				getFieldEditorParent());
		addField(editor);
		editor =
			new BooleanFieldEditor(
				PROP_FRAGMENT_PROJECT_UPDATE,
				PDEPlugin.getResourceString(KEY_FRAGMENT_PROJECT_UPDATE),
				getFieldEditorParent());
		addField(editor);
		editor =
			new BooleanFieldEditor(
				PROP_MANIFEST_UPDATE,
				PDEPlugin.getResourceString(KEY_MANIFEST_UPDATE),
				getFieldEditorParent());
		addField(editor);
		editor =
			new BooleanFieldEditor(
				PROP_CONVERSION_UPDATE,
				PDEPlugin.getResourceString(KEY_CONVERSION_UPDATE),
				getFieldEditorParent());
		addField(editor);
		new Label(getFieldEditorParent(), SWT.NULL);
		editor =
			new BooleanFieldEditor(
				PROP_CLASSPATH_CONTAINERS,
				PDEPlugin.getResourceString(KEY_CLASSPATH_CONTAINERS),
				getFieldEditorParent());
		addField(editor);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.BUILDPATH_PREFERENCE_PAGE);
	}

	public static boolean isPluginProjectUpdate() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(PROP_PLUGIN_PROJECT_UPDATE, D_PLUGIN_PROJECT_UPDATE);
		return store.getBoolean(PROP_PLUGIN_PROJECT_UPDATE);
	}

	public static boolean isFragmentProjectUpdate() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(
			PROP_FRAGMENT_PROJECT_UPDATE,
			D_FRAGMENT_PROJECT_UPDATE);
		return store.getBoolean(PROP_FRAGMENT_PROJECT_UPDATE);
	}

	public static boolean isManifestUpdate() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(PROP_MANIFEST_UPDATE, D_MANIFEST_UPDATE);
		return store.getBoolean(PROP_MANIFEST_UPDATE);
	}

	public static boolean isConversionUpdate() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(PROP_CONVERSION_UPDATE, D_CONVERSION_UPDATE);
		return store.getBoolean(PROP_CONVERSION_UPDATE);
	}
	
	public static boolean getUseClasspathContainers() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(PROP_CLASSPATH_CONTAINERS, D_CLASSPATH_CONTAINERS);
		return store.getBoolean(PROP_CLASSPATH_CONTAINERS);
	}

	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}

	public boolean performOk() {
		boolean value = super.performOk();
		PDEPlugin.getDefault().savePluginPreferences();
		return value;
	}
}