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
package org.csstudio.scan.server;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanErrorHandler;
import org.csstudio.scan.command.ScanErrorHandler.Result;
import org.csstudio.scan.server.command.ScriptCommandContextImpl;
import org.csstudio.scan.server.internal.JythonSupport;
import org.csstudio.scan.command.ScanScriptContext;
import org.python.core.PyException;

/** Implementation of a command
 *
 *  <p>Wraps a {@link ScanCommand} and allows execution of the command.
 *
 *  <p>Most commands will perform one unit of work,
 *  for example set a PV.
 *  A loop on the other hand will perform one unit of work per loop
 *  iteration.
 *
 *  <p>The {@link ExecutableScan} queries each command for the number of work
 *  units that it will perform, and the command must then update
 *  the {@link ScanContext} with the number of performed work
 *  units.
 *
 *  <p>Work units are just a guess to provide a progress indication.
 *  If a command cannot estimate the number of work units
 *  because it will dynamically perform more or less work,
 *  it should return 1.
 *  Likewise, a delay of 1 second or 1 minute would each be one
 *  work unit, even though their duration differs a lot.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ScanCommandImpl<C extends ScanCommand>
{
    final protected C command;

    final protected JythonSupport jython;

    private ScanErrorHandler error_handler;

    /** Initialize
     *  @param command Command that is implemented
     *  @param jython Jython interpreter, may be <code>null</code>
     *  @throws Exception on error
     */
    public ScanCommandImpl(final C command, final JythonSupport jython) throws Exception
    {
        this.command = command;
        this.jython = jython;

        // Implement error handler?
        final String error_handler_class = command.getErrorHandler();
        if (error_handler_class.isEmpty()  ||  jython == null)
            error_handler = null;
        else
        {
            try
            {
                error_handler = jython.loadClass(ScanErrorHandler.class, error_handler_class);
            }
            catch (PyException ex)
            {
                throw new Exception(JythonSupport.getExceptionMessage(ex), ex);
            }
        }
    }

    /** Set the address of this command.
    *
    *  <p>To be called by scan system, not end user code.
    *
    *  @param address Address of this command within command sequence
    *  @return Address of next command
    */
    final public long setAddress(final long address)
    {
        return command.setAddress(address);
    }

    /** @return {@link ScanCommand} */
    final public C getCommand()
    {
        return command;
    }

    /** Most commands will perform one unit of work,
     *  for example set a PV.
     *  A loop on the other hand will perform one unit of work per loop
     *  iteration, so derived implementations may override.
     *
     *  @return Number of work units that this command performs */
    public long getWorkUnits()
    {
        return 1;
    }

    /** Determine which devices are required by this command
     *  @param macros {@link MacroContext} for resolving macros
     *  @return Device (alias) names used by the command
     *  @throws Exception on macro error
     */
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        return new String[0];
    }

    /** Simulate the command
     *
     *  <p>Should log the execution steps in the {@link SimulationContext}
     *
     *  @param context {@link SimulationContext}
     *  @throws Exception on error
     *
     *  @see SimulationContext#logExecutionStep(String, double)
     */
    public void simulate(final SimulationContext context) throws Exception
    {
        context.logExecutionStep(context.getMacros().resolveMacros(command.toString()), 0.1);
    }

    /** Execute the command
     *
     *  <p>Should update the performed work units on the {@link ScanContext}
     *
     *  @param context {@link ScanContext}
     *  @throws Exception on error
     *
     *  @see ScanContext#workPerformed(int)
     */
    abstract public void execute(ScanContext context) throws Exception;

    /** Short-cut execution of the current command
     *
     *  <p>When a command is inside <code>execute()</code> awaiting some condition,
     *  calling <code>next()</code> will break out of that condition
     *  and end execution of the command.
     *
     *  <p>It does _not_ abort the scan but allows the following commands
     *  to be executed.
     */
    public void next()
    {
        // Default implementation does not support this
        logger.log(Level.WARNING, "{0} does not support 'next'", command);
    }

    /** Invoke the command's error handler
     *
     *  <p>If command has no custom error handler, 'Abort' will be returned.
     *
     *  @param context {@link ScanContext}
     *  @param error Error from call to <code>execute</code>
     *  @return How to proceed
     *  @throws Exception on error while trying to handle the error
     */
    public Result handleError(final ScanContext context, final Exception error) throws Exception
    {
        if (error_handler == null)
            return ScanErrorHandler.Result.Abort;
        else
        {
            try
            {
                final ScanScriptContext script_context = new ScriptCommandContextImpl(context);
                return error_handler.handleError(command, error, script_context);
            }
            catch (PyException ex)
            {
                throw new Exception("Error handler for " + command.getCommandName() + ":" +
                        JythonSupport.getExceptionMessage(ex), ex);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return command.toString();
    }
}
