/*******************************************************************************
 * Copyright (c) 2010-2021 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import org.phoebus.applications.alarm.client.AlarmClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import org.phoebus.framework.jobs.NamedThreadFactory;

/** Filter for alarm enablement based on datetime
 *  @author Jacqueline Garrahan
 */
@SuppressWarnings("nls")
public class EnabledDateTimeFilter
{
    /** Timer for re-enable */
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("EnableDateTimeFilter"));

    /** Listener to notify when reactivated */
    final private Consumer<Boolean> listener;

    private volatile LocalDateTime enable_date = null;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private volatile ScheduledFuture<?> scheduled_execution = null;


    /** Initialize
     */
    public EnabledDateTimeFilter(final LocalDateTime enable_date,
                  final Consumer<Boolean> listener)
    {
        this.listener = listener;
        this.enable_date = enable_date;
        Duration duration = Duration.between(LocalDateTime.now(), this.enable_date);
        this.scheduled_execution = TIMER.schedule(EnabledDateTimeFilter.this::enable, duration.toSeconds(), TimeUnit.SECONDS);
    }


    /** Send alarm enable */
    private void enable()
    {
        listener.accept(true);
    }

    /** Cancel pending event */
    public void cancel() {
        scheduled_execution.cancel(false);
    }


    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("EnableDateTimeFilter '").append(enable_date.format(formatter));
        return buf.toString();
    }
}
