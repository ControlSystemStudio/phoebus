/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.ui;

import java.util.List;

import org.csstudio.trends.databrowser3.Messages;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Dialog for formula
 *  @author Kay Kasemir
 */
public class FormulaDialog extends Dialog<Boolean>
{
    private final FormulaPane form_pane;

    public FormulaDialog(final String formula, final List<InputItem> inputs)
    {
        setTitle(Messages.Formula);
        setResizable(true);

        form_pane = new FormulaPane(formula, inputs);

        getDialogPane().setContent(form_pane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(form_pane.okProperty().not());

        setResultConverter(button -> button == ButtonType.OK);
    }

    /** @return Formula. Only valid when OK */
    public String getFormula()
    {
        return form_pane.getFormula();
    }

    /** @return {@link InputItem}s used in the formula. Only valid when OK */
    public InputItem[] getInputs()
    {
        return form_pane.getInputs();
    }

}
