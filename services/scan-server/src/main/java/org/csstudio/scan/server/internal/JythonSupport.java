/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.internal;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.scan.server.ScanServerInstance;
import org.python.core.Options;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyVersionInfo;
import org.python.core.RegistryKey;
import org.python.util.PythonInterpreter;

/** Helper for obtaining Jython interpreter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JythonSupport implements AutoCloseable
{
    static final boolean initialized = init();

    final private PythonInterpreter interpreter;

    /** Perform static, one-time initialization */
    private static boolean init()
    {
        final List<String> paths = new ArrayList<>();
        try
        {
            final Properties pre_props = System.getProperties();
            final Properties props = new Properties();

            // Compare jython setup with
            // org.csstudio.display.builder.runtime.script.internal.JythonScriptSupport

            // Disable cachedir to avoid creation of cachedir folder.
            // See http://www.jython.org/jythonbook/en/1.0/ModulesPackages.html#java-package-scanning
            // and http://wiki.python.org/jython/PackageScanning
            props.setProperty(RegistryKey.PYTHON_CACHEDIR_SKIP, "true");

            // By default, Jython compiler creates bytecode files xxx$py.class
            // adjacent to the *.py source file.
            // They are owned by the current user, which typically results in
            // problems for other users, who can either not read them, or not
            // write updates after *.py changes.
            Options.dont_write_bytecode = true;

            // With python.home defined, there is no more
            // "ImportError: Cannot import site module and its dependencies: No module named site"
            // Skipping the site import still results in faster startup
            props.setProperty("python.import.site", "false");

            // Prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
            props.setProperty("python.console.encoding", "UTF-8");

            // Options: error, warning, message (default), comment, debug
            // props.setProperty("python.verbose", "debug");

            // No need to add numjy,
            // it's found on __pyclasspath__ because it's in the scan-model module

            // Add scan script paths
            for (String pref_path : ScanServerInstance.getScanConfig().getScriptPaths())
            {
                // Resolve built-in examples
                if (pref_path.startsWith(PathStreamTool.EXAMPLES))
                {
                    String path = PathStreamTool.patchExamplePath(pref_path);
                    final URL url = ScanServerInstance.class.getResource(path);
                    if (url == null)
                        throw new Exception("Error in scan script path " + pref_path);
                    path = url.toExternalForm();
                    // Patch file:/path/to/the_file.jar!/path/within
                    if (path.startsWith("file:"))
                        path = path.substring(5);
                    path = path.replace(".jar!", ".jar");
                    paths.add(path);
                }
                else // Add as-is
                    paths.add(pref_path);
            }

            props.setProperty("python.path", paths.stream().collect(Collectors.joining(java.io.File.pathSeparator)));

            PythonInterpreter.initialize(pre_props, props, new String[0]);
            final PySystemState state = Py.getSystemState();
            final PyVersionInfo version = PySystemState.version_info;
            logger.log(Level.INFO, "Initial Paths for Jython " + version.major + "." + version.minor + "." + version.micro + ":");
            for (Object o : state.path)
                logger.log(Level.INFO, " * " + Objects.toString(o));

            // Display Builder Scripts would sometimes fail in "from ... import ..." with this error:
            //
            // File "..jython-standalone-2.7.1.jar/Lib/warnings.py", line 226, in warn
            // IndexError: index out of range: 0
            //
            // That version of Lib/warnings.py:226 tries to read sys.argv[0],
            // so setting sys.argv[0] avoids the crash.
            state.argv.clear();
            state.argv.add("ScanServerScript");
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Once this worked OK, but now the Jython initialization failed. Don't you hate computers?", ex);
            return false;
        }
        return true;
    }

    /** Initialize
     *  @throws Exception on error
     */
    public JythonSupport() throws Exception
    {
        final PySystemState state = new PySystemState();

        // Creating a PythonInterpreter is very slow.
        //
        // In addition, concurrent creation is not supported, resulting in
        //     Lib/site.py", line 571, in <module> ..
        //     Lib/sysconfig.py", line 159, in _subst_vars AttributeError: {'userbase'}
        // or  Lib/site.py", line 122, in removeduppaths java.util.ConcurrentModificationException
        //
        // Sync. on JythonSupport to serialize the interpreter creation and avoid above errors.
        // Curiously, this speeds the interpreter creation up,
        // presumably because they're not concurrently trying to access the same resources?
        synchronized (JythonSupport.class)
        {
            interpreter = new PythonInterpreter(null, state);
        }
    }

    /** Load a Jython class
     *
     *  @param type Type of the Java object to return
     *  @param class_name Name of the Jython class,
     *                    must be in package (file) using lower case of class name
     *  @param args Arguments to pass to constructor
     *  @return Java object for instance of Jython class
     *  @throws Exception on error
     */
    @SuppressWarnings("unchecked")
    public <T> T loadClass(final Class<T> type, final String class_name, final String... args) throws Exception
    {
        // Get package name
        final String pack_name = class_name.toLowerCase();
        logger.log(Level.FINE, "Loading Jython class {0} from {1}",
                   new Object[] { class_name, pack_name });

        try
        {
            // Import class into Jython
            // Debug: Print the path that's actually used
            // final String statement = "import sys\nprint sys.path\nfrom " + pack_name +  " import " + class_name;
            final String statement = "from " + pack_name +  " import " + class_name;
            interpreter.exec(statement);
        }
        catch (PyException ex)
        {
            logger.log(Level.WARNING, "Error loading Jython class {0} from {1}",
                new Object[] { class_name, pack_name });
            logger.log(Level.WARNING, "Jython sys.path:\n * {0}",
                       interpreter.getSystemState()
                                  .path
                                  .stream()
                                  .collect(Collectors.joining("\n * ")));

            throw new Exception("Error loading Jython class " + class_name + ":" + getExceptionMessage(ex), ex);
        }
        // Create Java reference
        final PyObject py_class = interpreter.get(class_name);
        final PyObject py_object;
        if (args.length <= 0)
            py_object = py_class.__call__();
        else
        {
            final PyObject[] py_args = new PyObject[args.length];
            for (int i=0; i<py_args.length; ++i)
                py_args[i] = new PyString(args[i]);
            py_object = py_class.__call__(py_args);
        }
        final T java_ref = (T) py_object.__tojava__(type);
        return java_ref;
    }

    /** We can only report the message of an exception back to scan server
     *  clients, not the whole exception because it doesn't 'serialize'.
     *  The PyException, however, tends to have no message at all.
     *  This helper tries to generate a somewhat useful message
     *  from the content of the exception.
     *  @param ex Python exception
     *  @return Message with info about python exception
     */
    public static String getExceptionMessage(final PyException ex)
    {
        final StringBuilder buf = new StringBuilder();
        if (ex.value instanceof PyString)
            buf.append(" ").append(ex.value.asString());
        else if (ex.getCause() != null)
            buf.append(" ").append(ex.getCause().getMessage());
        if (ex.traceback != null)
        {
            buf.append(" ");
            ex.traceback.dumpStack(buf);
        }
        return buf.toString();
    }

    /** Close the interpreter, release resources */
    @Override
    public void close() throws Exception
    {
        interpreter.close();
    }
}
