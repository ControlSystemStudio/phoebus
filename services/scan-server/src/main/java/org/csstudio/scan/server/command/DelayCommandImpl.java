/*******************************************************************************
 * Copyright (c) 2011-2019 Oak Ridge National Laboratory.
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

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.info.ScanState;
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
    private volatile long remaining_ms = -1;

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

        final long millis = Math.round(command.getSeconds()*1000);
        remaining_ms = millis;
        boolean paused = false;
        Instant end = Instant.now().plusMillis(millis);
        try
        {
            while (remaining_ms > 0  &&  ! done.tryAcquire(Math.min(remaining_ms, 100), TimeUnit.MILLISECONDS))
            {
                if (command_context.getScanState() == ScanState.Paused)
                {
                    if (! paused)
                    {
                        paused = true;
                        logger.log(Level.INFO, "Delay paused, remaining: " + SecondsParser.formatSeconds(remaining_ms/1000.0));
                    }
                }
                else
                {
                    if (paused)
                    {
                        paused = false;
                        end = Instant.now().plusMillis(remaining_ms);
                        logger.log(Level.INFO, "Delay resumed");
                    }
                    else
                        remaining_ms = Duration.between(Instant.now(), end).toMillis();
                }
            }
        }
        finally
        {
            remaining_ms = -1;
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
        final long remain = remaining_ms;
        if (remain > 0)
            info += ". Remaining: " + SecondsParser.formatSeconds(remain/1000.0);
        return info;
    }
}
