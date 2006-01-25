/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Implementation of the AbstractTemplateOption that allows users to choose a value from
 * the fixed set of options using radio buttons.
 * 
 * @since 3.2
 */
public class RadioChoiceOption extends AbstractChoiceOption {
	
	private Button[] fButtons;
	private Label fLabel;

	/**
	 * Constructor for RadioChoiceOption.
	 * When creating options with more than 2 choices it is suggested to use
	 * ComboChoiceOption instead of RadioChoiceOption
	 * 
	 * @param section
	 *            the parent section.
	 * @param name
	 *            the unique name
	 * @param label
	 *            the presentable label
	 * @param choices
	 *            the list of choices from which the value can be chosen. Each
	 *            array entry should be an array of size 2, where position 0
	 *            will be interpeted as the choice unique name, and position 1
	 *            as the choice presentable label.
	 */
	public RadioChoiceOption(BaseOptionTemplateSection section, String name,
			String label, String[][] choices) {
		super(section, name, label, choices);
	}

	private Button createRadioButton(Composite parent, int span, String[] choice) {
		Button button = new Button(parent, SWT.RADIO);
		button.setData(choice[0]);
		button.setText(choice[1]);
		GridData gd = fill(button, span);
		gd.horizontalIndent = 10;
		return button;
	}


	public void createControl(Composite parent, int span) {
		
		fLabel = createLabel(parent, 1);
		fLabel.setEnabled(isEnabled());
		fill(fLabel, 1);
		
		Composite radioComp = createComposite(parent, span - 1);
		fill(radioComp, span - 1);
		GridLayout layout = new GridLayout(fChoices.length, true);
		layout.marginWidth = layout.marginHeight = 0;
		radioComp.setLayout(layout);
		
		fButtons = new Button[fChoices.length];

		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.widget;
				if (isBlocked())
					return;
				if (b.getSelection()) {
					setValue(b.getData().toString());
					getSection().validateOptions(RadioChoiceOption.this);
				}
			}
		};

		for (int i = 0; i < fChoices.length; i++) {
			Button button = createRadioButton(radioComp, 1, fChoices[i]);
			fButtons[i] = button;
			button.addSelectionListener(listener);
			button.setEnabled(isEnabled());
		}
		
		if (getChoice() != null)
			selectChoice(getChoice());
	}

	protected void setOptionValue(Object value) {
		if (fButtons != null && value != null) {
			selectChoice(value.toString());
		}
	}

	protected void setOptionEnabled(boolean enabled) {
		if (fLabel != null) {
			fLabel.setEnabled(enabled);
			for (int i = 0; i < fButtons.length; i++) {
				fButtons[i].setEnabled(enabled);
			}
		}
	}

	protected void selectOptionChoice(String choice) {
		for (int i = 0; i < fButtons.length; i++) {
			Button button = fButtons[i];
			String bname = button.getData().toString();
			if (bname.equals(choice)) {
				button.setSelection(true);
			} else {
				button.setSelection(false);
			}
		}
	}
}
