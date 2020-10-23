/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.undo;

import static org.phoebus.ui.undo.UndoableAction.logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/** Manager for {@link UndoableAction}s
 *
 *  @author Xihui Chen original org.csstudio.swt.xygraph.undo.OperationsManager
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UndoableActionManager
{
    private final SizeLimitedStack<UndoableAction> undoStack;
    private final SizeLimitedStack<UndoableAction> redoStack;
    private final List<UndoRedoListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Keeps track of changes when actions are put on the undo stack and redo stack.
     * This can be used to determine if a resource has changes even if the
     * undo stack is empty.
     */
    private int changeCount;

    /** @param stack_size Number of undo/redo entries */
    public UndoableActionManager(final int stack_size)
    {
        undoStack = new SizeLimitedStack<UndoableAction>(stack_size);
        redoStack = new SizeLimitedStack<UndoableAction>(stack_size);
    }

    /** @param listener Listener to add */
    public void addListener(final UndoRedoListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public boolean removeListener(final UndoRedoListener listener)
    {
        return listeners.remove(listener);
    }

    /** @return <code>true</code> if there is an action to un-do */
    public boolean canUndo()
    {
        return ! undoStack.isEmpty();
    }

    /** @return <code>true</code> if there is an action to re-do */
    public boolean canRedo()
    {
        return ! redoStack.isEmpty();
    }

    /** @param action Action to perform and add to the un-do stack */
    public void execute(final UndoableAction action)
    {
        try
        {
            action.run();
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, "Action failed: " + action, ex);
            return;
        }
        add(action);
    }

    /** @param action Action that has already been performed, which can be un-done */
    public void add(final UndoableAction action)
    {
        changeCount++;
        undoStack.push(action);
        redoStack.clear();
        fireOperationsHistoryChanged();
    }

    /** Undo the last command
     *  @returns Action that was un-done
     */
    public UndoableAction undoLast()
    {
        if (undoStack.isEmpty())
            return null;
        final UndoableAction action = undoStack.pop();
        try
        {
            changeCount--;
            logger.log(Level.FINE, "Undo {0}", action);
            action.undo();
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, "Undo failed: " + action, ex);
            return null;
        }
        redoStack.push(action);
        fireOperationsHistoryChanged();
        return action;
    }

    /** Re-do the last command
     *  @returns Action that was re-done
     */
    public UndoableAction redoLast()
    {
        if (redoStack.isEmpty())
            return null;
        changeCount++;
        final UndoableAction action = redoStack.pop();
        logger.log(Level.FINE, "Redo {0}", action);
        action.run();
        undoStack.push(action);
        fireOperationsHistoryChanged();
        return action;
    }

    /** Clear all undo/redo operations */
    public void clear()
    {
        undoStack.clear();
        redoStack.clear();
        changeCount = 0;
        fireOperationsHistoryChanged();
    }

    private void fireOperationsHistoryChanged()
    {
        final String to_undo = undoStack.isEmpty() ? null : undoStack.peek().toString();
        final String to_redo = redoStack.isEmpty() ? null : redoStack.peek().toString();
        for (final UndoRedoListener listener : listeners)
            listener.operationsHistoryChanged(to_undo, to_redo, changeCount);
    }

    /**
     * Accessor for the undo stack.
     * @return A {@link List} of undo actions. May be empty.
     */
    public List<UndoableAction> getUndoStack(){
        return undoStack.getItems();
    }
}
