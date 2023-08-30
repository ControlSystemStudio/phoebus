/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import static org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.runtimeModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;

/** Collect widget counts
 *  @author Kay Kasemir
 */
public class DisplayWidgetStats
{
    /** Map of widget type to count */
    private final Map<String, AtomicInteger> types = new HashMap<>();
    private int rules = 0;
    private int scripts = 0;
    private int total = 0;

    /** @param model Display for which to create statistics */
    public DisplayWidgetStats(final DisplayModel model)
    {
        countWidgets(model);
    }

    /** @return Map of widget type to count */
    public Map<String, AtomicInteger> getTypes()
    {
        return types;
    }

    /** @return Number of rules */
    public int getRules()
    {
        return rules;
    }

    /** @return Number of scripts */
    public int getScripts()
    {
        return scripts;
    }

    /** @return Number of widgets */
    public int getTotal()
    {
        return total;
    }

    private void countWidgets(final Widget widget)
    {
        // Counts the WidgetModel as type 'display'
        // just like any other widget.
        ++total;

        rules += widget.propRules().getValue().size();
        scripts += widget.propScripts().getValue().size();

        // Create resp. increment count for this widget's type
        final String type = widget.getType();
        types.compute(type, (t, c) ->
        {
            if (c == null)
                return new AtomicInteger(1);
            c.incrementAndGet();
            return c;
        });

        // Recurse into embedded or child widgets
        if (widget instanceof EmbeddedDisplayWidget || widget instanceof NavigationTabsWidget)
        {
            final Optional<WidgetProperty<DisplayModel>> optPropModel = widget.checkProperty(runtimeModel);
            if (optPropModel.isPresent())
            {
                final DisplayModel emb_model = optPropModel.get().getValue();
                if (emb_model != null)
                    countWidgets(emb_model);
            }
        }
        else if (widget instanceof TabsWidget)
        {
            final List<TabItemProperty> tabs = ((TabsWidget)widget).propTabs().getValue();
            for (TabItemProperty tab : tabs)
                for (Widget child : tab.children().getValue())
                    countWidgets(child);
        }
        else
        {
            final ChildrenProperty children = ChildrenProperty.getChildren(widget);
            if (children != null)
                for (Widget child : children.getValue())
                    countWidgets(child);
        }
    }
}
