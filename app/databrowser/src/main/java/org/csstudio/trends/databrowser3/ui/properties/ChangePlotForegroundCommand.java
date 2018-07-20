/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.paint.Color;

/** Undo-able command to change plot foreground color
 *  @author Kay Kasemir
 */
public class ChangePlotForegroundCommand extends UndoableAction
{
    final private Model model;
    final private Color old_color, new_color;

    /** Register and perform the command
     *  @param model Model to configure
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param new_color New value
     */
    public ChangePlotForegroundCommand(final Model model,
            final UndoableActionManager operations_manager,
            final Color new_color)
    {
        super(Messages.Color);
        this.model = model;
        this.old_color = model.getPlotForeground();
        this.new_color = new_color;
        operations_manager.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        model.setPlotForeground(new_color);
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        model.setPlotForeground(old_color);
    }
}
