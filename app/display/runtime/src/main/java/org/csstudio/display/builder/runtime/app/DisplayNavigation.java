/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.phoebus.ui.undo.SizeLimitedStack;

/** Navigate backward/forward between displays
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayNavigation
{
    /** Listener to DisplayNavigation changes */
    @FunctionalInterface
    public static interface Listener
    {
        /** @param navigation Display navigation where forward and/or backward stack changed */
        public void displayHistoryChanged(DisplayNavigation navigation);
    }

    private final SizeLimitedStack<DisplayInfo> backwardStack = new SizeLimitedStack<>(10);
    private DisplayInfo current = null;
    private final SizeLimitedStack<DisplayInfo> forwardStack = new SizeLimitedStack<>(10);

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    /** @param listener Listener to add */
    public void addListener(final Listener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final Listener listener)
    {
        listeners.remove(listener);
    }

    private void notifyListeners()
    {
        for (Listener listener : listeners)
            listener.displayHistoryChanged(this);
    }

    /** Inform display navigation that display was opened
     *  @param display Display that was just opened
     */
    public void setCurrentDisplay(final DisplayInfo display)
    {
        boolean changed = false;
        if (current != null)
        {
            if (current.equals(display))
                return;
            backwardStack.push(current);
            changed = true;
        }
        current = display;
        if (! forwardStack.isEmpty())
        {
            forwardStack.clear();
            changed = true;
        }
        if (changed)
            notifyListeners();
    }

    /** @return List of displays available for navigating backward */
    public List<DisplayInfo> getBackwardDisplays()
    {
        return backwardStack.getItems();
    }

    /** @return List of displays available for navigating forward */
    public List<DisplayInfo> getForwardDisplays()
    {
        return forwardStack.getItems();
    }

    /** @param count Number of displays to go back
     *  @return Display at that point in history
     */
    public DisplayInfo goBackward(int count)
    {
        count = Math.min(backwardStack.size(), count);
        for (int i=0; i<count; ++i)
        {
            if (current != null)
                forwardStack.push(current);
            current = backwardStack.pop();
        }
        notifyListeners();
        return current;
    }

    /** @param count Number of displays to go forward
     *  @return Display at that point in 'future'
     */
    public DisplayInfo goForward(int count)
    {
        count = Math.min(forwardStack.size(), count);
        for (int i=0; i<count; ++i)
        {
            if (current != null)
                backwardStack.push(current);
            current = forwardStack.pop();
        }
        notifyListeners();
        return current;
    }

    /** Clear history etc. */
    public void dispose()
    {
        forwardStack.clear();
        backwardStack.clear();
        current = null;
        listeners.clear();
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        final String backward = backwardStack.getItems().stream().map(d -> d.getName()).collect(Collectors.joining(", "));
        final String curr = current == null ? " -- " : current.getName();
        // Show forwardStack in 'reverse' order so overall result has all items from first to last in order
        final List<DisplayInfo> fwd = new ArrayList<>(forwardStack.getItems());
        Collections.reverse(fwd);
        final String forward = fwd.stream().map(d -> d.getName()).collect(Collectors.joining(", "));
        return "[ " + backward + " ], " + curr + ", [ " + forward + " ]";
    }
}
