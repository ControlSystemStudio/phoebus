/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.perfmon;

import java.util.function.DoubleConsumer;

import javafx.animation.AnimationTimer;

/** Frame-per-second monitor
 *  @author Kay Kasemir
 */
public class FPSMonitor extends AnimationTimer
{
    // Number of frames to use for average
    // Based on nominal 60Hz, this updates every 3 seconds
    private static final int AVG = 60*3;

    private final DoubleConsumer fps_handler;
    private long updates = 0;
    private long last = 0;
    private double fps = 60.0;

    /** @param fps_handler Invoked with frames-per-second */
    public FPSMonitor(DoubleConsumer fps_handler)
    {
        this.fps_handler = fps_handler;
    }

    @Override
    public void handle(long now)
    {
        // System.out.println(now);
        if (++updates > AVG)
        {
            if (last > 0)
            {
                long nanos = (now - last)  / AVG;
                fps = 1.0e9 / nanos;
                fps_handler.accept(fps);
            }
            updates = 0;
            last = now;
        }
    }
}
