/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.formula;

import java.util.List;

import org.csstudio.apputil.formula.ui.FormulaDialog;
import org.csstudio.apputil.formula.ui.FormulaPane;
import org.csstudio.apputil.formula.ui.InputItem;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

/** Demo of the {@link FormulaPane}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final List<InputItem> inputs = List.of(
            new InputItem("fred", "x"),
            new InputItem("freddy", "y"),
            new InputItem("jane", "z"),
            new InputItem("janet", "jj"));

        final FormulaDialog dlg = new FormulaDialog("2*x + y", inputs);
        final Boolean ok = dlg.showAndWait().orElse(false);
        if (ok)
        {
            System.out.println("Formula: " + dlg.getFormula());
            for (InputItem input : dlg.getInputs())
                System.out.println(input.variable_name.get() + " = " + input.input_name.get());
        }
    }

    public static void main(final String[] args)
    {
        launch(FormulaDialogDemo.class, args);
    }
}
