/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.concurrent.atomic.AtomicReference;
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
    private static enum State
    {
        Init,
        Starting,
        Running,
        Stopping
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.Init);

    @Override
    public void start()
    {
        if (! state.compareAndSet(State.Init, State.Starting))
        {
            logger.log(Level.WARNING, "Skipping display startup for " + widget.getDisplayName() + ", state was " + state.get());
            return;
        }
        logger.log(Level.INFO, () -> "Display Runtime startup for " + widget.getDisplayName() + " ...       ===========");
        super.start();
        RuntimeUtil.startChildRuntimes(widget.runtimeChildren());

        if (state.compareAndSet(State.Starting, State.Running))
            logger.log(Level.INFO, () -> "Display Runtime startup for " + widget.getDisplayName() + " completed ===========");
        else
            logger.log(Level.WARNING, "Display startup for " + widget.getDisplayName() + " ended in " + state.get() + " state");
    }

    @Override
    public void stop()
    {
        if (! state.compareAndSet(State.Running, State.Stopping))
        {
            logger.log(Level.WARNING, "Skipping display shutdown for " + widget.getDisplayName() + ", state was " + state.get());
            return;
        }
        logger.log(Level.INFO, () -> "Display Runtime shutdown for " + widget.getDisplayName() + " ...       ===========");
        RuntimeUtil.stopChildRuntimes(widget.runtimeChildren());
        super.stop();
        if (state.compareAndSet(State.Stopping, State.Init))
            logger.log(Level.INFO, () -> "Display Runtime shutdown for " + widget.getDisplayName() + " completed ===========");
        else
            logger.log(Level.WARNING, "Display shutdown for " + widget.getDisplayName() + " ended in " + state.get() + " state");
    }
}
