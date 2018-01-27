/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.csstudio.javafx.rtplot.util.NamedThreadFactory;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

/** Not an actual Plugin Activator, but providing plugin-related helpers
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    final public static Logger logger = Logger.getLogger(Activator.class.getPackageName());

    /** Thread pool for scrolling, throttling updates
     *  <p>No upper limit for threads.
     *  Removes all threads after 10 seconds
     */
    public static final ScheduledExecutorService thread_pool;

    static
    {
        // After 10 seconds, delete all idle threads
        thread_pool = Executors.newScheduledThreadPool(0, new NamedThreadFactory("RTPlot"));
        ((ThreadPoolExecutor)thread_pool).setKeepAliveTime(10, TimeUnit.SECONDS);
    }

    public static Image getIcon(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }
}
