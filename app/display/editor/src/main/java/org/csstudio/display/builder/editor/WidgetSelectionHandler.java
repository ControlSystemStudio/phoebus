/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.display.builder.model.Widget;

/** Handler for currently selected widgets in a model.
 *
 *  <p>Has list of currently selected widgets,
 *  allows adding/removing widgets from selection,
 *  updates listeners.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetSelectionHandler
{
    private final List<WidgetSelectionListener> listeners = new CopyOnWriteArrayList<>();
    private volatile List<Widget> selected_widgets = Collections.emptyList();

    /** @param listener Listener to add, will also be invoked with current selection. */
    public void addListener(final WidgetSelectionListener listener)
    {
        listeners.add(listener);
        listener.selectionChanged(getSelection());
    }

    /** @param listener Listener to remove. */
    public void removeListener(final WidgetSelectionListener listener)
    {
        if (! listeners.remove(listener))
            throw new IllegalStateException("Unknown listener");
    }

    /** @return Currently selected widgets. May be empty list, never <code>null</code> */
    public List<Widget> getSelection()
    {
        return selected_widgets;
    }

    /** Clear selection, will inform all listeners */
    public void clear()
    {
        setSelection(null);
    }

    /** Update selection, will inform all listeners
     *  @param widgets Currently selected widgets
     */
    public void setSelection(final List<Widget> widgets)
    {
        final List<Widget> safe_list = (widgets == null)
            ? Collections.emptyList()
            : Collections.unmodifiableList(widgets);
        selected_widgets = safe_list;
        for (WidgetSelectionListener listener : listeners)
            listener.selectionChanged(safe_list);
    }

    /** Toggle selection of a widget
     *  @param widget Widget to add if not already in selection,
     *                or remove if it is in the selection
     */
    public void toggleSelection(final Widget widget)
    {
        final List<Widget> update = new ArrayList<>(selected_widgets);
        if (! update.remove(widget))
            update.add(widget);
        setSelection(update);
    }
}
