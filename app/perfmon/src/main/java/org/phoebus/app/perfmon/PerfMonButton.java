/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.perfmon;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Button (meant for status bar) that shows performance info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PerfMonButton extends Button
{
    private final FPSMonitor fps_monitor;

    public PerfMonButton()
    {
        updateFPS(60.0);
        fps_monitor = new FPSMonitor(this::updateFPS);
        fps_monitor.start();
        setTooltip(new Tooltip("Press for GC"));
        setOnAction(event ->
        {
            Runtime.getRuntime().gc();
        });
    }

    private void updateFPS(double fps)
    {
        final long free = Runtime.getRuntime().freeMemory();
        final long total = Runtime.getRuntime().totalMemory();

        final long avail = (int) ((free * 100) / total);
        final double gb = total / 1024.0 / 1024.0 / 1024.0;

        setText(String.format("Avail: %d%% of %.2fGB. FPS: %.1f", avail, gb, fps));
    }

    public void dispose()
    {
        fps_monitor.stop();
    }
}
