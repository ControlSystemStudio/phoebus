/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.csstudio.scan.server.httpd.ScanWebServer;
import org.csstudio.scan.server.internal.ScanServerImpl;

/** Main routine for the Scan Server application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerMain
{
    public static final Logger logger = Logger.getLogger(ScanServerMain.class.getPackageName());

    /** Default port used by scan server's REST interface */
    public static final int DEFAULT_PORT = 4810;

    private static final CountDownLatch done = new CountDownLatch(1);

    public static final String VERSION = "4.5.0";

    private static ScanServerImpl scan_server;

    public static ScanServer getScanServer()
    {
        return scan_server;
    }

    public static void stop()
    {
        done.countDown();
    }

    public static void main(final String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(ScanServerMain.class.getResourceAsStream("/logging.properties"));

        logger.info("Scan Server (PID " + ProcessHandle.current().pid() + ")");
        int port = DEFAULT_PORT;

        scan_server = new ScanServerImpl();

        scan_server.start();

        final ScanWebServer httpd = new ScanWebServer(port);
        try
        {
            httpd.start();

            logger.info("Scan Server REST interface on http://localhost:" + port + "/index.html");

            // Main thread could do other things while web server is running...
            done.await();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot start", ex);
        }
        httpd.shutdown();
        logger.info("Done.");
    }
}
