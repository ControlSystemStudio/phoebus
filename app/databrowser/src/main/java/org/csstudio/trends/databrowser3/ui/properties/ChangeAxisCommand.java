/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.Objects;
import java.util.Optional;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change item's axis
 *  @author Kay Kasemir
 */
public class ChangeAxisCommand extends UndoableAction
{
    final private ModelItem item;
    final private AxisConfig old_axis, new_axis;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param axis New value
     */
    public ChangeAxisCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final AxisConfig axis)
    {
        super(Messages.Axis);
        this.item = item;
        this.old_axis = item.getAxis();
        this.new_axis = Objects.requireNonNull(axis);
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        if (!new_axis.isVisible())
            new_axis.setVisible(true);
        item.setAxis(new_axis);
        final Optional<Model> model = item.getModel();
        if (model.isPresent()  &&  !model.get().hasAxisActiveItems(old_axis))
            old_axis.setVisible(false);
    }

    @Override
    public void undo()
    {
        if (!old_axis.isVisible())
            old_axis.setVisible(true);
        item.setAxis(old_axis);
        final Optional<Model> model = item.getModel();
        if (model.isPresent()  &&  !model.get().hasAxisActiveItems(new_axis))
            new_axis.setVisible(false);
    }
}
