package org.phoebus.applications.eslog.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThrottledExecutor
{
    /** Executor used to throttle redraws on new messages. */
    private final ScheduledThreadPoolExecutor updateTimer = new ScheduledThreadPoolExecutor(
            1);
    private final Runnable task;
    long delay;
    TimeUnit unit;

    public ThrottledExecutor(final Runnable task, long delay, TimeUnit unit)
    {
        this.task = task;
        this.delay = delay;
        this.unit = unit;
    }

    public void schedule()
    {
        if (!this.updateTimer.getQueue().isEmpty())
        {
            // already scheduled
            return;
        }
        this.updateTimer.schedule(this.task, this.delay, this.unit);
    }
}
