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

import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Runtime for the {@link DataBrowserWidget}
 *
 *  <p>Adds 'configure' and toolbar entries to context menu.
 *
 *  <p>Updates 'selection_value' from 'selection_value_pv'.
 *  Selection PV must be set when runtime starts.
 *  Can not be changed while running.
 *
 *  @author Megan Grodowitz Initial version
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartWidgetRuntime  extends WidgetRuntime<StripchartWidget>
{
    private final List<RuntimeAction> runtime_actions = new ArrayList<>(2);

    @Override
    public void initialize(final StripchartWidget widget)
    {
        super.initialize(widget);
        runtime_actions.add(new ConfigureAction("Configure Plot", widget.runtimePropConfigure()));
        runtime_actions.add(new ToggleToolbarAction(widget));
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions()
    {
        return runtime_actions;
    }
}
