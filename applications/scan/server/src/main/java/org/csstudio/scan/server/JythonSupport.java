/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
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
        final List<String> paths = new ArrayList<String>();
        try
        {
            final Properties pre_props = System.getProperties();
            final Properties props = new Properties();

            // TODO Jython setup
            // Locate the jython plugin for 'home' to allow use of /Lib in there
            final String home = "/";
            // getPluginPath("org.python.jython", "/");
            if (home == null)
                throw new Exception("Cannot locate jython bundle");

            // Jython 2.7(b3) needs these to set sys.prefix and sys.executable.
            // If left undefined, initialization of Lib/site.py fails with
            // posixpath.py", line 394, in normpath AttributeError:
            // 'NoneType' object has no attribute 'startswith'
            props.setProperty("python.home", home);
            props.setProperty("python.executable", "css");

            // Disable cachedir to avoid creation of cachedir folder.
            // See http://www.jython.org/jythonbook/en/1.0/ModulesPackages.html#java-package-scanning
            // and http://wiki.python.org/jython/PackageScanning
            props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");

            // With python.home defined, there is no more
            // "ImportError: Cannot import site module and its dependencies: No module named site"
            // Skipping the site import still results in faster startup
            props.setProperty("python.import.site", "false");

            // Prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
            props.setProperty("python.console.encoding", "UTF-8");

            // Options: error, warning, message (default), comment, debug
            // props.setProperty("python.verbose", "debug");

            // TODO Add scan script paths
//            final String[] pref_paths = ScanSystemPreferences.getScriptPaths();
//            for (String pref_path : pref_paths)
//            {   // Resolve platform:/plugin/...
//                if (pref_path.startsWith("platform:/plugin/"))
//                {
//                    final String plugin_path = pref_path.substring(17);
//                    // Locate name of plugin and path within plugin
//                    final int sep = plugin_path.indexOf('/');
//                    final String plugin, path_within;
//                    if (sep < 0)
//                    {
//                        plugin = plugin_path;
//                        path_within = "/";
//                    }
//                    else
//                    {
//                        plugin = plugin_path.substring(0, sep);
//                        path_within = plugin_path.substring(sep + 1);
//                    }
//                    final String path = getPluginPath(plugin, path_within);
//                    if (path == null)
//                        throw new Exception("Error in scan script path " + pref_path);
//                    else
//                        paths.add(path);
//                }
//                else // Add as-is
//                    paths.add(pref_path);
//            }

            props.setProperty("python.path", paths.stream().collect(Collectors.joining(java.io.File.pathSeparator)));

            PythonInterpreter.initialize(pre_props, props, new String[0]);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Once this worked OK, but now the Jython initialization failed. Don't you hate computers?", ex);
            return false;
        }
        return true;
    }

//    /** Locate a path inside a bundle.
//    *
//    *  <p>If the bundle is JAR-ed up, the {@link FileLocator} will
//    *  return a location with "file:" and "..jar!/path".
//    *  This method patches the location such that it can be used
//    *  on the Jython path.
//    *
//    *  @param bundle_name Name of bundle
//    *  @param path_in_bundle Path within bundle
//    *  @return Location of that path within bundle, or <code>null</code> if not found or no bundle support
//    *  @throws IOException on error
//    */
//   private static String getPluginPath(final String bundle_name, final String path_in_bundle) throws IOException
//   {
//       final Bundle bundle = Platform.getBundle(bundle_name);
//       if (bundle == null)
//           return null;
//       final URL url = FileLocator.find(bundle, new Path(path_in_bundle), null);
//       if (url == null)
//           return null;
//       String path = FileLocator.resolve(url).getPath();
//       if (path.startsWith("file:/"))
//          path = path.substring(5);
//       path = path.replace(".jar!", ".jar");
//
//       return path;
//   }

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
            logger.log(Level.WARNING, "Search path: {0}",interpreter.getSystemState().path);

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
