/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Runtime for the {@link StripchartWidget}
 *
 *  <p>Adds 'configure' and toolbar entries to context menu.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartWidgetRuntime  extends WidgetRuntime<StripchartWidget>
{
    private class OpenDataBrowserAction extends RuntimeAction
    {
        OpenDataBrowserAction()
        {
            super(Messages.OpenDataBrowser, "/icons/databrowser.png");
        }

        @Override
        public void run()
        {
            widget.runtimePropOpenDataBrowser().trigger();
        }
    }

    private final List<RuntimeAction> runtime_actions = new ArrayList<>(3);

    @Override
    public void initialize(final StripchartWidget widget)
    {
        super.initialize(widget);
        runtime_actions.add(new ConfigureAction("Configure Plot", widget.runtimePropConfigure()));
        runtime_actions.add(new ToggleToolbarAction(widget));
        runtime_actions.add(new OpenDataBrowserAction());
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions()
    {
        return runtime_actions;
    }
}
