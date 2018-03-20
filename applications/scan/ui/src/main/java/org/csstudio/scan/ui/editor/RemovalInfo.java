/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;

/** Info about a removed item,
 *  allowing re-insertion at the original place
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RemovalInfo
{
    private final Model model;
    private final ScanCommandWithBody parent;
    private final ScanCommand previous;
    private final ScanCommand command;
    private final int update_index, update_total;

    /** @param model Scan tree model
     *  @param parent Parent item, for example Loop or <code>null</code> for top-level item
     *  @param previous Previous item within the parent or top-level list, <code>null</code> if first
     *  @param command Command that was removed
     *  @param update_index Hint: This is update i ..
     *  @param update_total .. out of a series of N total updates
     */
    public RemovalInfo(final Model model,
                       final ScanCommandWithBody parent,
                       final ScanCommand previous,
                       final ScanCommand command,
                       final int update_index,
                       final int update_total)
    {
        this.model = model;
        this.parent = parent;
        this.previous = previous;
        this.command = command;
        this.update_index = update_index;
        this.update_total = update_total;
    }

    /** Undo the removal
     *  @throws Exception on error
     */
    public void undo() throws Exception
    {
        if (! reinsert(null, model.getCommands()))
            throw new Exception("Cannot re-insert cut command");
    }

    /** Recursively attempt to insert removed item
     *  @param commands_parent Parent of commands
     *  @param commands List of commands
     *  @return <code>true</code> if successful
     *  @throws Exception on error
     */
    private boolean reinsert(final ScanCommandWithBody commands_parent, final List<ScanCommand> commands) throws Exception
    {
        // Was command removed at this level in the tree?
        if (commands_parent == parent)
        {
            model.insert(parent, previous, command, true, update_index, update_total);
            return true;
        }

        // Descend down the tree
        for (ScanCommand item : commands)
            if (item instanceof ScanCommandWithBody)
            {   // Can command be re-inserted at or below this command?
                final ScanCommandWithBody cmd = (ScanCommandWithBody) item;
                if (reinsert(cmd, cmd.getBody()))
                    return true;
                // else: keep looking
            }
        return false;
    }
}
