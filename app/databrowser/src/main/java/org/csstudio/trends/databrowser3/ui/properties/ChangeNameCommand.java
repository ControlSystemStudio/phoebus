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
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's name
 *  @author Kay Kasemir
 */
public class ChangeNameCommand extends UndoableAction
{
    final private ModelItem item;
    final private String old_name, new_name;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_name New value
     *  @throws Exception on error
     */
    public ChangeNameCommand(final UndoableActionManager operations_manager,
                             final ModelItem item, final String new_name) throws Exception
    {
        super(Messages.ItemName);
        this.item = item;
        this.old_name = item.getName();
        this.new_name = new_name;
        try
        {
            item.setName(new_name);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                MessageFormat.format(Messages.ChangeNameErrorFmt, old_name, new_name),
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
            item.setName(new_name);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                    MessageFormat.format(Messages.ChangeNameErrorFmt, old_name, new_name),
                    ex);
        }
    }

    @Override
    public void undo()
    {
        try
        {
            item.setName(old_name);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error,
                   MessageFormat.format(Messages.ChangeNameErrorFmt, new_name, old_name),
                   ex);
        }
    }
}
