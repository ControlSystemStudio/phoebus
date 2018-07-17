/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.FormulaInput;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change a FormulaItem's expression and inputs
 *  @author Kay Kasemir
 */
public class ChangeFormulaCommand extends UndoableAction
{
    final private FormulaItem formula;
    final private String old_expression, new_expression;
    final private FormulaInput old_inputs[], new_inputs[];

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param formula Model item to configure
     *  @param expression Formula expression
     *  @param inputs Inputs to formula
     *  @throws Exception on error
     */
    public ChangeFormulaCommand(
            final UndoableActionManager operations_manager,
            final FormulaItem formula, final String expression,
            final FormulaInput inputs[]) throws Exception
    {
        super(Messages.Formula);
        this.formula = formula;
        this.old_expression = formula.getExpression();
        this.old_inputs = formula.getInputs();
        this.new_expression = expression;
        this.new_inputs = inputs;
        // Potentially throw exception before registering for undo because there's nothing to undo
        formula.updateFormula(new_expression, new_inputs);
        if (operations_manager != null)
            operations_manager.add(this);
    }

    @Override
    public void run()
    {
        try
        {
            formula.updateFormula(new_expression, new_inputs);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error, Messages.Formula, ex);
        }
    }

    @Override
    public void undo()
    {
        try
        {
            formula.updateFormula(old_expression, old_inputs);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error, Messages.Formula, ex);
        }
    }
}
