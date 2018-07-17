/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to delete items
 *  @author Kay Kasemir
 */
public class DeleteItemsCommand extends UndoableAction
{
    final private Model model;
    final private List<ModelItem> items;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param items Model items to delete
     */
    public DeleteItemsCommand(final UndoableActionManager operations_manager,
            final Model model, final List<ModelItem> items)
    {
        super(Messages.DeleteItem);
        this.model = model;
        // This list could be a reference to the
        // model's list.
        // Since we will loop over this list,
        // assert that there are no comodification problems
        // by creating a copy.
        this.items = new ArrayList<>(items);
        operations_manager.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        for (ModelItem item : items)
            model.removeItem(item);
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        for (ModelItem item : items)
        {
            try
            {
                model.addItem(item);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(
                        Messages.Error,
                        MessageFormat.format(Messages.AddItemErrorFmt, item.getName()),
                        ex);
            }
        }
    }
}
