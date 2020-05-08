/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.phoebus.framework.jobs.NamedThreadFactory;

/** Script (Jython, Javascript) Support
 *
 *  <p>Each instance of the support module maintains one interpreter instance.
 *  Script files are parsed/compiled (possibly slow) and can then be executed
 *  multiple times (hopefully faster).
 *
 *  <p>Scripts are executed on one thread per support/interpreter.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptSupport
{
    /** Increment instance numbers across all script support threads */
    private static final ThreadFactory thread_factory = new NamedThreadFactory("ScriptSupport");

    /** Single thread script executor, shared by Jython and Javascript */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(thread_factory);

    /** Futures of submitted scripts to allow cancellation */
    private final Queue<Future<Object>> active_scripts = new ConcurrentLinkedQueue<>();

    // Script supports.
    // Could provide two executors, one for jython and one for javascript,
    // but each one needs to be single-threaded because there's only one interpreter
    // with only one global variable for 'window' etc.
    private final PythonScriptSupport python;
    private final JythonScriptSupport jython;
    private final JavaScriptSupport javascript;

    public ScriptSupport() throws Exception
    {
        python = new PythonScriptSupport(this);
        jython = new JythonScriptSupport(this);
        javascript = new JavaScriptSupport(this);
    }

    /** Prepare script file for submission
     *
     *  @param path Path to the script. May be <code>null</null>.
     *              Added to the script engine's search path
     *              if not null to allow access to other scripts
     *              in the same location.
     *  @param name Name of script, used for messages
     *              and to identify the type of script (*.py, *.js)
     *  @param stream Stream for the script content
     *  @return {@link Script}
     *  @throws Exception on error
     */
    public Script compile(final String path, final String name, final InputStream stream) throws Exception
    {
        if (ScriptInfo.isPython(path, name))
            return python.compile(path, name);
        final InputStream script_stream = patchScript(name, stream);
        if (ScriptInfo.isJython(name))
            return jython.compile(path, name, script_stream);
        else if (ScriptInfo.isJavaScript(name))
            return javascript.compile(name, script_stream);
        throw new Exception("Cannot compile '" + name + "'");
    }

    /** Update legacy package names
     *  @param path Name of script (file name, URL)
     *  @param stream Stream for the script content
     *  @return Patched stream
     *  @throws Exception on error
     */
    private InputStream patchScript(final String path, final InputStream stream) throws Exception
    {
        boolean warned = false;

        final StringBuilder buf = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null)
        {
            // Replace "from org.csstudio.opibuilder.scriptUtil" or
            // "import org.csstudio.opibuilder.scriptUtil",
            // but skip if this is indented within code that tries to handle portability, e.g.:
            // if 'getVersion' in dir(widget):
            //     .. new API
            // else:
            //     from org.csstudio.opibuilder.scriptUtil
            if (line.length() > 0      &&
                line.charAt(0) != ' '  &&
                line.charAt(0) != '\t' &&
                line.contains("org.csstudio.opibuilder.scriptUtil"))
            {
                if (! warned)
                {   // This message can be quite frequent, and in most cases the display will work just fine,
                    // to don't use WARNING let alone SEVERE
                    logger.log(Level.INFO,
                               "Script '" + path + "' accessed deprecated org.csstudio.opibuilder.scriptUtil, " +
                               "update to org.csstudio.display.builder.runtime.script.PVUtil");
                    warned = true;
                }
                line = line.replace("org.csstudio.opibuilder.scriptUtil", "org.csstudio.display.builder.runtime.script");
            }
            buf.append(line).append('\n');
        }
        stream.close();

        return new ByteArrayInputStream(buf.toString().getBytes());
    }

    /** Request that a script gets executed
     *  @param callable {@link Callable} for executing the script
     *  @return Future for script that was just submitted
     */
    Future<Object> submit(final Callable<Object> callable)
    {
        try
        {
            final Future<Object> running = executor.submit(callable);
            // No longer track scripts that have finished
            active_scripts.removeIf(f -> f.isDone());
            active_scripts.add(running);
            return running;
        }
        catch (RejectedExecutionException ex)
        {
            // Rejection happens when we submit a script while the display has closed down
            // Log only at fine level for debugging, otherwise OK to skip the script.
            logger.log(Level.FINE, "Skipping script, display closed", ex);
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Release resources (interpreter, ...) */
    public void close()
    {
        // Prevent new scripts from starting
        executor.shutdown();
        // Interrupt scripts which are still running
        // (OK to cancel() if script already finished)
        for (Future<Object> running : active_scripts)
            running.cancel(true);

        jython.close();
    }
}
