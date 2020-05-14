/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.ModelPlugin;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.runtime.Preferences;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.python.core.Options;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.core.PyVersionInfo;
import org.python.core.RegistryKey;
import org.python.util.PythonInterpreter;

/** Jython script support
 *
 *  <p>To debug, see python.verbose which can also be set
 *  as VM property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JythonScriptSupport extends BaseScriptSupport implements AutoCloseable
{
    private final ScriptSupport support;

    final static boolean initialized = init();

    private final PythonInterpreter python;

    /** Perform static, one-time initialization */
    private static boolean init()
    {
        try
        {
            final Properties pre_props = System.getProperties();
            final Properties props = new Properties();

            // Jython 2.7(b3) needed these to set sys.prefix and sys.executable.
            // Locate the jython plugin for 'home' to allow use of /Lib in there
            // final String home = null; // getPluginPath("org.python.jython", "/");

            // If left undefined, initialization of Lib/site.py fails with
            // posixpath.py", line 394, in normpath AttributeError:
            // 'NoneType' object has no attribute 'startswith'
            // props.setProperty("python.home", home);
            // props.setProperty("python.executable", "None");

            // Disable cachedir to avoid creation of cachedir folder.
            // See http://www.jython.org/jythonbook/en/1.0/ModulesPackages.html#java-package-scanning
            // and http://wiki.python.org/jython/PackageScanning
            props.setProperty(RegistryKey.PYTHON_CACHEDIR_SKIP, "true");

            // By default, Jython compiler creates bytecode files xxx$py.class
            // adjacent to the *.py source file.
            // They are owned by the current user, which typically results in
            // problems for other users, who can either not read them, or not
            // write updates after *.py changes.
            // There is no way to have them be created in a different, per-user directory.
            // C Python honors an environment variable PYTHONDONTWRITEBYTECODE=true to
            // disable its bytecode files, but Jython only checks that in its command line launcher.
            // Use the same environment variable in case it's defined,
            // and default to disabled bytecode, i.e. the safe alternative.
            if (System.getenv("PYTHONDONTWRITEBYTECODE") == null)
                Options.dont_write_bytecode = true;
            else
                Options.dont_write_bytecode = Boolean.parseBoolean(System.getenv("PYTHONDONTWRITEBYTECODE"));

            // With python.home defined, there is no more
            // "ImportError: Cannot import site module and its dependencies: No module named site"
            // Skipping the site import still results in faster startup
            props.setProperty("python.import.site", "false");

            // Prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
            props.setProperty("python.console.encoding", "UTF-8");

            // This will replace entries found on JYTHONPATH
            final String python_path = Preferences.python_path;
            if (! python_path.isEmpty())
                props.setProperty("python.path", python_path);

            // Options: error, warning, message (default), comment, debug
            // props.setProperty("python.verbose", "debug");
            // org.python.core.Options.verbose = Py.DEBUG;

            PythonInterpreter.initialize(pre_props, props, new String[0]);
            final PySystemState state = Py.getSystemState();
            final PyList paths = state.path;

            // Add the examples/connect2j to path.
            // During development, examples are in
            // "file:/some/path/phoebus/applications/display/model/target/classes/examples"
            final String examples = ModelPlugin.class.getResource("/examples").toString();
            if (examples.startsWith("file:"))
                paths.add(examples.substring(5) + "/connect2j");
            // In the compiled version, examples are in
            // "jar:file:/some/path/display-model-0.0.1.jar!/examples"
            else if (examples.startsWith("jar:file:"))
                paths.add(examples.substring(9).replace("jar!/", "jar/") + "/connect2j");
            else
                logger.log(Level.WARNING, "Cannot locate examples/connect2j from " + examples);

            final PyVersionInfo version = PySystemState.version_info;
            logger.log(Level.INFO, "Initial Paths for Jython " + version.major + "." + version.minor + "." + version.micro + ": " + paths);

            // Scripts would sometimes fail in "from ... import ..." with this error:
            //
            // File "..jython-standalone-2.7.1.jar/Lib/warnings.py", line 226, in warn
            // IndexError: index out of range: 0
            //
            // That version of Lib/warnings.py:226 tries to read sys.argv[0],
            // so setting sys.argv[0] avoids the crash.
            // Since state is shared by all scripts in a display,
            // set it to a generic "DisplayBuilderScript"
            state.argv.clear();
            state.argv.add("DisplayBuilderScript");

            return true;
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Once this worked OK, but now the Jython initialization failed. Don't you hate computers?", ex);
        }
        return false;
    }

    /** Create executor for jython scripts
     *  @param support {@link ScriptSupport}
     */
    public JythonScriptSupport(final ScriptSupport support) throws Exception
    {
        this.support = support;

        // Concurrent creation of python interpreters has in past resulted in
        //     Lib/site.py", line 571, in <module> ..
        //     Lib/sysconfig.py", line 159, in _subst_vars AttributeError: {'userbase'}
        // or  Lib/site.py", line 122, in removeduppaths java.util.ConcurrentModificationException
        // Sync. on JythonScriptSupport to serialize the interpreter creation and avoid above errors.
        final long start = System.currentTimeMillis();
        synchronized (JythonScriptSupport.class)
        {
            // Could create a new 'state' for each interpreter
            // ++ Seems 'correct' since each interpreter then has its own path etc.
            // -- In scan server, some instances of the PythonInterpreter seemed
            //    to fall back to the default PySystemState even though
            //    a custom state was provided. Seemed related to thread local,
            //    not fully understood.
            // -- Using a new PySystemState adds about 3 second startup time,
            //    while using the default state only incurs that 3 second delay
            //    on very first access.
            // ==> Not using state = new PySystemState();
            final PySystemState state = null;
            python = new PythonInterpreter(null, state);
        }
        final long end = System.currentTimeMillis();
        logger.log(Level.FINE, "Time to create jython: {0} ms", (end - start));
    }

    /** @param path Path to add to head of python search path */
    private void addToPythonPath(final String path)
    {
        // Since using default PySystemState (see above), check if already in paths
        final PyList paths = python.getSystemState().path;

        // Prevent concurrent modification
        synchronized (JythonScriptSupport.class)
        {
            final int index = paths.indexOf(path);

            // Warn about "examples:/... path that won't really work.
            // Still add to the list so we only get the warning once,
            // plus maybe some day we'll be able to use it...
            if (index < 0  &&
                path.startsWith(ModelResourceUtil.EXAMPLES_SCHEMA + ":"))
                logger.log(Level.WARNING, "Jython will be unable to access scripts in " + path + ". Install examples in file system.");

            // Already top entry?
            if (index == 0)
                return;
            // Remove if further down in the list
            if (index > 0)
                paths.remove(index);
            // Add to front of list
            paths.add(0, path);
        }
        logger.log(Level.FINE, "Adding to jython path: {0}", path);
    }

    /** Parse and compile script file
     *
     *  @param path Path to add to search path, or <code>null</code>
     *  @param name Name of script (file name, URL)
     *  @param stream Stream for the script content
     *  @return {@link Script}
     *  @throws Exception on error
     */
    public Script compile(final String path, final String name, final InputStream stream) throws Exception
    {
        if (path != null)
            addToPythonPath(path);
        final long start = System.currentTimeMillis();
        final PyCode code = python.compile(new InputStreamReader(stream), name);
        final long end = System.currentTimeMillis();
        logger.log(Level.FINE, "Time to compile {0}: {1} ms", new Object[] { name, (end - start) });
        return new JythonScript(this, name, code);
    }

    /** Request that a script gets executed
     *  @param script {@link JythonScript}
     *  @param widget Widget that requests execution
     *  @param pvs PVs that are available to the script
     *  @return Future for script that was just started
     */
    public Future<Object> submit(final JythonScript script, final Widget widget, final RuntimePV... pvs)
    {
        // Skip script that's already in the queue.
        if (! markAsScheduled(script))
            return null;

        // System.out.println("Submit on " + Thread.currentThread().getName());
        return support.submit(() ->
        {
            // System.out.println("Executing " + script + " on " + Thread.currentThread().getName());
            // Script may be queued again
            removeScheduleMarker(script);
            try
            {
                // Executor is single-threaded.
                // Should be OK to set 'widget' etc.
                // of the shared python interpreter
                // because only one script will execute at a time.
                // Still, occasionally saw NullPointerException at
                // org.python.core.PyType$MROMergeState.isMerged(PyType.java:2094)
                // from the set("widget"..) call.
                // Moving those into sync. section to see if that makes a difference
                synchronized (JythonScriptSupport.class)
                {
                    python.set("widget", widget);
                    python.set("pvs", pvs);
                }
                logger.log(Level.INFO, () -> "Exec " + script + " for " + widget + " in " + python + ", locals: " + python.getLocals());
                // .. but don't want to block for the duration of the script
                python.exec(script.getCode());
            }
            catch (final Throwable ex)
            {
                final StringBuilder buf = new StringBuilder();
                buf.append("Script execution failed\n");
                try
                {
                    final DisplayModel model = widget.getDisplayModel();
                    buf.append("Display '").append(model.getDisplayName()).append("', ");
                }
                catch (Exception ignore)
                {
                    // Skip display model
                }
                buf.append(widget).append(", ").append(script);
                logger.log(Level.WARNING, buf.toString(), ex);
            }
            finally
            {
                // Clear because otherwise PySystemState keeps widget and PVs in memory
                python.set("pvs", null);
                python.set("widget", null);
            }
            // System.out.println("Finished " + script);
            return null;
        });
    }

    /** Release resources (interpreter, ...) */
    @Override
    public void close()
    {
        python.close();
    }
}
