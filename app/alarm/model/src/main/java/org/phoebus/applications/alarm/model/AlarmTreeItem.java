/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.phoebus.util.text.CompareNatural;

/** Base class for all nodes in the alarm tree
 *  @param STATE Type used for the alarm state
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class AlarmTreeItem<STATE extends BasicState>
{
    /** Visible name of the item */
    private final String name;

    /** Parent node */
    protected volatile AlarmTreeItem<BasicState> parent;

    /** Sub-tree elements of this item
     *
     *  <p>For nodes, this is the thread-safe {@link CopyOnWriteArrayList}
     *  and <u>all entries are kept in sorted order</u> based on the item's name.
     *
     *  <p>For leaf, it's an empty list.
     */
    protected final List<AlarmTreeItem<?>> children;

    /** Full path name of this item.
     *  Like parent it's final so that it can be computed once,
     *  because it is used very often.
     */
    private final String path_name;

    private List<TitleDetail> guidance = Collections.emptyList();

    private List<TitleDetail> displays = Collections.emptyList();

    private List<TitleDetail> commands = Collections.emptyList();

    private List<TitleDetailDelay> actions = Collections.emptyList();

    /** Constructor for item or leaf
     *  @param parent Parent item, <code>null</code> for root
     *  @param name Name of this item
     *  @param children {@link CopyOnWriteArrayList} for item, empty list for leaf
     */
    protected AlarmTreeItem(final AlarmTreeItem<BasicState> parent, final String name, final List<AlarmTreeItem<?>> children)
    {
        this.parent = parent;
        this.name = name;
        this.children = children;
        if (parent == null)
            path_name = AlarmTreePath.makePath(null, name);
        else
        {
            path_name = AlarmTreePath.makePath(parent.getPathName(), name);

            // Keep sorted by inserting at appropriate index
            // Note that getChild(name) depends on this order!
            final int index = Collections.binarySearch(parent.children, this, (a, b) -> CompareNatural.compareTo(a.getName(), b.getName()));
            if (index < 0)
                parent.children.add(-index-1, this);
            else
                parent.children.add(index, this);
        }
    }

    /** @return Name */
    public final String getName()
    {
        return name;
    }

    /** @return Full path name to this item, including the item name itself */
    public final String getPathName()
    {
        return path_name;
    }

    /** @return Parent item. <code>null</code> for root */
    public AlarmTreeItem<BasicState> getParent()
    {
        return parent;
    }

    /** Detach item from parent: Remove from parent's list of children
     *  @throws Error if parent didn't know about this child item
     */
    public void detachFromParent()
    {
        final AlarmTreeItem<BasicState> p = parent;
        parent = null;
        logger.log(Level.FINE, "Detach " + getPathName() + " from parent");
        if (p != null)
            if (! p.children.remove(this))
                throw new Error("Corrupt alarm tree, " + p.getPathName() + " is not aware of " + getPathName());
    }

    public List<AlarmTreeItem<?>> getChildren()
    {
        return children;
    }

    /** Locate child element by name.
     *  @param child_name Name of child to locate.
     *  @return Child with given name or <code>null</code> if not found.
     */
    public AlarmTreeItem<?> getChild(final String name)
    {
        // Binary search for name
        // Depends on nodes being in 'natural' order
        int low = 0, high = children.size()-1;
        while (low <= high)
        {
            final int mid = (low + high) >>> 1;
            final AlarmTreeItem<?> val = children.get(mid);
            final int cmp = CompareNatural.compareTo(val.getName(), name);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return val; // key found
        }
        return null;
    }

    abstract public STATE getState();

    /** @param guidance Guidance entries
     *  @return <code>true</code> if guidance was changed, <code>false</code> if no change
     */
    public boolean setGuidance(final List<TitleDetail> guidance)
    {
        if (this.guidance.equals(guidance))
            return false;
        this.guidance = guidance;
        return true;
    }

    public List<TitleDetail> getGuidance()
    {
        return guidance;
    }

    public boolean setDisplays(final List<TitleDetail> displays)
    {
        if (this.displays.equals(displays))
            return false;
        this.displays = displays;
        return true;
    }

    public List<TitleDetail> getDisplays()
    {
        return displays;
    }

    public boolean setCommands(final List<TitleDetail> commands)
    {
        if (this.commands.equals(commands))
            return false;
        this.commands = commands;
        return true;
    }

    public List<TitleDetail> getCommands()
    {
        return commands;
    }

    public boolean setActions(final List<TitleDetailDelay> actions)
    {
        if (this.actions.equals(actions))
            return false;
        this.actions = actions;
        return true;
    }

    public List<TitleDetailDelay> getActions()
    {
        return actions;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
