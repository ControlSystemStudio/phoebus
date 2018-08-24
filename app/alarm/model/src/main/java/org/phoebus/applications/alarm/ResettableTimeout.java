/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.framework.jobs.NamedThreadFactory;

/** Reset-able timeout
 *
 *  <p>Meant for one-time use:
 *  Timeout is started.
 *  While running, it may be reset several times.
 *  Eventually, if there are no more resets,
 *  it will time out.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResettableTimeout
{
    private final long timeout_secs;

	private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ResettableTimeout"));
    private final CountDownLatch no_more_messages = new CountDownLatch(1);
    private final Runnable signal_no_more_messages = () -> no_more_messages.countDown();
    private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>();

    /** @param timeout_secs Seconds after which we time out */
    public ResettableTimeout(final long timeout_secs)
	{
	    this.timeout_secs = timeout_secs;
		reset();
	}

	/** Reset the timer. As long as this is called within the timeout, we keep running */
    public void reset()
    {
        final ScheduledFuture<?> previous = timeout.getAndSet(timer.schedule(signal_no_more_messages, timeout_secs, TimeUnit.SECONDS));
        if (previous != null)
            previous.cancel(false);
    }

    /** Waits for a timeout, i.e. no 'reset' within the timeout
     *
     *  @param seconds Seconds to wait for a timeout
     *  @return <code>true</code> if timed out, otherwise (after waiting for 'seconds'), returns <code>false</code>
     */
    public boolean awaitTimeout(final long seconds)
    {
        try
        {
            return no_more_messages.await(seconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex)
        {
        }

        return false;
    }

    public void shutdown()
    {
        final ScheduledFuture<?> previous = timeout.getAndSet(null);
        if (previous != null)
            previous.cancel(false);

        timer.shutdown();
    }
}
