/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.csstudio.scan.server.config.ScanConfig;
import org.csstudio.scan.server.httpd.ScanWebServer;
import org.csstudio.scan.server.internal.ScanServerImpl;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;

/** Main Instance of the Scan Server application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerInstance
{
    public static final Logger logger = Logger.getLogger(ScanServerInstance.class.getPackageName());

    private static final CountDownLatch done = new CountDownLatch(1);

    public static final String VERSION = "4.5.0";

    private static URL scan_config_file = ScanServerInstance.class.getResource("/examples/scan_config.xml");

    private static ScanConfig scan_config;

    private static ScanServerImpl scan_server;

    public static String getScanConfigPath()
    {
        return scan_config_file.toExternalForm();
    }

    public static ScanConfig getScanConfig()
    {
        return scan_config;
    }

    public static ScanServer getScanServer()
    {
        return scan_server;
    }

    public static void stop()
    {
        done.countDown();
    }

    private static void help()
    {
        // http://patorjk.com/software/taag/#p=display&f=Epic&t=Scan%20Server
        System.out.println(" _______  _______  _______  _          _______  _______  _______           _______  _______ ");
        System.out.println("(  ____ \\(  ____ \\(  ___  )( (    /|  (  ____ \\(  ____ \\(  ____ )|\\     /|(  ____ \\(  ____ )");
        System.out.println("| (    \\/| (    \\/| (   ) ||  \\  ( |  | (    \\/| (    \\/| (    )|| )   ( || (    \\/| (    )|");
        System.out.println("| (_____ | |      | (___) ||   \\ | |  | (_____ | (__    | (____)|| |   | || (__    | (____)|");
        System.out.println("(_____  )| |      |  ___  || (\\ \\) |  (_____  )|  __)   |     __)( (   ) )|  __)   |     __)");
        System.out.println("      ) || |      | (   ) || | \\   |        ) || (      | (\\ (    \\ \\_/ / | (      | (\\ (   ");
        System.out.println("/\\____) || (____/\\| )   ( || )  \\  |  /\\____) || (____/\\| ) \\ \\__  \\   /  | (____/\\| ) \\ \\__");
        System.out.println("\\_______)(_______/|/     \\||/    )_)  \\_______)(_______/|/   \\__/   \\_/   (_______/|/   \\__/");
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                    - This text");
        System.out.println("-config scan_config.xml  - Scan config (REST port, jython paths, simulation settings");
        System.out.println("-settings settings.xml   - Import preferences (PV connectivity) from property format file");
        System.out.println();
    }

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(ScanServerInstance.class.getResourceAsStream("/logging.properties"));

        logger.info("Scan Server (PID " + ProcessHandle.current().pid() + ")");

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try
        {
            while (iter.hasNext())
            {
                final String cmd = iter.next();
                if (cmd.startsWith("-h"))
                {
                    help();
                    return;
                }
                else if (cmd.equals("-config"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -config file name");
                    iter.remove();
                    scan_config_file = new File(iter.next()).toURI().toURL();
                    iter.remove();
                }
                else if (cmd.equals("-settings"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -settings file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    logger.info("Loading settings from " + filename);
                    PropertyPreferenceLoader.load(new FileInputStream(filename));
                }
                else
                    throw new Exception("Unknown option " + cmd);
            }
        }
        catch (Exception ex)
        {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }

        logger.info("Configuration: " + scan_config_file);
        scan_config = new ScanConfig(scan_config_file.openStream());

        scan_server = new ScanServerImpl();
        scan_server.start();

        logger.info("Scan Server REST interface on http://localhost:" + scan_config.getPort() + "/index.html");
        final ScanWebServer httpd = new ScanWebServer(scan_config.getPort());
        try
        {
            httpd.start();

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
