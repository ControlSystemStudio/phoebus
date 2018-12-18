/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.phoebus.framework.jobs.NamedThreadFactory;

/** Factory of named daemon threads
 *
 *  <p>Primarily, this allows using the Executors.*
 *  with threads names that can be recognized in the
 *  debugger.
 *  @author Kay Kasemir
 */
public class NamedDaemonPool
{
    // Using one thread per CPU core should make best use of the CPU.
    // Having just one such ExecutorService, however, may not be optimal:
    // If the display 'runtime' uses all cores,
    // there should still be headroom for the 'editor' to also use cores
    // --> Create one thread pool for 'model', one for 'editor', one for 'runtime',
    //     so they can later be adjusted to balance the load

    /** Create executor service for model related tasks
     *
     *  @param name Name of the thread pool
     *  @return ExecutorService
     */
    public static ExecutorService createThreadPool(final String name)
    {
        // Idea was this:
        // Runtime tasks are likely waiting on I/O,
        // so have more threads than CPU cores.
        // Still, limit number of threads to some multiple of CPU cores
        // to prevent erroneous creation of a gazillion threads.
        // Core pool size of 0 and timeout results in all threads being
        // closed when none are used for some time.
        // Queue to hold submitted runnables while all threads are busy.
        //
        //      final int threads = Runtime.getRuntime().availableProcessors()*10;
        //      return new ThreadPoolExecutor(0, threads,
        //              10L, TimeUnit.SECONDS,
        //              new LinkedBlockingQueue<Runnable>(),
        //              new NamedDaemonPool(name));
        //
        // In reality, when a thread executing on the DisplayRuntime pool
        // would then submit other threads for execution, those
        // would be invoked sequentially, with only one thread active at a time?!

        // Basically Executors.newCachedThreadPool():
        // Submitted runnables are executed ASAP,
        // either on new thread or one that was in the cache.
        // This allows multiple embedded displays to all load in parallel.
        // Cache clears after 10 seconds.
        // Downside: No way to avoid a gazillion threads.
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                10L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new NamedThreadFactory(name));
    }

    /** Create scheduled executor service for model related tasks
     *
     *  @param name Name of the thread pool
     *  @return ScheduledExecutorService
     */
    public static ScheduledExecutorService createTimer(final String name)
    {
        final ScheduledExecutorService timer = Executors.newScheduledThreadPool(0, new NamedThreadFactory(name));
        ((ThreadPoolExecutor)timer).setKeepAliveTime(10, TimeUnit.SECONDS);
        ((ThreadPoolExecutor)timer).setMaximumPoolSize(1);
        return timer;
    }
}
