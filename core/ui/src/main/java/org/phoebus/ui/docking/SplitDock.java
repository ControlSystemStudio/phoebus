/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import static org.phoebus.ui.docking.DockPane.logger;

import java.util.ArrayList;
import java.util.logging.Level;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

/** Pane that holds two sub-nodes, each either a {@link DockPane} or a {@link SplitDock}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SplitDock extends SplitPane
{
    /** In principle, 'getParent()' provides the parent of a node,
     *  which should either be a {@link BorderPane} or a {@link SplitDock}.
     *  JFX, however, will only update the parent when the node is rendered.
     *  While assembling or updating the scene, getParent() can return null.
     *  Further, a node added to a SplitDock (SplitPane) will actually have
     *  a SplitPaneSkin$Content as a parent and not a SplitPane let alone SplitDock.
     *
     *  We therefore track the parent that matters for our needs
     *  in the user data under this key.
     */
    private Parent dock_parent;

    /** Create a split section
     *  @param dock_parent {@link BorderPane} of {@link SplitDock}
     *  @param horizontally Horizontal?
     *  @param first Top or left item
     *  @param second Bottom or right item
     */
    public SplitDock(final Parent dock_parent, final boolean horizontally, final Control first, final Control second)
    {
        this.dock_parent = dock_parent;
        if (! horizontally)
            setOrientation(Orientation.VERTICAL);

        if (! ((first instanceof SplitDock) || (first instanceof DockPane)))
            throw new IllegalArgumentException("Expect DockPane or another nested SplitDock, got " + first.getClass().getName());
        if (! ((second instanceof SplitDock) || (second instanceof DockPane)))
            throw new IllegalArgumentException("Expect DockPane or another nested SplitDock, got " + first.getClass().getName());

        getItems().setAll(first, second);
    }

    /** @return Horizontal? */
    public boolean isHorizontal()
    {
        return getOrientation() == Orientation.HORIZONTAL;
    }

    /** @return Position of top/botton respectively left/right divider, 0..1 */
    public double getDividerPosition()
    {
        final ObservableList<Divider> dividers = getDividers();
        if (dividers.size() < 1)
            return 0.5;
        return dividers.get(0).getPosition();
    }

    /** @param position Position of top/botton respectively left/right divider, 0..1 */
    public void setDividerPosition(final double position)
    {
        setDividerPosition(0, position);
    }

    /** @param item DockPane of SplitDock to remove
     *  @return <code>true</code> if that was the first (left, top) item.
     *          Otherwise it was the second (right, bottom).
     */
    boolean removeItem(final Control item)
    {
        final boolean first = getItems().indexOf(item) == 0;
        getItems().remove(item);
        return first;
    }

    /** @param first Add as first (left, top) item?
     *  @param item Item to add
     */
    void addItem(final boolean first, final Node item)
    {
        if (! ((item instanceof SplitDock) || (item instanceof DockPane)))
            throw new IllegalArgumentException("Expect DockPane or another nested SplitDock, got " + item.getClass().getName());
        if (first)
            getItems().add(0, item);
        else
            getItems().add(item);
    }

    /** @return Can this split be merged ? */
    public boolean canMerge()
    {
        for (Node child : new ArrayList<>(getItems()))
            if (child instanceof SplitDock)
                if (! ((SplitDock) child).canMerge())
                    return false;
        return findEmptyDock() != null;
    }

    /** If this split holds only one useful item, the other one
     *  being an empty DockPane,
     *  replace ourself in the parent with that one non-empty item.
     *
     *  <p>Merges 'deep', i.e. first checks if any of the child
     *  items can itself be merged, then merges this one.
     */
    public void merge()
    {
        logger.log(Level.INFO, "SplitDock merging empty sections " + this);
        // First recurse to merge child splits.
        // Use copy to avoid comodification
        for (Node child : new ArrayList<>(getItems()))
            if (child instanceof SplitDock)
                ((SplitDock) child).merge();

        final DockPane empty_dock = findEmptyDock();
        if (empty_dock == null)
        {
            logger.log(Level.INFO, "No mergable, empty DockPane in " + this);
            return;
        }

        // Remove the empty dock from this split
        getItems().remove(empty_dock);

        // Usually left with just one child (dock or nested split)
        if (getItems().isEmpty())
        {
            logger.log(Level.FINE, "Cannot merge completely empty SplitPane " + this);
            return;
        }

        // Remove this split, move remaining child up to parent
        final Node child = getItems().get(0);
        if (dock_parent instanceof BorderPane)
        {
            final BorderPane parent = (BorderPane) dock_parent;
            // parent.getCenter() == this.
            // No need to remove 'this' from parent, just update center to child
            parent.setCenter(child);
        }
        else if (dock_parent instanceof SplitDock)
        {
            final SplitDock parent = (SplitDock) dock_parent;
            final boolean was_first = parent.removeItem(this);
            parent.addItem(was_first, child);
        }
        else
        {
            logger.log(Level.WARNING, "Cannot merge " + this + ", parent is " + dock_parent);
            return;
        }

        // Tell child about its new dock_parent
        if (child instanceof DockPane)
            ((DockPane)child).setDockParent(dock_parent);
        else if (child instanceof SplitDock)
            ((SplitDock)child).dock_parent = dock_parent;
    }

    /** Find an empty DockPane that should trigger a 'merge'
     *
     *  <p>Will not return anything when one of the panes is 'fixed',
     *  so the other one needs to remain even when empty.
     *
     *  @return First DockPane child that's empty, or <code>null</code>
     */
    private DockPane findEmptyDock()
    {
        // If one of them is 'fixed', don't bother checking the other:
        // Need to keep this SplitDock
        for (Node item : getItems())
            if (isFixed(item))
                return null;

        // Find the first empty DockPane
        for (Node item : getItems())
            if (isEmptyDock(item))
                  return (DockPane) item;
        return null;
    }

    /** @param item Potential {@link DockPane}
     *  @return Is 'item' a 'fixed' {@link DockPane}?
     */
    private boolean isFixed(final Node item)
    {
        return isFixedDock(item)  ||  isFixedSplit(item);
    }

    /** @param item Potential {@link DockPane}
     *  @return Is 'item' a 'fixed' {@link DockPane}?
     */
    private boolean isFixedDock(final Node item)
    {
        return item instanceof DockPane  &&
               ((DockPane)item).isFixed();
    }

    /** @param item Potential {@link SplitDock}
     *  @return Are both sides of the split fixed?
     */
    private boolean isFixedSplit(final Node item)
    {
        if (! (item instanceof SplitDock))
            return false;
        final SplitDock split = (SplitDock) item;
        for (Node sub : split.getItems())
            if (! isFixed(sub))
                return false;
        return true;
    }


    /** @param item Potential {@link DockPane}
     *  @return Is 'item' an empty {@link DockPane}?
     */
    private boolean isEmptyDock(final Node item)
    {
        if (item instanceof DockPane)
        {
            final DockPane dock = (DockPane) item;
            // Calling getTabs() instead of getDockItems()
            // to safe a little time, not creating casted list
            if (dock.getTabs().isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "SplitDock for " + getItems();
    }
}
