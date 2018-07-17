/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change a PV item's request type
 *  @author Kay Kasemir
 */
public class ChangeRequestTypeCommand extends UndoableAction
{
    final private PVItem item;
    final private RequestType old_request_type, new_request_type;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_request_type New value
     */
    public ChangeRequestTypeCommand(final UndoableActionManager operations_manager,
            final PVItem item, final RequestType new_request_type)
    {
        super(Messages.RequestType);
        this.item = item;
        this.old_request_type = item.getRequestType();
        this.new_request_type = new_request_type;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setRequestType(new_request_type);
    }

    @Override
    public void undo()
    {
        item.setRequestType(old_request_type);
    }
}
