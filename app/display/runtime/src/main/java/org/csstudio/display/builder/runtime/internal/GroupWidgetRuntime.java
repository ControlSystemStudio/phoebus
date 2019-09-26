/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Group widget Runtime.
 *
 *  <p>Starts/stop the widgets in the group.
 *
 *  @author Kay Kasemir
 */
public class GroupWidgetRuntime extends WidgetRuntime<GroupWidget>
{
    @Override
    public void start()
    {
        super.start();
        RuntimeUtil.startChildRuntimes(widget.runtimeChildren());
    }

    @Override
    public void stop()
    {
        RuntimeUtil.stopChildRuntimes(widget.runtimeChildren());
        super.stop();
    }
}
