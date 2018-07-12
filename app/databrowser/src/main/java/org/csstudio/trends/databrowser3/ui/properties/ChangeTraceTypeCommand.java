/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's trace type
 *  @author Kay Kasemir
 */
public class ChangeTraceTypeCommand extends UndoableAction
{
    final private ModelItem item;
    final private TraceType old_trace_type, new_trace_type;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_trace_type New value
     */
    public ChangeTraceTypeCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final TraceType new_trace_type)
    {
        super(Messages.TraceType);
        this.item = item;
        this.old_trace_type = item.getTraceType();
        this.new_trace_type = new_trace_type;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        item.setTraceType(new_trace_type);
    }

    @Override
    public void undo()
    {
        item.setTraceType(old_trace_type);
    }
}
