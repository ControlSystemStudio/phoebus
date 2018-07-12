/*******************************************************************************
 * Copyright (c) 2010=2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change a PV item's request type
 *  @author Kay Kasemir
 */
public class ChangeVisibilityCommand extends UndoableAction
{
    final private ModelItem item;
    final private boolean old_visibility, new_visibility;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_trace_type New value
     */
    public ChangeVisibilityCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final boolean visible)
    {
        super(Messages.TraceVisibility);
        this.item = item;
        this.old_visibility = item.isVisible();
        this.new_visibility = visible;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        final AxisConfig axis = item.getAxis();
        item.setVisible(new_visibility);
        axis.setVisible(item.getModel().get().hasAxisActiveItems(axis));
    }

    @Override
    public void undo()
    {
        final AxisConfig axis = item.getAxis();
        item.setVisible(old_visibility);
        axis.setVisible(item.getModel().get().hasAxisActiveItems(axis));
    }
}
