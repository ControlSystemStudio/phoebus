/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.NamedDaemonPool;

/** Runtime that generates dummy widget updates
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DummyRuntime
{
    final private ScheduledExecutorService timer = NamedDaemonPool.createTimer("DemoTimer");

    public DummyRuntime(final DisplayModel model)
    {
        startWidgetRuntime(model);
    }

    private void startWidgetRuntime(final Widget widget)
    {
        if (widget.getType().equals("label"))
            timer.scheduleWithFixedDelay(new DummyTextUpdater(widget), 100, 100, TimeUnit.MILLISECONDS);
        else if (widget.getType().equals("rectangle"))
            timer.scheduleWithFixedDelay(new DummyPositionUpdater(widget), 100, 100, TimeUnit.MILLISECONDS);

        final ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
            for (final Widget child : children.getValue())
                startWidgetRuntime(child);
    }

    public void shutdown()
    {
        timer.shutdown();
    }
}