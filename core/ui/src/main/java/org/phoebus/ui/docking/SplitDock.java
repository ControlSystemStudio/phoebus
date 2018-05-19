/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.SplitPane;

/** Pane that holds two sub-nodes, each either a {@link DockPane} or a {@link SplitDock}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SplitDock extends SplitPane
{
    // TODO Track dock_parent (SplitDock or BorderPane)?
    public SplitDock(final boolean horizontally, final Control... items)
    {
        if (! horizontally)
            setOrientation(Orientation.VERTICAL);
        if (items.length > 2)
            throw new IllegalArgumentException("Allow at most two items, got " + items.length);
        for (Control item : items)
            if (! ((item instanceof SplitDock) || (item instanceof DockPane)))
                throw new IllegalArgumentException("Expect DockPane or another nested SplitDock, got " + item.getClass().getName());

        getItems().setAll(items);
    }

    /** @param item Item to remove
     *  @return <code>true</code> if that was the first (left, top) item.
     *          Otherwise it was the second (right, bottom).
     */
    boolean removeItem(final DockPane item)
    {
        final boolean first = getItems().indexOf(item) == 0;
        getItems().remove(item);
        return first;
    }

    /** @param first Add as first (left, top) item?
     *  @param item Item to add
     */
    void addItem(final boolean first, final Control item)
    {
        if (! ((item instanceof SplitDock) || (item instanceof DockPane)))
            throw new IllegalArgumentException("Expect DockPane or another nested SplitDock, got " + item.getClass().getName());
        if (first)
            getItems().add(0, item);
        else
            getItems().add(item);
    }

    @Override
    public String toString()
    {
        return "SplitDock for " + getItems();
    }
}
