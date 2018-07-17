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

/** Undo-able command to add value axis to Model
 *  @author Kay Kasemir
 */
public class AddAxisCommand extends UndoableAction
{
    /** Model to which axis is added */
    final private Model model;

    /** The axis that was added */
    final private AxisConfig axis;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param pv PV where to add archive
     *  @param archive Archive data source to add
     */
    public AddAxisCommand(final UndoableActionManager operations_manager,
            final Model model)
    {
        super(Messages.AddAxis);
        this.model = model;
        operations_manager.add(this);
        axis = model.addAxis();
    }

    /** @return AxisConfig that was added */
    public AxisConfig getAxis()
    {
        return axis;
    }

    @Override
    public void run()
    {
        model.addAxis(axis);
    }

    @Override
    public void undo()
    {
        model.removeAxis(axis);
    }
}
