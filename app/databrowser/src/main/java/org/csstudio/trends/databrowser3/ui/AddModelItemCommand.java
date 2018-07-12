/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.text.MessageFormat;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.FormulaInput;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to add a ModelItem to the Model
 *  @author Kay Kasemir
 */
public class AddModelItemCommand extends UndoableAction
{
    final private Model model;
    final private ModelItem item;

    /** Create PV via undo-able AddModelItemCommand,
     *  displaying errors in dialog
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param pv_name Name of new PV
     *  @param period scan period
     *  @param axis Axis
     *  @param archive Archive data source
     *  @return AddModelItemCommand or <code>null</code> on error
     */
    public static AddModelItemCommand forPV(
            final UndoableActionManager operations_manager,
            final Model model,
            final String pv_name,
            final double period,
            final AxisConfig axis,
            final ArchiveDataSource archive)
    {
        // Create item
        final PVItem item;
        try
        {
            item = new PVItem(pv_name, period);
            if (archive != null)
                item.addArchiveDataSource(archive);
            else
                item.useDefaultArchiveDataSources();
            axis.setVisible(true);
            item.setAxis(axis);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                    MessageFormat.format(Messages.AddItemErrorFmt, pv_name), ex);
            return null;
        }
        // Add to model via undo-able command
        return new AddModelItemCommand(operations_manager, model, item);
    }

    /** Create PV via undo-able AddModelItemCommand,
     *  displaying errors in dialog
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param axis Axis
     *  @return AddModelItemCommand or <code>null</code> on error
     */
    public static AddModelItemCommand forFormula(
            final UndoableActionManager operations_manager,
            final Model model,
            final String formula_name,
            final AxisConfig axis)
    {
        // Create item
        final FormulaItem item;
        try
        {
            item = new FormulaItem(formula_name, "0", new FormulaInput[0]); //$NON-NLS-1$
            axis.setVisible(true);
            item.setAxis(axis);
        }
        catch (Exception ex)
        {
        ExceptionDetailsErrorDialog.openError(
                Messages.Error,
                MessageFormat.format(Messages.AddItemErrorFmt, formula_name), ex);
            return null;
        }
        // Add to model via undo-able command
        return new AddModelItemCommand(operations_manager, model, item);
    }

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param item Item to add
     */
    public AddModelItemCommand(final UndoableActionManager operations_manager,
            final Model model, final ModelItem item)
    {
        super(Messages.AddPV);
        this.model = model;
        this.item = item;
        try
        {
            model.addItem(item);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                    MessageFormat.format(Messages.AddItemErrorFmt, item.getName()), ex);
            // Exit before registering for undo because there's nothing to undo
            return;
        }
        operations_manager.add(this);
    }

    /** @return {@link ModelItem} (PV, Formula) that this command added */
    public ModelItem getItem()
    {
        return item;
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        try
        {
            model.addItem(item);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.AddItemErrorFmt, item.getName()), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        model.removeItem(item);
    }
}
