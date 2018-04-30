/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.print.ModelPrinter;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;

/** Alarm Server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmServerMain implements ServerModelListener
{
    private final SynchronousQueue<Boolean> restart = new SynchronousQueue<>();

    private volatile ServerModel model;

    private AlarmServerMain(final String server, final String config)
    {
        logger.info("Server: " + server);
        logger.info("Config: " + config);

        try
        {
            // 'main' loop that keeps performing a full startup and shutdown
            // whenever a 'restart' is requested.
            boolean run = true;
            while (run)
            {
                model = new ServerModel(server, config, this);
                model.start();

                // Run until, via command topic, asked to
                // a) restart (restart given with value 'true')
                // b) shut down (restart given with value 'false')
                run = restart.take();

                model.shutdown();
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.SEVERE, "Alarm Server main loop error", ex);
        }

        logger.info("Done.");
        System.exit(0);

    }

    /** Handle commands
     *
     *  <ul>
     *  <li>dump -
     *      Dumps complete alarm tree
     *  <li>dump some/path -
     *      Dumps subtree
     *  <li>pvs -
     *      Prints all PVs
     *  <li>pvs disconnected -
     *      Prints all disconnected PVs
     *  <li>pvs /some/path -
     *      Prints PVs in subtree
     *  <li>restart -
     *      Re-load configuration
     *  <li>shutdown -
     *      Quit
     *  </ul>
     *
     *  TODO Alarm server console?
     *  At this time, the alarm server has no console/shell/terminal interface.
     *  It can only receive alarms from a "..Command" topic,
     *  and then prints the result on the console.
     *  Ideally, command and reply could be seen in the same terminal.
     *
     *  @param command Command received from the client
     *  @param detail Detail for the command, usually path to alarm tree node
     */
    @Override
    public void handleCommand(final String command, final String detail)
    {
        try
        {
            if (command.startsWith("ack"))
            {
                final AlarmTreeItem<?> node = model.findNode(detail);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + detail + "'");
                acknowledge(node, true);
            }
            else if (command.startsWith("unack"))
            {
                final AlarmTreeItem<?> node = model.findNode(detail);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + detail + "'");
                acknowledge(node, false);
            }
            else if (command.equalsIgnoreCase("dump"))
            {
                final AlarmTreeItem<?> node;
                if (detail.isEmpty())
                    node = model.getRoot();
                else
                    node = model.findNode(detail);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + detail + "'");
                System.out.println(node.getPathName() + ":");
                ModelPrinter.print(node);
            }
            else if (command.equalsIgnoreCase("pvs"))
            {
                final AlarmTreeItem<?> node;
                if (detail.startsWith("/"))
                    node = model.findNode(detail);
                else
                    node = model.getRoot();
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + detail + "'");
                System.out.println("PVs for " + node.getPathName() + ":");
                listPVs(node, detail != null && detail.startsWith("dis"));
            }
            else if (command.equalsIgnoreCase("shutdown"))
                restart.offer(false);
            else if (command.equalsIgnoreCase("restart"))
            {
                logger.log(Level.INFO, "Restart requested");
                restart.offer(true);
            }
            else
                throw new Exception("Unknown command");
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Error for command: '" + command + "', detail '" + detail + "'", ex);
        }
    }

    private void acknowledge(final AlarmTreeItem<?> node, final boolean acknowledge)
    {
        if (node instanceof AlarmServerPV)
        {
            final AlarmServerPV pv_node = (AlarmServerPV) node;
            pv_node.acknowledge(acknowledge);
        }
        else
            for (AlarmTreeItem<?> child : node.getChildren())
                acknowledge(child, acknowledge);
    }

    private void listPVs(final AlarmTreeItem<?> node, final boolean disconnected_only)
    {
        if (node instanceof AlarmServerPV)
        {
            final AlarmServerPV pv_node = (AlarmServerPV) node;
            if (disconnected_only  && pv_node.isConnected())
                return;
            System.out.println(pv_node);
        }
        else
            for (AlarmTreeItem<?> child : node.getChildren())
                listPVs(child, disconnected_only);
    }

    private static void help()
    {
        // http://patorjk.com/software/taag/#p=display&f=Epic&t=Alarm%20Server
        System.out.println(" _______  _        _______  _______  _______    _______  _______  _______           _______  _______");
        System.out.println("(  ___  )( \\      (  ___  )(  ____ )(       )  (  ____ \\(  ____ \\(  ____ )|\\     /|(  ____ \\(  ____ )");
        System.out.println("| (   ) || (      | (   ) || (    )|| () () |  | (    \\/| (    \\/| (    )|| )   ( || (    \\/| (    )|");
        System.out.println("| (___) || |      | (___) || (____)|| || || |  | (_____ | (__    | (____)|| |   | || (__    | (____)|");
        System.out.println("|  ___  || |      |  ___  ||     __)| |(_)| |  (_____  )|  __)   |     __)( (   ) )|  __)   |     __)");
        System.out.println("| (   ) || |      | (   ) || (\\ (   | |   | |        ) || (      | (\\ (    \\ \\_/ / | (      | (\\ (   ");
        System.out.println("| )   ( || (____/\\| )   ( || ) \\ \\__| )   ( |  /\\____) || (____/\\| ) \\ \\__  \\   /  | (____/\\| ) \\ \\__");
        System.out.println("|/     \\|(_______/|/     \\||/   \\__/|/     \\|  \\_______)(_______/|/   \\__/   \\_/   (_______/|/   \\__/        ");
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                    - This text");
        System.out.println("-server   localhost:9092 - Kafka server");
        System.out.println("-config   Accelerator    - Alarm configuration");
        System.out.println("-settings settings.xml   - Import preferences (PV connectivity) from property format file");
        System.out.println();
    }


    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(AlarmServerMain.class.getResourceAsStream("/logging.properties"));

        logger.info("Alarm Server (PID " + ProcessHandle.current().pid() + ")");

        String server = "localhost:9092";
        String config = "Accelerator";

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
                else if (cmd.equals("-server"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -server name");
                    iter.remove();
                    server = iter.next();
                    iter.remove();
                }
                else if (cmd.equals("-config"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -config name");
                    iter.remove();
                    config = iter.next();
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

        new AlarmServerMain(server, config);
    }
}
