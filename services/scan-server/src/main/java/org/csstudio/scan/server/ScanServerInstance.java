/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
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

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.server.config.ScanConfig;
import org.csstudio.scan.server.httpd.ScanWebServer;
import org.csstudio.scan.server.internal.ScanServerImpl;
import org.csstudio.scan.server.log.DataLogFactory;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.util.shell.CommandShell;

/** Main Instance of the Scan Server application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerInstance
{
    public static final Logger logger = Logger.getLogger(ScanServerInstance.class.getPackageName());

    private static final CountDownLatch done = new CountDownLatch(1);

    public static final String VERSION = "4.6.0";

    private static URL scan_config_file = ScanServerInstance.class.getResource("/examples/scan_config.xml");

    private static ScanConfig scan_config;

    private static ScanServerImpl scan_server;

    public static URL getScanConfigURL()
    {
        return scan_config_file;
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
        System.out.println("-help                       - This text");
        System.out.println("-config scan_config.xml     - Scan config (REST port, jython paths, simulation settings");
        System.out.println("-settings settings.xml      - Import preferences (PV connectivity) from property format file");
        System.out.println("-logging logging.properties - Load log settings");
        System.out.println("-noshell                    - Disable the command shell for running without a terminal");
        System.out.println();
    }

    private static final String COMMANDS =
        "Scan Server Commands:\n" +
        "help            -  Show commands\n" +
        "server          -  Server info\n" +
        "scans           -  List scans\n" +
        "info ID         -  Show info about scan with given ID\n" +
        "commands ID     -  Dump scan's commands\n" +
        "devices ID      -  Show devices used by scan\n" +
        "data ID         -  Dump log data for scan\n" +
        "abort ID        -  Abort given scan\n" +
        "abort           -  Abort all scans\n" +
        "remove ID       -  Remove (completed) scan with given ID\n" +
        "removeCompleted -  Remove completed scans\n" +
        "gc              -  Run GC\n" +
        "shutdown        -  Stop the scan server";


    private static boolean handleShellCommands(final String... args) throws Throwable
    {
        if (args == null)
            stop();
        else if (args.length == 1)
        {
            if (args[0].startsWith("shut"))
                stop();
            else if (args[0].equals("server"))
            {
                final ScanServerInfo info = getScanServer().getInfo();
                System.out.println(info);
            }
            else if (args[0].equals("scans"))
            {
                System.out.println("Scans:");
                // List latest last so that console will show the 'current' one,
                // while older ones scroll up.
                final List<ScanInfo> scans = getScanServer().getScanInfos();
                for (int i=scans.size()-1; i>=0; --i)
                    System.out.println(scans.get(i));
            }
            else if (args[0].equals("abort"))
                getScanServer().abort(-1);
            else if (args[0].equals("removeCompleted"))
                getScanServer().removeCompletedScans();
            else if (args[0].equals("gc"))
                Runtime.getRuntime().gc();
            else
                return false;
        }
        else if (args.length == 2)
        {
            final int id = Integer.parseInt(args[1]);
            if (args[0].startsWith("com"))
            {
                System.out.println("Commands of scan " + id + ":");
                final String commands = getScanServer().getScanCommands(id);
                System.out.println(commands);
            }
            else if (args[0].startsWith("dev"))
            {
                System.out.println("Devices of scan " + id + ":");
                for (DeviceInfo dev : getScanServer().getDeviceInfos(id))
                    System.out.println(dev);
            }
            else if (args[0].equals("info"))
                System.out.println(getScanServer().getScanInfo(id));
            else if (args[0].equals("data"))
            {
                // Dump data
                final ScanData data = getScanServer().getScanData(id);
                final ScanDataIterator sheet = new ScanDataIterator(data);

                // Header: Device names
                for (String device : sheet.getDevices())
                    System.out.print(device + "  ");
                System.out.println();
                // Rows
                while (sheet.hasNext())
                {
                    final ScanSample[] line = sheet.getSamples();
                    for (ScanSample sample : line)
                        System.out.print(sample + "  ");
                    System.out.println();
                }
            }
            else if (args[0].equals("abort"))
                getScanServer().abort(id);
            else if (args[0].equals("remove"))
                getScanServer().remove(id);
            else
                return false;
        }
        else
            return false;
        return true;
    }

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(ScanServerInstance.class.getResourceAsStream("/scan_server_logging.properties"));

        // Handle arguments
        boolean use_shell = true;
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
                else if (cmd.equals("-logging"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -logging file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    logger.info("Loading log settings from " + filename);
                    LogManager.getLogManager().readConfiguration(new FileInputStream(filename));
                }
                else if (cmd.equals("-noshell"))
                {
                    use_shell = false;
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

        logger.info("Scan Server (PID " + ProcessHandle.current().pid() + ")");

        logger.info("Configuration: " + scan_config_file);
        scan_config = new ScanConfig(scan_config_file.openStream());

        DataLogFactory.startup(scan_config.getDataLogParm());

        scan_server = new ScanServerImpl();
        scan_server.start();

        logger.info("Scan Server REST interface on http://localhost:" + scan_config.getPort() + "/index.html");
        final ScanWebServer httpd = new ScanWebServer(scan_config.getPort());
        try
        {
            httpd.start();

            final CommandShell shell;
            if (use_shell)
            {
                shell = new CommandShell(COMMANDS, ScanServerInstance::handleShellCommands);
                shell.start();
            }
            else
                shell = null;

            // Main thread could do other things while web server is running...
            done.await();

            if (shell != null)
                shell.stop();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot start", ex);
        }
        httpd.shutdown();

        DataLogFactory.shutdown();

        logger.info("Done.");
        System.exit(0);
    }
}
