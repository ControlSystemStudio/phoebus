/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;

/** Model of a scan with helpers to insert, delete and represent as tree
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Model
{
    private final List<ScanCommand> model = new ArrayList<>();

    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(final ModelListener listener)
    {
        listeners.add(listener);
    }

    public void setCommands(final List<ScanCommand> commands)
    {
        model.clear();
        model.addAll(commands);
        for (ModelListener listener : listeners)
            listener.commandsChanged();
    }

    public List<ScanCommand> getCommands()
    {
        return model;
    }

    /** @param target Item before or after which new command should be inserted.
     *                If <code>null</code>, inserts at start of list.
     *  @param command New command to insert
     *  @param after <code>true</code> to insert after target, else before
     *  @throws Exception if element cannot be inserted
     */
    public void insert(final ScanCommand target, final ScanCommand command, final boolean after) throws Exception
    {
        insert(model, target, command, after);
    }


    /** @param commands Commands, either 'root' of model or body of a command with body
     *  @param target Item before or after which new command should be inserted.
     *                If <code>null</code>, inserts at start of list.
     *  @param command New command to insert
     *  @param after <code>true</code> to insert after target, else before
     *  @throws Exception if element cannot be inserted
     */
    public void insert(final List<ScanCommand> commands, final ScanCommand target, final ScanCommand command, final boolean after) throws Exception
    {
        if (! doInsert(commands, target, command, after))
            throw new Exception("Cannot locate insertion point for command in list");
    }


    /** Insert command in list, recursing down to find insertion target
     *  @param commands List of scan commands
     *  @param target Item before or after which new command should be inserted.
     *                If <code>null</code>, inserts at start of list.
     *  @param command New command to insert
     *  @param after <code>true</code> to insert after target, else before
     *  @return <code>true</code> if command could be inserted in this list
     */
    private boolean doInsert(final List<ScanCommand> commands,
            final ScanCommand target, final ScanCommand command, final boolean after)
    {
        if (target == null)
        {
            commands.add(0, command);
            for (ModelListener listener : listeners)
                listener.commandAdded(command);
            return true;
        }
        for (int i=0; i<commands.size(); ++i)
        {
            final ScanCommand current = commands.get(i);
            if (current == target)
            {   // Found the insertion point
                commands.add(after ? i+1 : i, command);
                for (ModelListener listener : listeners)
                    listener.commandAdded(command);
                return true;
            }
            else if (current instanceof ScanCommandWithBody)
            {   // Recurse into body, because target may be there.
                final ScanCommandWithBody cmd = (ScanCommandWithBody) current;
                if (doInsert(cmd.getBody(), target, command, after))
                    return true;
                // else: target wasn't in that body
            }
        }
        return false;
    }

    /** @param command Command to remove
     *  @return Info about removal
     *  @throws Exception on error
     */
    public RemovalInfo remove(final ScanCommand command) throws Exception
    {
        final RemovalInfo info = remove(null, model, command);
        if (info == null)
            throw new Exception("Cannot locate item to be removed");
        return info;
    }

    /** @param parent Parent item, <code>null</code> for root of tree
     *  @param commands List of scan commands under parent
     *  @param command Command to remove
     *  @return Info about removal
     */
    private RemovalInfo remove(final ScanCommand parent,
            final List<ScanCommand> commands, final ScanCommand command)
    {
        for (int i=0; i<commands.size(); ++i)
        {
            final ScanCommand current = commands.get(i);
            if (current == command)
            {   // Found the item
                commands.remove(i);
                for (ModelListener listener : listeners)
                    listener.commandRemoved(current);
                return new RemovalInfo(this, parent, i > 0 ? commands.get(i-1) : null, command);
            }
            else if (current instanceof ScanCommandWithBody)
            {   // Recurse into body, because target may be inside.
                final ScanCommandWithBody cmd = (ScanCommandWithBody) current;
                final List<ScanCommand> body = cmd.getBody();
                final RemovalInfo info = remove(cmd, body, command);
                if (info != null)
                    return info;
                // else: target wasn't in that body
            }
        }
        return null;
    }

}
