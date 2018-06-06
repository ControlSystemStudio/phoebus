/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.framework.jobs.LogWriter;

import py4j.GatewayServer;

/** Provides a gateway through which to run Python scripts. Java objects are made
 *  accessible to Python through the gateway using a map.
 *
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class PythonGatewaySupport
{
    /** Check if the local python setup can import 'connect2j'
     *  @return <code>true</code> if connect2j is installed
     */
    public static boolean isConnect2jInstalled()
    {
        try
        {
            final Process process = new ProcessBuilder("python", "-c", "import connect2j").start();
            final Thread stdout = new LogWriter(process.getInputStream(), "Python", Level.INFO);
            final Thread stderr = new LogWriter(process.getErrorStream(), "Python", Level.WARNING);
            stdout.start();
            stderr.start();
            process.waitFor();
            stderr.join();
            stdout.join();
            return process.exitValue() == 0;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Python error", ex);
        }
        return false;
    }

    /**
     * Run a Python script, with a map of Java objects made accessible through a
     * gateway to Python. The port to which the gateway server is listening is
     * passed as an argument to the script.
     *
     * @param map Map which is to be accessed by the script
     * @param script Path (including name) of script which is to be run
     * @throws Exception If unable to execute command to run script
     */
    public static void run(Map<String, Object> map, String script) throws Exception
    {
        GatewayServer server = new GatewayServer(new MapWrapper(map), 0);
        server.start();
        int port = server.getListeningPort();
        if (port == -1)
        {
            server.shutdown();
            throw new Exception("Exception instantiating PythonGatewaySupport: GatewayServer not listening");
        }

        // start Python process, passing port used to connect to Py4J Java Gateway
        final Process process = new ProcessBuilder("python", script, Integer.toString(port)).start();
        final Thread error_log = new LogWriter(process.getErrorStream(), "PythonErrors", Level.WARNING);
        final Thread python_out = new LogWriter(process.getInputStream(), "PythonOutput", Level.INFO);
        error_log.start();
        python_out.start();

        try
        {
            process.waitFor();
        }
        catch (InterruptedException ex)
        {
            // ignore; closing display creates interruption
        }
        finally
        {
            process.destroyForcibly();
        }

        error_log.join();
        python_out.join();
        server.shutdown();
    }

    /** Wrapper class which allows access to map for PythonGatewaySupport
     *
     *  'unused' methods are accessed by python script via gateway
     */
    @SuppressWarnings("unused")
    private static class MapWrapper
    {
        private Map<String, Object> map;
        public MapWrapper()
        {
            this.map = Collections.emptyMap();
        }
        public MapWrapper(Map<String, Object> map)
        {
            this.map = map;
        }
        public Map<String, Object> getMap()
        {
            return map;
        }
        public void setMap(Map<String, Object> map)
        {
            this.map = map;
        }
    }
}