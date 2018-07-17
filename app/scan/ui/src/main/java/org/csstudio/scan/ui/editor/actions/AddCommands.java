/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;
import org.csstudio.scan.ui.editor.Model;
import org.phoebus.ui.undo.UndoableAction;

/** Add one or more commands to scan
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AddCommands extends UndoableAction
{
    private final Model model;
    private final ScanCommandWithBody parent;
    private final ScanCommand location;
    private final List<ScanCommand> new_commands;
    private final boolean after;

    public AddCommands(final Model model, final ScanCommand location, final List<ScanCommand> new_commands,
                       final boolean after)
    {
        this(model, null, location, new_commands, after);
    }


    /** @param model Model
     *  @param commands Commands at root of model, or within body of target
     *  @param location Location where to insert
     *  @param new_commands New commands to insert
     *  @param after After the location, or before the location?
     */
    public AddCommands(final Model model, final ScanCommandWithBody parent,
                       final ScanCommand location, final List<ScanCommand> new_commands,
                       final boolean after)
    {
        super("Add commands");
        this.model = model;
        this.parent = parent;
        this.location = location;
        this.new_commands = new ArrayList<>(new_commands);
        this.after = after;
    }

    @Override
    public void run()
    {
        try
        {
            final List<ScanCommand> commands = parent == null ? model.getCommands() : parent.getBody();
            ScanCommand target = location;
            if (location == null  && commands.size() > 0)
                target = commands.get(commands.size()-1);
            boolean insert_after = after;
            final int N = new_commands.size();
            int i = 0;
            for (ScanCommand command : new_commands)
            {
                model.insert(parent, target, command, insert_after, ++i, N);
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
            // Perform removal in reverse of addition,
            // so item added last will be removed first
            final int N = new_commands.size();
            for (int i=N-1; i>=0; --i)
            {
                final ScanCommand command= new_commands.get(i);
                model.remove(command, N-i, N);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot add commands", ex);
        }
    }
}
