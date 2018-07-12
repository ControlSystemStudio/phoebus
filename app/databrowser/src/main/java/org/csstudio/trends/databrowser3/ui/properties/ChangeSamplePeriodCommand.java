/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.text.MessageFormat;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's line width
 *  @author Kay Kasemir
 */
public class ChangeSamplePeriodCommand extends UndoableAction
{
    final private PVItem item;
    final private double old_period, new_period;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_period New value
     *  @throws Exception on error
     */
    public ChangeSamplePeriodCommand(
            final UndoableActionManager operations_manager,
            final PVItem item, final double new_period) throws Exception
    {
        super(Messages.ScanPeriod);
        this.item = item;
        this.old_period = item.getScanPeriod();
        this.new_period = new_period;
        try
        {
            item.setScanPeriod(new_period);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.ScanPeriodChangeErrorFmt, item.getName()),
                ex);
            // Exit before registering for undo because there's nothing to undo
            throw ex;
        }
        operations_manager.add(this);
    }

    @Override
    public void run()
    {
        try
        {
            item.setScanPeriod(new_period);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.ScanPeriodChangeErrorFmt, item.getName()),
                ex);
        }
    }

    @Override
    public void undo()
    {
        try
        {
            item.setScanPeriod(old_period);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.ScanPeriodChangeErrorFmt, item.getName()),
                ex);
        }
    }
}
