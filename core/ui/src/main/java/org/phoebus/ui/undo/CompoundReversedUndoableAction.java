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

/** A CompoundUndoableAction that executes undo operation in the same order as the 'do' operation
 *
 *  <p>Used for list order manipulations
 *
 *  @author Krisztián Löki
 */
@SuppressWarnings("nls")
public class CompoundReversedUndoableAction extends CompoundUndoableAction
{
    /** @param name Name used to show action in undo/redo UI */
    public CompoundReversedUndoableAction(final String name)
    {
        super(name);
    }
    @Override
    public void undo()
    {
        for (UndoableAction step : steps)
            step.undo();
    }
}
