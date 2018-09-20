/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.apputil.formula.ui.FormulaDialog;
import org.csstudio.apputil.formula.ui.InputItem;
import org.csstudio.trends.databrowser3.model.FormulaInput;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;

/** Editor (opens dialog) for FormulaItem's formula and inputs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaItemEditor
{
    /** Edit a formula item
     *  @param parent Parent node, used to position dialogs
     *  @param formula_item {@link FormulaItem} to edit
     *  @param undo Undo/redo
     *  @return <code>true</code> on success
     */
    public static boolean run(final Node parent, final FormulaItem formula_item, final UndoableActionManager undo)
    {
        final FormulaDialog dlg = new FormulaDialog(formula_item.getExpression(), determineInputs(formula_item));
        DialogHelper.positionDialog(dlg, parent, -400, -300);

        if (! dlg.showAndWait().orElse(false))
            return false;

        try
        {
            // Update formula_item from dialog
            final Model model = formula_item.getModel().get();
            final List<FormulaInput> new_inputs = new ArrayList<>();
            for (final InputItem input : dlg.getInputs())
            {
                final ModelItem item = model.getItem(input.input_name.get());
                if (item == null)
                    throw new Exception("Cannot locate formula input " + input.input_name.get());
                new_inputs.add(new FormulaInput(item, input.variable_name.get()));
            }
            // Update formula via undo-able command
            new ChangeFormulaCommand(undo, formula_item,
                    dlg.getFormula(),
                    new_inputs.toArray(new FormulaInput[new_inputs.size()]));
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update formula", ex);
            return false;
        }
        return true;
    }

    /** @return List of inputs for formula: Each model item is a possible input,
     *          mapped to a variable name that's either already used in the
     *          formula for that model item, or a simple "x1", "x2", ... when
     *          not already used
     */
    private static List<InputItem> determineInputs( final FormulaItem formula_item)
    {
        final Model model = formula_item.getModel().get();

        // Create list of inputs.
        final List<InputItem> inputs = new ArrayList<>();
        // Every model item is a possible input.
        model_loop: for (ModelItem model_item : model.getItems())
        {   // Formula cannot be an input to itself
            if (model_item == formula_item)
                continue;
            // Create InputItem for that ModelItem
            InputItem input = null;
            // See if model item is already used in the formula
            for (FormulaInput existing_input : formula_item.getInputs())
            {
                if (existing_input.getItem() == model_item)
                {   // Yes, use the existing variable name
                    input = new InputItem(model_item.getName(),
                                          existing_input.getVariableName());
                    break;
                }
            }
            // If input is unused, assign variable name x1, x2, ...
            if (input == null)
            {
                for (InputItem existing_item : inputs)
                    if (existing_item.input_name.get().equals(model_item.getName()))
                    {    // The item with the same name was already added to the input list.
                        continue model_loop;
                    }
                // Try "x1", then "xx1", "xxx1" until an unused name is found
                String var_name = Integer.toString(inputs.size()+1);
                boolean name_in_use;
                do
                {
                    name_in_use = false;
                    var_name = "x" + var_name;
                    for (final FormulaInput existing_input : formula_item.getInputs())
                        if (existing_input.getVariableName().equals(var_name))
                        {
                            name_in_use = true;
                            break;
                        }
                }
                while (name_in_use);
                input = new InputItem(model_item.getName(), var_name);
            }
            inputs.add(input);
        }
        return inputs;
    }
}
