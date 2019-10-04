/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.internal.JythonSupport;
import org.phoebus.util.time.SecondsParser;

/** {@link ScanCommandImpl} that delays the scan for some time
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DelayCommandImpl extends ScanCommandImpl<DelayCommand>
{
    /** Helper to await the delay while allowing 'next' */
    final private Semaphore done = new Semaphore(1);
    private volatile Instant started = null;

    /** {@inheritDoc} */
    public DelayCommandImpl(final DelayCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        context.logExecutionStep(command.toString(), command.getSeconds());
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext command_context) throws Exception
    {
        // Reset semaphore
        done.tryAcquire();

        started = Instant.now();
        try
        {
            // Wait for duration of delay. next() may release early
            final long millis = Math.round(command.getSeconds()*1000);
            done.tryAcquire(millis, TimeUnit.MILLISECONDS);
        }
        finally
        {
            started = null;
        }

        command_context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        done.release();
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        String info = super.toString();
        final Instant start = started;
        if (start != null)
        {
            final Instant now = Instant.now();
            final Duration duration = Duration.between(start, now);
            info += ". Elapsed: " + SecondsParser.formatSeconds(duration.getSeconds() + duration.getNano()*1e-9);
        }
        return info;
    }
}
