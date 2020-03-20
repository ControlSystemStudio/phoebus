/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.logging.Level;

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
@SuppressWarnings("nls")
public class DisplayRuntime extends WidgetRuntime<DisplayModel>
{
    @Override
    public void start()
    {
        logger.log(Level.INFO, () -> "Display Runtime startup for " + widget.getDisplayName() + " ...       ===========");
        super.start();
        RuntimeUtil.startChildRuntimes(widget.runtimeChildren());
        logger.log(Level.INFO, () -> "Display Runtime startup for " + widget.getDisplayName() + " completed ===========");
    }

    @Override
    public void stop()
    {
        logger.log(Level.INFO, () -> "Display Runtime shudown for " + widget.getDisplayName() + " ...       ===========");
        RuntimeUtil.stopChildRuntimes(widget.runtimeChildren());
        super.stop();
        logger.log(Level.INFO, () -> "Display Runtime shudown for " + widget.getDisplayName() + " completed ===========");
    }
}
