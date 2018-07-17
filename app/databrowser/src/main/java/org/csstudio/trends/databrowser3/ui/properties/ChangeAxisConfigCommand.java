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
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Undo-able command to change axis configuration
 *  @author Kay Kasemir
 */
public class ChangeAxisConfigCommand extends UndoableAction
{
    final private AxisConfig axis;
    final private AxisConfig old_config;
    private AxisConfig new_config;

    /** Register the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param axis Axis configuration to undo/redo
     */
    public ChangeAxisConfigCommand(final UndoableActionManager operations_manager,
            final AxisConfig axis)
    {
        super(Messages.Axis);
        this.axis = axis;
        this.old_config = axis.copy();
        operations_manager.add(this);
    }

    /** Must be called after the original configuration was changed */
    public void rememberNewConfig()
    {
        this.new_config = axis.copy();
    }

    @Override
    public void run()
    {
        apply(new_config);
    }

    @Override
    public void undo()
    {
        apply(old_config);
    }

    /** Apply whatever's different in the given configuration to the axis
     *  @param config
     */
    private void apply(final AxisConfig config)
    {
        if (axis.isVisible() != config.isVisible())
            axis.setVisible(config.isVisible());
        if (! axis.getName().equals(config.getName()))
            axis.setName(config.getName());
        if (axis.isUsingAxisName() != config.isUsingAxisName())
            axis.useAxisName(config.isUsingAxisName());
        if (axis.isUsingTraceNames() != config.isUsingTraceNames())
            axis.useTraceNames(config.isUsingTraceNames());
        if (axis.isOnRight() != config.isOnRight())
            axis.setOnRight(config.isOnRight());
        if (! axis.getPaintColor().equals(config.getPaintColor()))
            axis.setColor(config.getPaintColor());
        if (axis.isAutoScale() != config.isAutoScale())
            axis.setAutoScale(config.isAutoScale());
        if (axis.getMin() != config.getMin()  ||
                axis.getMax() != config.getMax())
            axis.setRange(config.getMin(), config.getMax());
        if (axis.isGridVisible() != config.isGridVisible())
            axis.setGridVisible(config.isGridVisible());
        if (axis.isLogScale() != config.isLogScale())
            axis.setLogScale(config.isLogScale());
    }
}
