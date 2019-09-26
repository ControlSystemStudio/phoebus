/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Tabs widget Runtime.
 *
 *  <p>Starts/stop the widgets in the tabs.
 *
 *  @author Kay Kasemir
 */
public class TabsWidgetRuntime extends WidgetRuntime<TabsWidget>
{
    @Override
    public void start()
    {
        super.start();
        for (TabItemProperty tab : widget.propTabs().getValue())
            RuntimeUtil.startChildRuntimes(tab.children());
    }

    @Override
    public void stop()
    {
        for (TabItemProperty tab : widget.propTabs().getValue())
            RuntimeUtil.stopChildRuntimes(tab.children());
        super.stop();
    }
}
