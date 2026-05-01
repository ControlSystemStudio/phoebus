/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;

/** Not an actual Plugin Activator, but providing plugin-related helpers
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    final public static Logger logger = Logger.getLogger(Activator.class.getPackageName());

    @Preference(name="shady_future") private static int[] rgba;
    public static final Color shady_future;

    /** When true, RTTank renders on the shared thread pool (one thread per CPU
     *  core) so many simultaneous Tank / ProgressBar instances update in
     *  parallel.  When false, all renders serialise on a single global thread
     *  (the pre-fix behaviour). Controlled by the {@code parallel_rendering}
     *  preference. */
    @Preference(name="parallel_rendering") public static boolean parallel_rendering;

    /** Thread pool for scrolling, throttling updates
     * 
     *  <p>One per CPU core allows that many plots to run updateImageBuffer in parallel.
     */
    public static final ScheduledExecutorService thread_pool
        =  Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("RTPlot"));

    static
    {
    	AnnotatedPreferences.initialize(Activator.class, "/rt_plot_preferences.properties");
        shady_future = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    public static Image getIcon(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }
}
