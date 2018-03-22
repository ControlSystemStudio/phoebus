/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.condition;

import java.time.Duration;

/** Helper for waiting with a time out
 *  <code>
 *  WaitWithTimeout timeout = new WaitWithTimeout(TimeDuration.ofSeconds(2.0));
 *  synchronized (lock)
 *  {
 *      while (! some_condition_met)
 *          if (timeout.waitUntilTimeout(lock))
 *              throw new TimeoutException("Timeout has expired");
 *  }
 *  </code>
 *  @author Kay Kasemir
 */
public class WaitWithTimeout
{
    final private long end_ms;

    /** Initialize time out to start 'now'
     *  @param duration Overall duration of the timeout, <code>null</code> to wait 'forever'
     */
    public WaitWithTimeout(final Duration duration)
    {
        end_ms = (duration != null  &&  duration.toMillis() > 0)
            ? System.currentTimeMillis() + duration.toMillis()
            : 0;
    }

    /** Wait on lock until timeout, or notification on lock
     *
     *  <p>If notified, but condition is not met, can be invoked again,
     *  keeping track of the overall duration of the desired timeout.
     *
     *  @param lock Object to wait on
     *  @return <code>true</code> if the wait timed out,
     *          <code>false</code> if wait was notified within time
     *  @throws InterruptedException if wait is interrupted
     */
    public boolean waitUntilTimeout(final Object lock) throws InterruptedException
    {
        if (end_ms > 0)
        {   // With timeout, see how much time is left
            final long ms_left = end_ms - System.currentTimeMillis();
            if (ms_left > 0)
                lock.wait(ms_left);
            else
                return true;
        }
        else // No timeout, wait forever
            lock.wait();
        return false;
    }
}
