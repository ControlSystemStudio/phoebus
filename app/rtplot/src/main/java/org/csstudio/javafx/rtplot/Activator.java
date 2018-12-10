/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

/** Not an actual Plugin Activator, but providing plugin-related helpers
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    final public static Logger logger = Logger.getLogger(Activator.class.getPackageName());

    public static final Color shady_future;

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

        final PreferencesReader prefs = new PreferencesReader(Activator.class, "/rt_plot_preferences.properties");
        final String[] rgba = prefs.get("shady_future").split("\\s*,\\s*");
        shady_future = new Color(Integer.parseInt(rgba[0]),Integer.parseInt(rgba[1]), Integer.parseInt(rgba[2]), Integer.parseInt(rgba[3]));
    }

    public static Image getIcon(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }
}
