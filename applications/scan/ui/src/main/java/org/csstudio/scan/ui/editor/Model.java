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

import org.csstudio.scan.command.CommandSequence;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;

import javafx.application.Platform;

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
        updateAddresses();
        for (ModelListener listener : listeners)
            Platform.runLater(() -> listener.commandsChanged());
    }

    /** Update the addresses in the command sequence */
    private void updateAddresses()
    {
        CommandSequence.setAddresses(model);
    }

    public List<ScanCommand> getCommands()
    {
        return model;
    }

    /** @param parent <code>null</code> for root list, otherwise e.g. loop that's the parent of new command
     *  @param target Item before or after which new command should be inserted.
     *                If <code>null</code>, inserts at start of list.
     *  @param command New command to insert
     *  @param after <code>true</code> to insert after target, else before
     *  @throws Exception if element cannot be inserted
     */
    public void insert(final ScanCommandWithBody parent, final ScanCommand target, final ScanCommand command, final boolean after) throws Exception
    {
        if (! doInsert(parent, target, command, after))
            throw new Exception("Cannot locate insertion point for command in list");
        updateAddresses();
    }

    /** Insert command in list, recursing down to find insertion target
     *  @param parent <code>null</code> for root list, otherwise e.g. loop that's the parent of new command
     *  @param target Item before or after which new command should be inserted.
     *                If <code>null</code>, inserts at start of list.
     *  @param command New command to insert
     *  @param after <code>true</code> to insert after target, else before
     *  @return <code>true</code> if command could be inserted in this list
     */
    private boolean doInsert(final ScanCommandWithBody parent,
            final ScanCommand target, final ScanCommand command, final boolean after)
    {
        final List<ScanCommand> commands = parent == null ? getCommands() : parent.getBody();
        if (target == null)
        {
            commands.add(0, command);
            for (ModelListener listener : listeners)
                listener.commandAdded(parent, command);
            return true;
        }
        for (int i=0; i<commands.size(); ++i)
        {
            final ScanCommand current = commands.get(i);
            if (current == target)
            {   // Found the insertion point
                commands.add(after ? i+1 : i, command);
                for (ModelListener listener : listeners)
                    listener.commandAdded(parent, command);
                return true;
            }
            else if (current instanceof ScanCommandWithBody)
            {   // Recurse into body, because target may be there.
                final ScanCommandWithBody cmd = (ScanCommandWithBody) current;
                if (doInsert(cmd, target, command, after))
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
        final RemovalInfo info = remove(null, command);
        if (info == null)
            throw new Exception("Cannot locate item to be removed");
        updateAddresses();
        return info;
    }

    /** @param parent Parent item, <code>null</code> for root of tree
     *  @param command Command to remove
     *  @return Info about removal
     */
    private RemovalInfo remove(final ScanCommandWithBody parent,
                               final ScanCommand command)
    {
        final List<ScanCommand> commands = parent == null ? model : parent.getBody();
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
                final RemovalInfo info = remove(cmd, command);
                if (info != null)
                    return info;
                // else: target wasn't in that body
            }
        }
        return null;
    }
}
