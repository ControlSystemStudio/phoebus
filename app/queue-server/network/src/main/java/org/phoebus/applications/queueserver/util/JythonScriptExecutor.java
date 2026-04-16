package org.phoebus.applications.queueserver.util;

import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.InputStream;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for executing Jython scripts in the Queue Server application.
 * Based on Phoebus display runtime JythonScriptSupport implementation.
 */
public class JythonScriptExecutor {

    private static final Logger logger = Logger.getLogger(JythonScriptExecutor.class.getPackageName());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "QueueServer-Jython");
        thread.setDaemon(true);
        return thread;
    });

    private final PythonInterpreter python;

    static {
        // Configure Jython options (similar to Phoebus display runtime)
        PySystemState.initialize();
        Options.dont_write_bytecode = true;

        // Set console encoding
        PySystemState sys = Py.getSystemState();
        sys.setdefaultencoding("utf-8");

        logger.log(Level.FINE, "Jython initialized for Queue Server");
    }

    /**
     * Create a new Jython script executor with a dedicated interpreter instance.
     */
    public JythonScriptExecutor() {
        // Synchronized to prevent concurrent initialization issues
        synchronized (JythonScriptExecutor.class) {
            this.python = new PythonInterpreter();
        }
    }

    /**
     * Execute a Python script with the given context variables.
     *
     * @param scriptContent The Python script code to execute
     * @param contextVars Variables to pass into the Python context (key -> value pairs)
     * @return The result object returned by the script (or null if none)
     */
    public Object execute(String scriptContent, java.util.Map<String, Object> contextVars) {
        try {
            // Set context variables in the Python interpreter
            if (contextVars != null) {
                for (java.util.Map.Entry<String, Object> entry : contextVars.entrySet()) {
                    python.set(entry.getKey(), entry.getValue());
                }
            }

            // Execute the script
            python.exec(scriptContent);

            // Try to get a result variable if one was set
            PyObject result = python.get("result");

            // Clear context to prevent memory leaks
            if (contextVars != null) {
                for (String key : contextVars.keySet()) {
                    python.set(key, null);
                }
            }

            // Convert PyObject to Java object
            return result != null ? result.__tojava__(Object.class) : null;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Jython script execution failed", e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a Python script asynchronously.
     *
     * @param scriptContent The Python script code to execute
     * @param contextVars Variables to pass into the Python context
     * @return Future that will contain the result
     */
    public Future<Object> executeAsync(String scriptContent, java.util.Map<String, Object> contextVars) {
        return EXECUTOR.submit(() -> execute(scriptContent, contextVars));
    }

    /**
     * Execute a Python script from a resource file.
     *
     * @param resourcePath Path to the Python script resource
     * @param contextVars Variables to pass into the Python context
     * @return The result object returned by the script
     */
    public Object executeResource(String resourcePath, java.util.Map<String, Object> contextVars) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            String scriptContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return execute(scriptContent, contextVars);
        }
    }

    /**
     * Execute a simple Python expression and return the result.
     *
     * @param expression Python expression to evaluate
     * @return The evaluated result
     */
    public Object eval(String expression) {
        try {
            PyObject result = python.eval(expression);
            return result.__tojava__(Object.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Jython eval failed for: " + expression, e);
            throw new RuntimeException("Evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Close this executor and release resources.
     */
    public void close() {
        if (python != null) {
            python.close();
        }
    }

    /**
     * Shutdown the shared executor service (call during application shutdown).
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
