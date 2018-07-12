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
import org.csstudio.scan.ui.editor.Model;
import org.csstudio.scan.ui.editor.RemovalInfo;
import org.phoebus.ui.undo.UndoableAction;

/** Remove one or more commands from scan
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RemoveCommands extends UndoableAction
{
    private final Model model;
    private final List<ScanCommand> to_remove;
    private List<RemovalInfo> removals = null;

    public RemoveCommands(final Model model,
                          final List<ScanCommand> to_remove)
    {
        super("Remove");
        this.model = model;
        this.to_remove = new ArrayList<>(to_remove);
    }

    @Override
    public void run()
    {
        // Remove commands from scan, going in reverse:
        // The list may contain a loop and items in that loop.
        // When first removing the loop, the items in the loop
        // can no longer be removed...
        // Going in reverse avoids that problem.
        //
        // Similarly, removed items are remembered in reverse
        // so that the undo can simply undo each removed item.
        removals = new ArrayList<RemovalInfo>();
        try
        {
            final int N = to_remove.size();
            for (int i=N-1;  i>=0;  --i)
            {
                final ScanCommand command = to_remove.get(i);
                removals.add(0, model.remove(command, N-i, N));
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot remove commands", ex);
        }
    }

    @Override
    public void undo()
    {
        try
        {
            if (removals != null)
            {
                final int N = to_remove.size();
                int i = 0;
                for (RemovalInfo removal : removals)
                    removal.undo(++i, N);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot undo removal", ex);
        }
    }
}
