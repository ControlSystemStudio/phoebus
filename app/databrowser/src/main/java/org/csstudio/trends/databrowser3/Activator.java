/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Global Data Browser helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    /** Logger for all Data Browser code */
    public static final Logger logger = Logger.getLogger(Activator.class.getPackageName());

    /** Dedicated timer for scrolling/updating plots.
     *
     *  <p>This timer is to be used for short-running tasks.
     */
    public static final ScheduledExecutorService timer;


    /** Thread pool, mostly for long running tasks like fetching archived data.
     *
     *  <p>No upper limit for threads.
     *  Removes all threads after 10 seconds
     */
    public static final ExecutorService thread_pool;

    static
    {
        // Up to 1 timer thread, delete when more than 10 seconds idle
        timer = Executors.newScheduledThreadPool(0, new NamedThreadFactory("DataBrowserTimer"));
        ((ThreadPoolExecutor)timer).setKeepAliveTime(10, TimeUnit.SECONDS);
        ((ThreadPoolExecutor)timer).setMaximumPoolSize(1);

        // After 10 seconds, delete all idle threads
        thread_pool = Executors.newCachedThreadPool(new NamedThreadFactory("DataBrowserPool"));
       ((ThreadPoolExecutor)thread_pool).setKeepAliveTime(10, TimeUnit.SECONDS);
    }

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link Image}
     *  @throws Exception on error
     */
    public static Image getImage(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link ImageView}
     *  @throws Exception on error
     */
    public static ImageView getIcon(final String base_name)
    {
        return new ImageView(getImage(base_name));
    }

    /*
    public static void main(String[] args) throws Exception
    {
        // Thread pool grows to allow 'unlimited' number of submitted tasks..
        for (int i=0; i<10; ++i)
        {
            final int id = i;
            thread_pool.submit(() ->
            {
                for (int j=0; j<5; ++j)
                {
                    System.out.println(id + " running on " + Thread.currentThread().getName());
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                System.out.println(Thread.currentThread().getName() + " running...");
            });
        }

        // timer uses one thread for 'scheduled' items
        // If Task A takes a long time, scheduled task B doesn't
        // get to run until task A completes.
        timer.schedule(() ->
        {
            System.out.println("Long task A ...");
            try
            {
                TimeUnit.SECONDS.sleep(4);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println("Long task A Done.");
        }, 1, TimeUnit.SECONDS);

        timer.schedule(() ->
        {
            System.out.println("Long task B ...");
            try
            {
                TimeUnit.SECONDS.sleep(2);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println("Long task B Done.");
        }, 2, TimeUnit.SECONDS);

        TimeUnit.SECONDS.sleep(10);
    }
    */
}
