/*******************************************************************************
 * Copyright (c) 2010-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.undo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** An undoable action that combines several steps
 *
 *  <p>Allows undo and redo as one operation.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CompoundUndoableAction extends UndoableAction
{
    final protected List<UndoableAction> steps = new ArrayList<>();

    /** @param name Name used to show action in undo/redo UI */
    public CompoundUndoableAction(final String name)
    {
        super(name);
    }

    /** @param step Step to add to the compound action */
    public void add(final UndoableAction step)
    {
        steps.add(step);
    }

    /** @param step Step to execute and then add to the compound action */
    public void execute(final UndoableAction step)
    {
        try
        {
            step.run();
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, "Action failed: " + step, ex);
            return;
        }
        steps.add(step);
    }

    @Override
    public void run()
    {
        for (UndoableAction step : steps)
            step.run();
    }

    @Override
    public void undo()
    {   // Revert each step in reverse order
        for (int i=steps.size()-1; i>=0; --i)
            steps.get(i).undo();
    }
}
