/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread factory that creates named daemon threads.
 *
 *  <p>To ease debugging over the default
 *  {@link Executors} "pool-x-thread-y" naming.
 *
 *  @author Kay Kasemir
 */
public class NamedThreadFactory implements ThreadFactory
{
    final private String name;
    final private AtomicInteger count = new AtomicInteger(1);

    public NamedThreadFactory(final String name)
    {
        this.name = name;
    }

    @Override
    public Thread newThread(final Runnable runnable)
    {
        final int number = count.getAndIncrement();
        final String thread_name = number == 1 ? name : name + number;
        final Thread thread = new Thread(runnable, thread_name);
        thread.setDaemon(true);
        return thread;
    }
}
