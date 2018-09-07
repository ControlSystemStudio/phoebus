/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;

/** One item in the model outline: Either a {@link Widget} or a {@link TabItemProperty}
 *  @author Kay Kasemir
 */
class WidgetOrTab
{
    private final Object widget_or_tab;

    static WidgetOrTab of(final Widget widget)
    {
        return new WidgetOrTab(widget);
    }

    static WidgetOrTab of(final TabItemProperty tab)
    {
        return new WidgetOrTab(tab);
    }

    private WidgetOrTab(Object w_o_t)
    {
        widget_or_tab = w_o_t;
    }

    boolean isWidget()
    {
        return widget_or_tab instanceof Widget;
    }

    Widget getWidget()
    {
        return (Widget)widget_or_tab;
    }

    TabItemProperty getTab()
    {
        return (TabItemProperty)widget_or_tab;
    }

    @Override
    public int hashCode()
    {
        return widget_or_tab == null ? 0 : widget_or_tab.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof WidgetOrTab))
            return false;
        return ((WidgetOrTab) obj).widget_or_tab == widget_or_tab;
    }
}
