package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.actions.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.model.ModelDataTransfer;
import org.eclipse.ui.texteditor.*;
import java.util.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.model.IModel;

public abstract class PDEEditorContributor extends EditorActionBarContributor {
	public static final String ACTIONS_SAVE = "EditorActions.save";
	public static final String ACTIONS_CUT = "EditorActions.cut";
	public static final String ACTIONS_COPY = "EditorActions.copy";
	public static final String ACTIONS_PASTE = "EditorActions.paste";
	private PDEMultiPageEditor editor;
	private IPDEEditorPage page;
	private SaveAction saveAction;
	private ClipboardAction cutAction;
	private ClipboardAction copyAction;
	private ClipboardAction pasteAction;
	private Hashtable globalActions = new Hashtable();
	private String menuName;

	class GlobalAction extends Action implements IUpdate {
		private String id;
		public GlobalAction(String id) {
			this.id = id;
		}
		public void run() {
			editor.performGlobalAction(id);
		}
		public void update() {
			getActionBars().updateActionBars();
		}
	}

	class ClipboardAction extends GlobalAction {
		public ClipboardAction(String id) {
			super(id);
			setEnabled(false);
		}
		public void selectionChanged(ISelection selection) {
		}
		public boolean isEditable() {
			return ((IModel)editor.getModel()).isEditable();
		}
	}

	class CutAction extends ClipboardAction {
		public CutAction() {
			super(ITextEditorActionConstants.CUT);
			setText(PDEPlugin.getResourceString(ACTIONS_CUT));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(isEditable() && selection.isEmpty() == false);
		}
	}

	class CopyAction extends ClipboardAction {
		public CopyAction() {
			super(ITextEditorActionConstants.COPY);
			setText(PDEPlugin.getResourceString(ACTIONS_COPY));
		}
		public void selectionChanged(ISelection selection) {
			setEnabled(selection.isEmpty() == false);
		}
	}

	class PasteAction extends ClipboardAction {
		public PasteAction() {
			super(ITextEditorActionConstants.PASTE);
			setText(PDEPlugin.getResourceString(ACTIONS_PASTE));
		}
		public void selectionChanged(ISelection selection) {
			boolean enabled = isEditable();
			if (enabled) {
				String[] typeNames = editor.getClipboard().getAvailableTypeNames();
				boolean knownType = false;
				for (int i = 0; i < typeNames.length; i++) {
					String typeName = typeNames[i];
					if (typeName.startsWith(ModelDataTransfer.TYPE_PREFIX)) {
						knownType = true;
						break;
					}
				}
				enabled = knownType;
			}
			setEnabled(enabled);
		}
	}

	class SaveAction extends Action implements IUpdate {
		public SaveAction() {
		}
		public void run() {
			if (editor != null)
				PDEPlugin.getActivePage().saveEditor(editor, false);
		}
		public void update() {
			if (editor != null) {
				setEnabled(editor.isDirty());
			} else
				setEnabled(false);
		}
	}

	public PDEEditorContributor(String menuName) {
		this.menuName = menuName;
		makeActions();
	}
	private void addGlobalAction(String id) {
		GlobalAction action = new GlobalAction(id);
		addGlobalAction(id, action);
	}
	private void addGlobalAction(String id, Action action) {
		globalActions.put(id, action);
	}
	public void contextMenuAboutToShow(IMenuManager mng) {
		mng.add(cutAction);
		mng.add(copyAction);
		mng.add(pasteAction);
		mng.add(new Separator());
		mng.add(saveAction);
	}
	public void contributeToMenu(IMenuManager mm) {
	}
	public void contributeToStatusLine(IStatusLineManager slm) {
	}
	public void contributeToToolBar(IToolBarManager tbm) {
	}
	public PDEMultiPageEditor getEditor() {
		return editor;
	}
	public IAction getGlobalAction(String id) {
		return (IAction) globalActions.get(id);
	}

	public IAction getSaveAction() {
		return saveAction;
	}
	public IStatusLineManager getStatusLineManager() {
		return getActionBars().getStatusLineManager();
	}

	protected void makeActions() {
		// clipboard actions
		cutAction = new CutAction();
		copyAction = new CopyAction();
		pasteAction = new PasteAction();
		addGlobalAction(ITextEditorActionConstants.CUT, cutAction);
		addGlobalAction(ITextEditorActionConstants.COPY, copyAction);
		addGlobalAction(ITextEditorActionConstants.PASTE, pasteAction);

		addGlobalAction(ITextEditorActionConstants.DELETE);
		addGlobalAction(ITextEditorActionConstants.UNDO);
		addGlobalAction(ITextEditorActionConstants.REDO);
		addGlobalAction(ITextEditorActionConstants.SELECT_ALL);
		addGlobalAction(ITextEditorActionConstants.FIND);
		addGlobalAction(ITextEditorActionConstants.BOOKMARK);

		saveAction = new SaveAction();
		saveAction.setText(PDEPlugin.getResourceString(ACTIONS_SAVE));

	}
	public void setActiveEditor(IEditorPart targetEditor) {
		if (editor != null)
			editor.updateUndo(null, null);
		this.editor = (PDEMultiPageEditor) targetEditor;
		editor.updateUndo(
			getGlobalAction(ITextEditorActionConstants.UNDO),
			getGlobalAction(ITextEditorActionConstants.REDO));
		IPDEEditorPage page = editor.getCurrentPage();
		setActivePage(page);
		updateSelectableActions(editor.getSelection());
	}
	public void setActivePage(IPDEEditorPage newPage) {
		IPDEEditorPage oldPage = page;
		this.page = newPage;
		if (newPage == null)
			return;
		updateActions();
		if (oldPage != null
			&& oldPage.isSource() == false
			&& newPage.isSource() == false)
			return;

		IActionBars bars = getActionBars();
		// update global actions
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.DELETE,
			page.getAction(ITextEditorActionConstants.DELETE));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.UNDO,
			page.getAction(ITextEditorActionConstants.UNDO));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.REDO,
			page.getAction(ITextEditorActionConstants.REDO));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.CUT,
			page.getAction(ITextEditorActionConstants.CUT));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.COPY,
			page.getAction(ITextEditorActionConstants.COPY));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.PASTE,
			page.getAction(ITextEditorActionConstants.PASTE));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.SELECT_ALL,
			page.getAction(ITextEditorActionConstants.SELECT_ALL));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.FIND,
			page.getAction(ITextEditorActionConstants.FIND));
		bars.setGlobalActionHandler(
			IWorkbenchActionConstants.BOOKMARK,
			page.getAction(ITextEditorActionConstants.BOOKMARK));
		bars.updateActionBars();
	}
	public void updateActions() {
		saveAction.update();
	}

	public void updateSelectableActions(ISelection selection) {
		cutAction.selectionChanged(selection);
		copyAction.selectionChanged(selection);
		pasteAction.selectionChanged(selection);
	}
}