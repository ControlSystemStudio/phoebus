/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Display Runtime.
 *
 *  <p>Initializes display-wide facilities
 *  and starts/stop the widgets in the display.
 *
 *  @author Kay Kasemir
 */
public class DisplayRuntime extends WidgetRuntime<DisplayModel>
{
    @Override
    public void start() throws Exception
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
