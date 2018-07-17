/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
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

/** Undo-able command to change time axis configuration
 *  @author Kay Kasemir
 */
public class ChangeTimeAxisConfigCommand extends UndoableAction
{
    final private Model model;
    final private boolean show_grid;

    /** Register the command and perform
     *  @param model
     *  @param operations_manager
     *  @param show_grid
     */
    public ChangeTimeAxisConfigCommand(final Model model,
            final UndoableActionManager operations_manager,
            final boolean show_grid)
    {
        super(Messages.TimeAxis);
        this.model = model;
        this.show_grid = show_grid;
        operations_manager.add(this);
        run();
    }

    @Override
    public void run()
    {
        model.setGridVisible(show_grid);
    }

    @Override
    public void undo()
    {
        model.setGridVisible(! show_grid);
    }
}
