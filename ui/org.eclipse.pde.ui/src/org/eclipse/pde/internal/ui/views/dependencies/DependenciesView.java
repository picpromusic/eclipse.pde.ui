/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.plugin.LoopDialog;
import org.eclipse.pde.internal.ui.views.target.StateViewPage;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

public class DependenciesView extends PageBookView implements IPreferenceConstants, IHelpContextIds {

	static enum ShowMode {
		SHOW_CALLERS, SHOW_CALLEES, SHOW_PATH;

		public int getPreferenceValue() {
			return this.ordinal();
		}

		public static ShowMode find(int preferenceValue) {
			ShowMode[] values = ShowMode.values();
			if (preferenceValue < 0 || preferenceValue >= values.length) {
				return null;
			}
			return values[preferenceValue];
		}
	}

	static class DummyPart implements IWorkbenchPart {
		private IWorkbenchPartSite fSite;

		public DummyPart(IWorkbenchPartSite site) {
			fSite = site;
		}

		@Override
		public void addPropertyListener(IPropertyListener listener) {/* dummy */
		}

		@Override
		public void createPartControl(Composite parent) {/* dummy */
		}

		@Override
		public void dispose() {
			fSite = null;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public IWorkbenchPartSite getSite() {
			return fSite;
		}

		@Override
		public String getTitle() {
			return null;
		}

		@Override
		public Image getTitleImage() {
			return null;
		}

		@Override
		public String getTitleToolTip() {
			return null;
		}

		@Override
		public void removePropertyListener(IPropertyListener listener) {/* dummy */
		}

		@Override
		public void setFocus() {/* dummy */
		}
	}

	class ShowLoopsAction extends Action {

		public ShowLoopsAction() {
			super("", AS_PUSH_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowLoopsAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowLoopsAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowLoopsAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_DEP_LOOP);
			setDisabledImageDescriptor(PDEPluginImages.DESC_DEP_LOOP_DISABLED);
			setEnabled(false);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), fLoops);
			dialog.open();
		}
	}

	class ShowCalleesAction extends Action {

		public ShowCalleesAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowCalleesAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowCalleesAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowCalleesAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_CALLEES);
			setDisabledImageDescriptor(PDEPluginImages.DESC_CALLEES_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_MODE, ShowMode.SHOW_CALLEES.getPreferenceValue());
				setViewType(false);
			}
		}
	}

	class ShowCallersAction extends Action {
		public ShowCallersAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowCallersAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowCallersAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowCallersAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_CALLERS);
			setDisabledImageDescriptor(PDEPluginImages.DESC_CALLERS_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_MODE, ShowMode.SHOW_CALLERS.getPreferenceValue());
				setViewType(true);
			}
		}
	}

	class ShowPathAction extends Action {
		public ShowPathAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowCallersAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowCallersAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowCallersAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_PATH);
			setDisabledImageDescriptor(PDEPluginImages.DESC_PATH_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_MODE, ShowMode.SHOW_PATH.getPreferenceValue());
				setViewType(true);
			}
		}
	}


	class ShowListAction extends Action {
		public ShowListAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowListAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowListAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowListAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_FLAT_LAYOUT);
			setDisabledImageDescriptor(PDEPluginImages.DESC_FLAT_LAYOUT_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_LIST, true);
				setPresentation(true);
			}
		}
	}

	class ShowTreeAction extends Action {

		public ShowTreeAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEUIMessages.DependenciesView_ShowTreeAction_label);
			setDescription(PDEUIMessages.DependenciesView_ShowTreeAction_description);
			setToolTipText(PDEUIMessages.DependenciesView_ShowTreeAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_HIERARCHICAL_LAYOUT);
			setDisabledImageDescriptor(PDEPluginImages.DESC_HIERARCHICAL_LAYOUT_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		@Override
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_LIST, false);
				setPresentation(false);
			}
		}
	}

	public static final String TREE_ACTION_GROUP = "tree"; //$NON-NLS-1$

	protected static final String MEMENTO_KEY_INPUT = "inputPluginId"; //$NON-NLS-1$

	private static final DependencyLoop[] NO_LOOPS = new DependencyLoop[0];

	private Map<IPageBookViewPage, IWorkbenchPart> fPagesToParts;
	private Map<IWorkbenchPart, IPageBookViewPage> fPartsToPages;

	private Object fInput;

	private PDEPreferencesManager fPreferences = PDEPlugin.getDefault().getPreferenceManager();

	private ShowCalleesAction fShowCallees;
	private ShowCallersAction fShowCallers;
	private ShowPathAction fShowPaths;
	private ShowListAction fShowList;
	private ShowTreeAction fShowTree;
	private ShowLoopsAction fShowLoops;

	// history of input elements (as Strings). No duplicates
	private ArrayList<String> fInputHistory;

	private DependencyLoop[] fLoops;

	private HistoryDropDownAction fHistoryDropDownAction;

	private IWorkbenchPart fLastDependenciesPart = null;

	private IWorkbenchPart fPartCalleesList;
	private IWorkbenchPart fPartCalleesTree;
	private IWorkbenchPart fPartCallersList;
	private IWorkbenchPart fPartCallersTree;
	private IWorkbenchPart fPartPathTree;

	/**
	 *
	 */
	public DependenciesView() {
		super();
		fPartsToPages = new HashMap<>(4);
		fPagesToParts = new HashMap<>(4);
		fInputHistory = new ArrayList<>();
		fLoops = NO_LOOPS;
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		actionBars.updateActionBars();
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
		manager.add(new Separator(TREE_ACTION_GROUP));
		manager.add(new Separator("type")); //$NON-NLS-1$
		manager.appendToGroup("type", fShowCallees); //$NON-NLS-1$
		manager.appendToGroup("type", fShowCallers); //$NON-NLS-1$
		manager.appendToGroup("type", fShowPaths); //$NON-NLS-1$
		manager.add(new Separator("presentation")); //$NON-NLS-1$
		manager.appendToGroup("presentation", fShowTree); //$NON-NLS-1$
		manager.appendToGroup("presentation", fShowList); //$NON-NLS-1$
		manager.add(new Separator("history")); //$NON-NLS-1$
		manager.appendToGroup("history", fShowLoops); //$NON-NLS-1$
		manager.appendToGroup("history", fHistoryDropDownAction); //$NON-NLS-1$
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		return createPage(getDefaultPart());
	}

	private IWorkbenchPart getDefaultPart() {
		switch (getShowMode()) {
		case SHOW_CALLEES:
			return fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST) ? fPartCalleesList : fPartCalleesTree;
		case SHOW_PATH:
			return fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST) ? fPartPathTree : fPartPathTree;
		case SHOW_CALLERS:
			default:
			return fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST) ? fPartCallersList : fPartCallersTree;
		}

	}

	private ShowMode getShowMode() {
		ShowMode showMode = ShowMode.find(fPreferences.getInt(DEPS_VIEW_SHOW_MODE));
		showMode = showMode != null ? showMode : ShowMode.SHOW_CALLEES;
		return showMode;
	}

	/**
	 * part of the part constants
	 */
	private IPageBookViewPage createPage(IWorkbenchPart part) {
		IPageBookViewPage page;
		if (part == fPartCalleesTree) {
			page = new DependenciesViewTreePage(this, new CalleesTreeContentProvider(this));
		} else if (part == fPartCalleesList) {
			page = new DependenciesViewListPage(this, new CalleesListContentProvider(this));
		} else if (part == fPartCallersTree) {
			page = new DependenciesViewTreePage(this, new CallersTreeContentProvider(this));
		} else if (part == fPartCallersList) {
			page = new DependenciesViewListPage(this, new CallersListContentProvider(this));
		} else {
			page = new StateViewPage(this);
		}

		initPage(page);
		page.createControl(getPageBook());
		fPartsToPages.put(part, page);
		fPagesToParts.put(page, part);
		return page;
	}

	@Override
	public void createPartControl(Composite parent) {

		fShowCallees = new ShowCalleesAction();
		fShowCallers = new ShowCallersAction();
		fShowPaths = new ShowPathAction();

		switch (getShowMode()) {
		case SHOW_CALLERS:
			fShowCallers.setChecked(true);
			break;
		case SHOW_PATH:
			break;
		case SHOW_CALLEES:
		default:
			fShowCallees.setChecked(true);
		}

		fShowTree = new ShowTreeAction();
		fShowTree.setChecked(!fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST));
		fShowList = new ShowListAction();
		fShowList.setChecked(fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST));

		fShowLoops = new ShowLoopsAction();
		fShowLoops.setEnabled(fLoops != NO_LOOPS);

		fHistoryDropDownAction = new HistoryDropDownAction(this);
		fHistoryDropDownAction.setEnabled(!fInputHistory.isEmpty());

		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);

		// Create the actions before calling super so that actions added by the
		// pages get added at the end
		super.createPartControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.DEPENDENCIES_VIEW);
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		IPageBookViewPage page = fPartsToPages.get(part);
		if (page == null && !fPartsToPages.containsKey(part)) {
			page = createPage(part);
		}
		if (page != null) {
			return new PageRec(part, page);
		}
		return null;
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();

		// empty cross-reference cache
		fPartsToPages.remove(part);
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return getDefaultPart();
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fPartCalleesList = new DummyPart(site);
		fPartCalleesTree = new DummyPart(site);
		fPartCallersList = new DummyPart(site);
		fPartCallersTree = new DummyPart(site);
		fPartPathTree = new DummyPart(site);

		if (memento == null)
			return;
		String id = memento.getString(MEMENTO_KEY_INPUT);
		if (id != null) {
			IPluginModelBase plugin = PluginRegistry.findModel(id);
			if (plugin != null) {
				fInput = plugin;
				addHistoryEntry(id);
				findLoops();
			}
		}
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof DummyPart;
	}

	public void openTo(Object object) {
		if (object != null && !object.equals(fInput)) {
			if (object instanceof IPluginModelBase) {
				String id = ((IPluginModelBase) object).getPluginBase().getId();
				addHistoryEntry(id);
			}
		}
		updateInput(object);
	}

	public void openPathsFor(Object object) {
		if (!fShowPaths.isChecked()) {
			fShowPaths.setChecked(true);
			fShowCallers.setChecked(false);
			fShowCallees.setChecked(false);
			fShowCallers.run();
		}
		openTo(object);
	}

	public void openCallersFor(Object object) {
		if (!fShowCallers.isChecked()) {
			fShowCallers.setChecked(true);
			fShowCallees.setChecked(false);
			fShowPaths.setChecked(false);
			fShowCallers.run();
		}
		openTo(object);
	}

	public void openCalleesFor(Object object) {
		if (!fShowCallees.isChecked()) {
			fShowCallees.setChecked(true);
			fShowCallers.setChecked(false);
			fShowPaths.setChecked(false);
			fShowCallees.run();
		}
		openTo(object);
	}

	private void updateInput(Object object) {
		fInput = object;
		findLoops();
		((DependenciesViewPage) getCurrentPage()).setInput(object);
	}

	/**
	 *
	 */
	private void findLoops() {
		fLoops = NO_LOOPS;
		if (fInput != null && fInput instanceof IPluginModel) {
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
				@Override
				public void run() {
					IPlugin plugin = ((IPluginModel) fInput).getPlugin();
					DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
					if (loops.length > 0) {
						fLoops = loops;
					}
				}
			});
		}
		if (fShowLoops != null)
			fShowLoops.setEnabled(fLoops != NO_LOOPS);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (fInput != null && fInput instanceof IPluginModelBase) {
			String inputPluginId = ((IPluginModelBase) fInput).getPluginBase().getId();
			memento.putString(MEMENTO_KEY_INPUT, inputPluginId);
		}
	}

	void setPresentation(boolean listNotTree) {
		IWorkbenchPart currentPart = getCurrentContributingPart();
		if (listNotTree) {
			if (currentPart == fPartCalleesTree) {
				partActivated(fPartCalleesList);
			} else if (currentPart == fPartCallersTree) {
				partActivated(fPartCallersList);
			}

		} else {
			if (currentPart == fPartCalleesList) {
				partActivated(fPartCalleesTree);
			} else if (currentPart == fPartCallersList) {
				partActivated(fPartCallersTree);
			}

		}
	}

	void setViewType(boolean callers) {
		IWorkbenchPart currentPart = getCurrentContributingPart();
		if (callers) {
			if (currentPart == fPartCalleesTree) {
				partActivated(fPartCallersTree);
			} else if (currentPart == fPartCalleesList) {
				partActivated(fPartCallersList);
			}

		} else {
			if (currentPart == fPartCallersTree) {
				partActivated(fPartCalleesTree);
			} else if (currentPart == fPartCallersList) {
				partActivated(fPartCalleesList);
			}

		}
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		IPage currPage = getCurrentPage();
		// if we try to show the same page, just call super and return, no use
		// calling any custom functions
		if (pageRec.page.equals(currPage)) {
			super.showPageRec(pageRec);
			return;
		}
		IStructuredSelection selection = null;
		if (currPage instanceof DependenciesViewPage) {
			selection = ((DependenciesViewPage) currPage).getSelection();
			((DependenciesViewPage) currPage).setActive(false);
		}
		IPage p = pageRec.page;
		if (p instanceof DependenciesViewPage) {
			((DependenciesViewPage) p).setInput(fInput);
			// configure view before actually showing it
			((DependenciesViewPage) p).setActive(true);
		}
		super.showPageRec(pageRec);
		if (p instanceof DependenciesViewPage) {
			updateTitle(fInput);
			((DependenciesViewPage) p).setSelection(selection);
		}
	}

	void updateTitle(Object newInput) {
		if (newInput == null) {
			updateTitle(""); //$NON-NLS-1$
		} else if (!newInput.equals(PDECore.getDefault().getModelManager())) {
			String name = PDEPlugin.getDefault().getLabelProvider().getText(newInput);
			String title;
			if (getCurrentContributingPart() == fPartCalleesTree) {
				title = NLS.bind(PDEUIMessages.DependenciesView_callees_tree_title, name);
			} else if (getCurrentContributingPart() == fPartCalleesList) {
				title = NLS.bind(PDEUIMessages.DependenciesView_callees_list_title, name);
			} else if (getCurrentContributingPart() == fPartCallersTree) {
				title = NLS.bind(PDEUIMessages.DependenciesView_callers_tree_title, name);
			} else {
				title = NLS.bind(PDEUIMessages.DependenciesView_callers_list_title, name);
			}
			if (fLoops != NO_LOOPS) {
				title = title + " " + PDEUIMessages.DependenciesView_cycles_title; //$NON-NLS-1$
			}
			updateTitle(title);
		}
	}

	void updateTitle(String description) {
		setContentDescription(description);
		setTitleToolTip(getTitle());
	}

	/**
	 * Adds the entry if new. Inserted at the beginning of the history entries
	 * list.
	 *
	 * @param entry
	 *            The new entry
	 */
	private void addHistoryEntry(String entry) {
		if (fInputHistory.contains(entry)) {
			fInputHistory.remove(entry);
		}
		fInputHistory.add(0, entry);
		if (fHistoryDropDownAction != null)
			fHistoryDropDownAction.setEnabled(true);
	}

	private void updateHistoryEntries() {
		for (int i = fInputHistory.size() - 1; i >= 0; i--) {
			String type = fInputHistory.get(i);
			if (PluginRegistry.findModel(type) == null) {
				fInputHistory.remove(i);
			}
		}
		if (fHistoryDropDownAction != null)
			fHistoryDropDownAction.setEnabled(!fInputHistory.isEmpty());
	}

	/**
	 * Goes to the selected entry, without updating the order of history
	 * entries.
	 *
	 * @param entry
	 *            The entry to open
	 */
	public void gotoHistoryEntry(String entry) {
		if (fInputHistory.contains(entry)) {
			updateInput(PluginRegistry.findModel(entry));
		}
	}

	/**
	 * Gets all history entries.
	 *
	 * @return All history entries
	 */
	public String[] getHistoryEntries() {
		if (fInputHistory.size() > 0) {
			updateHistoryEntries();
		}
		return fInputHistory.toArray(new String[fInputHistory.size()]);
	}

	/**
	 * Sets the history entries
	 *
	 * @param elems
	 *            The history elements to set
	 */
	public void setHistoryEntries(String[] elems) {
		fInputHistory.clear();
		for (int i = 0; i < elems.length; i++) {
			fInputHistory.add(elems[i]);
		}
		updateHistoryEntries();
	}

	/**
	 * @return Returns the fInput.
	 */
	public String getInput() {
		if (fInput != null) {
			return ((IPluginModelBase) fInput).getPluginBase().getId();
		}
		return null;
	}

	public boolean isShowingCallers() {
		return getShowMode() == ShowMode.SHOW_CALLERS;
	}

	protected void enableStateView(boolean enabled) {
		if (fLastDependenciesPart != null)
			partActivated(fLastDependenciesPart);
		else
			partActivated(getDefaultPart());
		fLastDependenciesPart = null;
		getViewSite().getActionBars().getToolBarManager().update(true);
	}

	@Override
	public void dispose() {
		super.dispose();
		fPartCalleesList.dispose();
		fPartCalleesTree.dispose();
		fPartCallersList.dispose();
		fPartCallersTree.dispose();
	}

}
