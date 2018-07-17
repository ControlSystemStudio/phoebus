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

/** Undo-able command to change item's Live Sample Buffer Size
 *  @author Kay Kasemir
 */
public class ChangeLiveCapacityCommand extends UndoableAction
{
    final private PVItem item;
    final private int old_size, new_size;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_size New value
     *  @throws Exception on error
     */
    public ChangeLiveCapacityCommand(
            final UndoableActionManager operations_manager,
            final PVItem item, final int new_size) throws Exception
    {
        super(Messages.LiveSampleBufferSize);
        this.item = item;
        this.old_size = item.getLiveCapacity();
        this.new_size = new_size;

        // Exit before registering for undo because there's nothing to undo
        final Exception ex = apply(new_size);
        if (ex != null)
            throw ex;
        operations_manager.add(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        apply(new_size);
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        apply(old_size);
    }

    /** Change item's data buffer
     *  @param size Desired size
     *  @return {@link Exception} on error
     */
    private Exception apply(final int size)
    {
        try
        {
            item.setLiveCapacity(size);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.ChangeLiveCapacityCommandErrorFmt,
                                     item.getName(), size),
                ex);
            return ex;
        }
        return null;
    }
}
