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
import org.phoebus.util.time.TimeRelativeInterval;

/** Undo-able command to change time axis
 *  @author Kay Kasemir
 */
public class ChangeTimerangeCommand extends UndoableAction
{
    final private Model model;
    final private TimeRelativeInterval old_range, new_range;

    /** Register and perform the command
     *  @param model Model
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param new_range New time range
     */
    public ChangeTimerangeCommand(final Model model, final UndoableActionManager operationsManager,
                                  final TimeRelativeInterval new_range)
    {
        super(Messages.TimeAxis);
        this.model = model;
        this.old_range = model.getTimerange();
        this.new_range = new_range;
        operationsManager.add(this);
        run();
    }

    @Override
    public void run()
    {
        model.setTimerange(new_range);
    }

    @Override
    public void undo()
    {
        model.setTimerange(old_range);
    }
}
