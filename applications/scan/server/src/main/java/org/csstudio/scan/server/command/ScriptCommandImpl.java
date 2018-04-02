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

import org.csstudio.scan.command.ScanScript;
import org.csstudio.scan.command.ScanScriptContext;
import org.csstudio.scan.command.ScriptCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.internal.JythonSupport;
import org.python.core.PyException;

/** {@link ScanCommandImpl} that executes a script
 *
 *  <p>Loads python code in constructor to allow early
 *  failure for basic Python syntax errors.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptCommandImpl extends ScanCommandImpl<ScriptCommand>
{
    final private ScanScript script_object;

    /** Thread that executes the loop variable write
     *
     *  SYNC on `this` to prevent race where
     *  executing thread tries to set thread back to null,
     *  while next() tries to interrupt.
     */
    private Thread thread = null;

    /** Flag to indicate 'next' was invoked */
    private volatile boolean is_cancelled;

    /** {@inheritDoc} */
    public ScriptCommandImpl(final ScriptCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        try
        {
            script_object = jython.loadClass(ScanScript.class, command.getScript(), command.getArguments());
        }
        catch (PyException ex)
        {
            throw new Exception(JythonSupport.getExceptionMessage(ex), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        try
        {
            final String[] names = script_object.getDeviceNames();
            for (int i=0; i<names.length; ++i)
                names[i] = macros.resolveMacros(names[i]);
            return names;
        }
        catch (PyException ex)
        {
            throw new Exception(command.getScript() + ":" + JythonSupport.getExceptionMessage(ex), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        is_cancelled = false;
        synchronized (this)
        {
            thread = Thread.currentThread();
        }
        try
        {
            final ScanScriptContext script_context = new ScriptCommandContextImpl(context);
            script_object.run(script_context);
        }
        catch (PyException ex)
        {
            // Ignore if 'next' was requested
            if (! is_cancelled)
                throw new Exception(command.getScript() + ":" + JythonSupport.getExceptionMessage(ex), ex);
        }
        finally
        {
            synchronized (this)
            {
                thread = null;
            }
        }

        context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        is_cancelled = true;
        synchronized (this)
        {
            if (thread != null)
                thread.interrupt();
        }
    }
}
