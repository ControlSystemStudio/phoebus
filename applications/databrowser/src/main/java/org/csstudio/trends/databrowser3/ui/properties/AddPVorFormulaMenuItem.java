/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.AddModelItemCommand;
import org.csstudio.trends.databrowser3.ui.AddPVDialog;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/** Menu item to add a PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AddPVorFormulaMenuItem extends MenuItem
{
    /** @param node Node relative to which the "Add .." dialog will be positioned
     *  @param model Model where new item will be added
     *  @param undo Undo/Redo
     *  @param formula Add formula or PV?
     */
    public AddPVorFormulaMenuItem(final Node node,
                                  final Model model, final UndoableActionManager undo,
                                  final boolean formula)
    {
        super(formula ? Messages.AddFormula : Messages.AddPV,
              Activator.getIcon(formula ? "add_formula" : "add"));
        setOnAction(event ->
        {
            final AddPVDialog dlg = new AddPVDialog(1, model, formula);
            DialogHelper.positionDialog(dlg, node, -400, -200);
            if (! dlg.showAndWait().orElse(false))
                return;

            final AxisConfig axis = AddPVDialog.getOrCreateAxis(model, undo, dlg.getAxisIndex(0));

            if (formula)
            {
                final AddModelItemCommand command = AddModelItemCommand.forFormula(undo, model, dlg.getName(0), axis);
                final FormulaItem item = (FormulaItem) command.getItem();
                // Open configuration dialog for new formula
                FormulaItemEditor.run(node, item, undo);
            }
            else
                AddModelItemCommand.forPV(undo, model, dlg.getName(0), dlg.getScanPeriod(0), axis, null);
        });
    }
}
