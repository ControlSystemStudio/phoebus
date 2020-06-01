/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.scan.command.ParallelCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.internal.JythonSupport;
import org.phoebus.framework.jobs.NamedThreadFactory;

/** Command that executes the commands in its body in parallel
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ParallelCommandImpl extends ScanCommandImpl<ParallelCommand>
{
    final private static ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("ParallelCommands"));

    final private List<ScanCommandImpl<?>> implementation;

    /** Initialize
     *  @param command Command description
     *  @param jython Jython interpreter, may be <code>null</code>
     */
    public ParallelCommandImpl(final ParallelCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        implementation = ScanCommandImplTool.implement(command.getBody(), jython);
    }

    /** Initialize without Jython support
     *  @param command Command description
     */
    public ParallelCommandImpl(final ParallelCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public long getWorkUnits()
    {
        long body_units = 1; // One unit for this command, rest for body
        for (ScanCommandImpl<?> command : implementation)
            body_units += command.getWorkUnits();
        return body_units;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final Set<String> device_names = new HashSet<>();
        for (ScanCommandImpl<?> command : implementation)
        {
            final String[] names = command.getDeviceNames(macros);
            for (String name : names)
                device_names.add(name);
        }
        return device_names.toArray(new String[device_names.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        final int level = context.incParallelLevel();
        final String level_spec = level > 1 ? " (level " + level + ")" : "";
        try
        {
            context.logExecutionStep("Start following commands in parallel" + level_spec, 0.0);
            context.simulate(implementation);
            context.logExecutionStep("Await completion of above commands" + level_spec, 0.0);
        }
        finally
        {
            context.decParallelLevel();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        final long end = command.getTimeout() > 0.0
                ? Math.round(System.currentTimeMillis() + command.getTimeout()*1000)
                : -1;
        final List<Future<Object>> results = new ArrayList<>();
        final Semaphore all_done = new Semaphore(1 - implementation.size());
        final AtomicReference<Exception> first_error = new AtomicReference<>();
        // Start commands in parallel
        for (ScanCommandImpl<?> body_command : implementation)
            results.add(launch(all_done, first_error, context, body_command));

        try
        {
            // Wait for commands to finish
            if (end > 0)
            {
                final long time_left = end - System.currentTimeMillis();
                if (time_left <= 0   ||
                    ! all_done.tryAcquire(time_left, TimeUnit.MILLISECONDS))
                    throw new TimeoutException();
            }
            else
                all_done.acquire();

            // Check if there was an error
            if (first_error.get() != null)
                throw new Exception(first_error.get().getMessage() + " (Parallel)", first_error.get());
        }
        catch (TimeoutException ex)
        {
            throw new Exception("Parallel time out (" + command.getTimeout() + " sec)", ex);
        }
        finally
        {   // In case of interruption or timeout, cancel (interrupt) all body commands.
            // NOP if commands completed gracefully.
            for (Future<Object> result : results)
                result.cancel(true);
        }
        context.workPerformed(1);
    }

    /** Launch one of the body commands
     *  @param all_done
     *  @param first_error
     *  @param context
     *  @param body_command
     *  @return Future for the command, may provide Exception
     */
    private Future<Object> launch(final Semaphore all_done, final AtomicReference<Exception> first_error, final ScanContext context, ScanCommandImpl<?> body_command)
    {
        logger.log(Level.FINE, "Launching: {0}", body_command);
        return executor.submit(new Callable<>()
        {
            @Override
            public Object call() throws Exception
            {
                try
                {
                    context.execute(body_command);
                    // Indicate that this one command completed
                    all_done.release();
                }
                catch (Exception ex)
                {
                    // Set first error (unless already non-null)
                    first_error.compareAndSet(null, ex);
                    // Release enough to wake the Parallel command that started this sub-command
                    all_done.release(implementation.size());
                    throw ex;
                }
                return null;
            }
        });
    }

    // public void next()
    // is not implemented because the 'next' command
    // will be sent to one of the active body commands.
    // Once they have all been completed, this command completes.

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return command.toString();
    }
}
