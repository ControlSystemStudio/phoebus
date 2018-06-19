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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.model.json.JsonTags;
import org.phoebus.applications.alarm.model.print.ModelPrinter;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;

import com.fasterxml.jackson.databind.JsonNode;

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
                logger.info("Fetching past alarm states...");
                final AlarmStateInitializer init = new AlarmStateInitializer(server, config);
                if (! init.awaitCompleteStates())
                    logger.log(Level.WARNING, "Keep receiving state updates, may have incomplete initial set of alarm states");
                final ConcurrentHashMap<String, ClientState> initial_states = init.shutdown();

                logger.info("Start handling alarms");
                model = new ServerModel(server, config, initial_states, this);
                model.start();

                // Run until, via command topic, asked to
                // a) restart (restart given with value 'true')
                // b) shut down (restart given with value 'false')
                run = restart.take();

                model.shutdown();
            }
        }
        catch (final Throwable ex)
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
     *  <li>pvs /some/path -
     *      Prints PVs in subtree
     *  <li>pv name_of_PV -
     *      Prints that PV
     *  <li>disconnected -
     *      Prints all disconnected PVs
     *  <li>restart -
     *      Re-load configuration
     *  <li>shutdown -
     *      Quit
     *  </ul>
     *
     *  TODO Alarm server console?
     *  At this time, the alarm server has no console/shell/terminal interface.
     *  It can only receive commands from a "..Command" topic,
     *  and then print the result on the console.
     *  Ideally, command and reply could be seen in a terminal.
     *
     *  @param command Command received from the client
     *  @param detail Detail for the command, usually path to alarm tree node
     */
    @Override
    public void handleCommand(final String path, final String json)
    {
        try
        {
            JsonNode jsonNode = (JsonNode) JsonModelReader.parseCommand(json);
            JsonNode commandNode = jsonNode.get(JsonTags.COMMAND);
            if (null == commandNode)
            {
                throw new Exception("Command parsing failed.");
            }
            
            final String command = commandNode.asText();
            if (command.startsWith("ack"))
            {
                final AlarmTreeItem<?> node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                acknowledge(node, true);
            }
            else if (command.startsWith("unack"))
            {
                final AlarmTreeItem<?> node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                acknowledge(node, false);
            }
            else if (command.equalsIgnoreCase("dump"))
            {
                final AlarmTreeItem<?> node;
                node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                System.out.println(node.getPathName() + ":");
                ModelPrinter.print(node);
            }
            else if (command.equalsIgnoreCase("pvs"))
            {
                final AlarmTreeItem<?> node;
                node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                System.out.println("PVs for " + node.getPathName() + ":");
                listPVs(node, false);
            }
            else if (command.equalsIgnoreCase("disconnected"))
            {
                final AlarmTreeItem<?> node;
                node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                System.out.println("PVs for " + node.getPathName() + ":");
                listPVs(node, true);
            }
            else if (command.equalsIgnoreCase("pv"))
            {
                final AlarmServerPV pv = model.findPV(path);
                if (pv == null)
                    throw new Exception("Unknown PV '" + path + "'");
                listPVs(pv, false);
            }
            else if (command.equalsIgnoreCase("shutdown"))
            {
                restart.offer(false);
            }
            else if (command.equalsIgnoreCase("restart"))
            {
                logger.log(Level.INFO, "Restart requested");
                restart.offer(true);
            }
            else
                throw new Exception("Unknown command.");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error for command. path: '" + path + "', JSON: '" + json + "'", ex);
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
            for (final AlarmTreeItem<?> child : node.getChildren())
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
            for (final AlarmTreeItem<?> child : node.getChildren())
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
        System.out.println("-create_topics           - Create Kafka topics for alarm configuration?");
        System.out.println("-settings settings.xml   - Import preferences (PV connectivity) from property format file");
        System.out.println("-export   config.xml     - Export alarm configuration to file");
        System.out.println("-import   config.xml     - Import alarm configruation from file");
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
                else if (cmd.equals("-create_topics"))
                {
                    iter.remove();
                    logger.info("Discovering and creating any missing topics at " + server);
                    CreateTopics.discoverAndCreateTopics(server, config);
                }
                else if (cmd.equals("-import"))
                {
                	if (! iter.hasNext())
                		throw new Exception("Missing -import file name");
                	iter.remove();
                	final String filename = iter.next();
                	iter.remove();
                	logger.info("Import model from " + filename);
                	new AlarmConfigTool().importModel(filename, server, config);
                	return;
                }
                else if (cmd.equals("-export"))
                {
                	if (! iter.hasNext())
                		throw new Exception("Missing -export file name");
                	iter.remove();
                	final String filename = iter.next();
                	iter.remove();
                	logger.info("Exporting model to " + filename);
                	new AlarmConfigTool().exportModel(filename, server, config);
                	return;
                }
                else
                    throw new Exception("Unknown option " + cmd);
            }
        }
        catch (final Exception ex)
        {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }


        new AlarmServerMain(server, config);
    }
}
