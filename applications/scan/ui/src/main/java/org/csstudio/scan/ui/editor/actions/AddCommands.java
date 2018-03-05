/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.actions;

import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.ui.editor.Model;
import org.phoebus.ui.undo.UndoableAction;

/** Add one or more commands to scan
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AddCommands extends UndoableAction
{
    private final Model model;
    private final ScanCommand location;
    private final List<ScanCommand> commands;
    private final boolean after;

    public AddCommands(final Model model, final ScanCommand location, final List<ScanCommand> commands,
                       final boolean after)
    {
        super("Add commands");
        this.model = model;
        this.location = location;
        this.commands = commands;
        this.after = after;
    }

    @Override
    public void run()
    {
        try
        {
            ScanCommand target = location;
            if (location == null  && commands.size() > 0)
                target = commands.get(commands.size()-1);
            boolean insert_after = after;
            for (ScanCommand command : commands)
            {
                model.insert(target, command, insert_after);
                // When many items are inserted, the first item may go "before"
                // the target.
                // That newly inserted command then becomes the target,
                // and everything else goes _after_ this new target.
                target = command;
                insert_after = true;
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot add commands", ex);
        }
    }

    @Override
    public void undo()
    {
        try
        {
            for (ScanCommand command : commands)
                model.remove(command);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot add commands", ex);
        }
    }
}
