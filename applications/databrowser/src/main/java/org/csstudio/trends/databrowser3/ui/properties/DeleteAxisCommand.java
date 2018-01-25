/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to delete value axis from Model.
 *  @author Kay Kasemir
 */
public class DeleteAxisCommand extends UndoableAction
{
    /** Model to which axis is added */
    final private Model model;

    /** The axis that was removed */
    final private AxisConfig axis;

    /** Index where axis used to be */
    final private int index;

    /** Initialize
     *  @param operationsManager
     *  @param model
     *  @param axis
     */
    public DeleteAxisCommand(final UndoableActionManager operationsManager,
            final Model model, final AxisConfig axis)
    {
        super(Messages.DeleteAxis);
        this.model = model;
        this.axis = axis;
        // Remember axis locations
        this.index = model.getAxisIndex(axis);
        operationsManager.execute(this);
    }

    @Override
    public void run()
    {
        model.removeAxis(axis);
    }

    @Override
    public void undo()
    {
        model.addAxis(index, axis);
    }
}
