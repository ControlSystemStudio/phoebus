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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.prefs.Preferences;


import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.model.json.JsonTags;
import org.phoebus.applications.alarm.model.print.ModelPrinter;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.util.shell.CommandShell;

import com.fasterxml.jackson.databind.JsonNode;

/** Alarm Server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmServerMain implements ServerModelListener
{
    private final SynchronousQueue<Boolean> restart = new SynchronousQueue<>();

    private volatile ServerModel  model;
    private volatile CommandShell shell;
    private String current_path = "";

    private static final String COMMANDS =
                        "Commands:\n\n" +
                        "Note: '.' and '..' will be interpreted as the current directory and the parent directory respectively.\n" +
                        "Spaces within a path do not need to be quoted.\n\n" +
                        "\tls               - List all alarm tree items in the current directory.\n" +
                        "\tls -disconnected - List all the disconnected PVs in the entire alarm tree.\n" +
                        "\tls -disabled     - List all the disabled PVs in the entire alarm tree.\n" +
                        "\tls -all          - List all alarm tree PVs in the entire alarm tree.\n" +
                        "\tls -active       - .. which are in active alarm.\n" +
                        "\tls -alarm        - .. alarm, active or acknowledged.\n" +
                        "\tls dir           - List all alarm tree items in the specified directory contained in the current directory.\n" +
                        "\tls /path/to/dir  - List all alarm tree items in the specified directory at the specified path.\n" +
                        "\tcd               - Change to the root directory.\n" +
                        "\tcd dir           - Change to the specified directory contained in the current directory.\n" +
                        "\tcd /path/to/dir  - Change to the specified directory at the specified path.\n" +
                        "\tpv pv            - Print the specified PV in the current directory.\n" +
                        "\tpv /path/to/pv   - Print the specified PV at the specified path.\n" +
                        "\tmode             - Show mode.\n" +
                        "\tmode normal      - Select normal mode.\n" +
                        "\tmode maintenance - Select maintenance mode.\n" +
                        "\trestart          - Re-load alarm configuration and restart.\n" +
                        "\tshutdown         - Shut alarm server down and exit.\n";

    private AlarmServerMain(final String server, final String config, final boolean use_shell)
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

                if (use_shell)
                {
                    shell = new CommandShell(COMMANDS, this::handleShellCommands);

                    // Start the command shell at the root node.
                    current_path = model.getRoot().getPathName();
                    shell.setPrompt(current_path);
                    shell.start();
                }
                // Run until, via command topic or shell input, asked to
                // a) restart (restart given with value 'true')
                // b) shut down (restart given with value 'false')
                run = restart.take();
                if (run)
                    logger.info("Restarting...");
                else
                    logger.info("Shutting down");

                if (use_shell)
                    shell.stop();

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

    /**
     * Handle shell commands. Passed to command shell.
     * @param args - variadic String
     * @return result - boolean result of executing the command.
     * @throws Throwable
     */
    private boolean handleShellCommands(final String... args) throws Throwable
    {
        if (args == null)
            restart.offer(false);
        else if (args.length == 1)
        {
            if (args[0].startsWith("shut"))
                restart.offer(false);
            else if (args[0].equals("restart"))
                restart.offer(true);
            else if (args[0].equals("mode"))
                System.out.println(AlarmLogic.getMaintenanceMode() ? "Maintenance mode" : "Normal mode");
            else if (args[0].startsWith("h"))
                // Return false will print the commands message.
                return false;
            else if (args[0].equals("cd")) // cd with no argument goes to root directory.
            {
                current_path = model.getRoot().getPathName();
                shell.setPrompt(current_path);
            }
            else if (args[0].equals("ls")) // List alarm tree items in current directory. _Not_ recursive descent.
            {
                List<AlarmTreeItem<?>> children = model.findNode(current_path).getChildren();
                for (final AlarmTreeItem<?> child : children)
                    System.out.println(child.getName() + " - " + child.getState());
            }
            else
                return false;
        }
        else if (args.length >= 2)
        {
            // Concatenate all the tokens whose index is > 0 into a single string.
            // This allows for spaces in PV and Node names. They would have been split on whitespace by the CommandShell.
            String args1 = "";
            for (int i = 1; i < args.length; i++)
                args1 += " " + args[i];
            args1 = args1.trim();

            try
            {
                if (args[0].equals("cd")) // Change directory to specified location.
                {
                    final String new_path = determinePath(args1);
                    final AlarmTreeItem<?> new_loc = model.findNode(new_path);

                    if (null == new_loc)
                    {
                        System.out.println("Node not found: " + new_path);
                        return false;
                    }

                    // Can't change location to leaves.
                    if (new_loc instanceof AlarmTreeLeaf)
                    {
                        System.out.println("Node not a directory: " + new_loc.getPathName());
                        return false;
                    }

                    current_path = new_loc.getPathName();
                    shell.setPrompt(current_path);
                }
                else if (args[0].equals("ls"))  // List the alarm tree items at the specified location.
                {
                    if (args1.startsWith("-disc")) // Print all disconnected PVs in tree.
                        listPVs(model.getRoot(), PVMode.Disconnected);
                    else if (args1.startsWith("-disa")) // Print all disconnected PVs in tree.
                        listPVs(model.getRoot(), PVMode.Disabled);
                    else if (args1.equals("-all")) // Print all the PVs in the tree.
                        listPVs(model.getRoot(), PVMode.All);
                    else if (args1.startsWith("-act")) // Print all the PVs in the tree that are in active alarm
                        listPVs(model.getRoot(), PVMode.InActiveAlarm);
                    else if (args1.startsWith("-ala")) // Print all the PVs in the tree that are in alarm
                        listPVs(model.getRoot(), PVMode.InAlarm);
                    else // List the PVs at the specified path.
                    {
                        final String path = determinePath(args1);
                        final AlarmTreeItem<?> node = model.findNode(path);

                        if (null == node)
                        {
                            System.out.println("Node not found: " + path);
                            return false;
                        }

                        final List<AlarmTreeItem<?>> children = node.getChildren();
                        for (final AlarmTreeItem<?> child : children)
                            System.out.println(child.getName() + " - " + child.getState());
                    }
                }
                else if (args[0].equals("pv")) // Print the specified PV.
                {
                    final String pvPath = determinePath(args1);
                    final AlarmTreeItem<?> node = model.findNode(pvPath);
                    if (node instanceof AlarmServerNode)
                    {
                        System.out.println("Specified alarm tree item is not a PV: " + pvPath);
                        return false;
                    }
                    final AlarmServerPV pv = (AlarmServerPV) node;
                    System.out.println(pv);
                }
                else if (args[0].equals("mode"))
                {
                    setMaintenanceMode(args1.startsWith("maint"));
                    System.out.println(AlarmLogic.getMaintenanceMode() ? "Maintenance mode" : "Normal mode");
                }
            } // Catch the exceptions caused by findNode searching a path that doesn't start with the root directory.
            catch (Exception ex)
            {
                System.out.println(ex.getMessage());
                return false;
            }
        }
        else
            return false;

        return true;
    }

    /**
     * Determines the new path based on the passed string.
     * <p> The passed string is expected to be ".", "..", a canonical path, a path from the current directory, or a child of the current directory.
     * <ol>
     * <li> .           -> current directory.
     * <li> ..          -> parent directory.
     * <li> /root/path/ -> /root/path/
     * <li> /dir        -> current_path/dir
     * <li> dir         -> current_path/dir
     * </ol>
     * @param arg - String to be examined.
     * @return new_path
     * @throws Exception
     */
    private String determinePath(final String arg) throws Exception
    {
        String new_path = current_path;
        if (arg.equals(".")) // Current directory.
        {
            return new_path;
        }
        else if (arg.equals("..")) // Parent directory.
        {
            AlarmTreeItem<?> parent = model.findNode(current_path).getParent();
            if (null != parent)
                new_path = parent.getPathName();
        }
        else if (arg.startsWith(model.getRoot().getPathName())) // If starts from root, treat it as a whole path.
        {
            new_path = arg;
        }
        else if (arg.startsWith("/")) // Allow for "command /dir".
        {
            new_path = current_path + arg;
        }
        else // Allow for "command dir".
        {
            new_path = current_path + "/" + arg;
        }

        // Replace "//" in simulated PVs with "\/\/"
        new_path = new_path.replaceAll("//", "\\\\/\\\\/");

        return new_path;
    }

    /** Handle commands
     *
     *  <ul>
     *  <li>acknowledge /some/path -
     *      Acknowledge alarms in subtree
     *  <li>unacknowledge /some/path -
     *      Un-Acknowledge alarms in subtree
     *  <li>mode [normal|maintenance] -
     *      Select normal or maintenance mode
     *  <li>dump -
     *      Dumps complete alarm tree
     *  <li>dump /some/path -
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
     *  @param path Alarm tree path
     *  @param json Command
     */
    @Override
    public void handleCommand(final String path, final String json)
    {
        try
        {
            final JsonNode jsonNode = (JsonNode) JsonModelReader.parseJsonText(json);
            final JsonNode commandNode = jsonNode.get(JsonTags.COMMAND);
            if (null == commandNode)
                throw new Exception("Command parsing failed.");

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
            else if (JsonTags.MAINTENANCE.equals(command))
                setMaintenanceMode(true);
            else if (JsonTags.NORMAL.equals(command))
                setMaintenanceMode(false);
            else if (JsonTags.DISABLE_NOTIFY.equals(command))
                setDisableNotify(true);
            else if (JsonTags.ENABLE_NOTIFY.equals(command))
                setDisableNotify(false);
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
                listPVs(node, PVMode.All);
            }
            else if (command.equalsIgnoreCase("disconnected"))
            {
                final AlarmTreeItem<?> node;
                node = model.findNode(path);
                if (node == null)
                    throw new Exception("Unknown alarm tree node '" + path + "'");
                System.out.println("PVs for " + node.getPathName() + ":");
                listPVs(node, PVMode.Disconnected);
            }
            else if (command.equalsIgnoreCase("pv"))
            {
                final AlarmServerPV pv = model.findPV(path);
                if (pv == null)
                    throw new Exception("Unknown PV '" + path + "'");
                listPVs(pv, PVMode.All);
            }
            else if (command.equals("shutdown"))
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

    private void setDisableNotify(final boolean disable_notify)
    {
        // Any change?
        if (disable_notify == AlarmLogic.getDisableNotify())
            return;
        // Configure alarm logic
        AlarmLogic.setDisableNotify(disable_notify);

        // Force state update of root to publish the updated server mode
        model.sendStateUpdate(model.getRoot().getPathName(), model.getRoot().getState());
    }

    private void setMaintenanceMode(final boolean maintenance_mode)
    {
        // Any change?
        if (maintenance_mode == AlarmLogic.getMaintenanceMode())
            return;
        // Configure alarm logic
        AlarmLogic.setMaintenanceMode(maintenance_mode);
        // Entering maintenance mode: Ack' all INVALID alarms
        if (maintenance_mode)
            acknowledgeInvalidUndefined(model.getRoot());

        // Force state update of root to publish the updated server mode
        model.sendStateUpdate(model.getRoot().getPathName(), model.getRoot().getState());
    }

    /** @param node Node where to start ack'ing all INVALID or UNDEFINED alarms */
    private void acknowledgeInvalidUndefined(final AlarmTreeItem<?> node)
    {
        if (node instanceof AlarmServerPV)
        {
            final AlarmServerPV pv_node = (AlarmServerPV) node;
            if (pv_node.getState().severity.ordinal() >= SeverityLevel.INVALID.ordinal())
                pv_node.acknowledge(true);
        }
        else
            for (final AlarmTreeItem<?> child : node.getChildren())
                acknowledgeInvalidUndefined(child);
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

    enum PVMode
    {
        All,
        InActiveAlarm,
        InAlarm,
        Disconnected,
        Disabled
    };

    private void listPVs(final AlarmTreeItem<?> node, final PVMode which)
    {
        listPVs(new AtomicInteger(), node, which);
    }

    private void listPVs(final AtomicInteger count, final AlarmTreeItem<?> node, final PVMode which)
    {
        if (node instanceof AlarmServerPV)
        {
            final AlarmServerPV pv_node = (AlarmServerPV) node;
            switch (which)
            {
            case Disabled:
                if (pv_node.isEnabled())
                    return;
                break;
            case Disconnected:
                if (!pv_node.isEnabled()  ||  pv_node.isConnected())
                    return;
                break;
            case InActiveAlarm:
                if (!pv_node.isEnabled()  ||  !pv_node.getState().severity.isActive())
                    return;
                break;
            case InAlarm:
                if (!pv_node.isEnabled()  ||  pv_node.getState().severity == SeverityLevel.OK)
                    return;
                break;
            default:
                break;
            }
            System.out.format("%3d : ", count.incrementAndGet());
            System.out.println(pv_node);
        }
        else
            for (final AlarmTreeItem<?> child : node.getChildren())
                listPVs(count, child, which);
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
        System.out.println("-help                          - This text");
        System.out.println("-server    localhost:9092      - Kafka server with port number");
        System.out.println("-config    Accelerator         - Alarm configuration");
        // Don't mention this option, prefer examples/create_topics.sh
        // System.out.println("-create_topics              - Create Kafka topics for alarm configuration?");
        System.out.println("-settings  settings.{xml,ini}  - Import preferences (PV connectivity) from property format file");
        System.out.println("-noshell                       - Disable the command shell for running without a terminal");
        System.out.println("-export    config.xml          - Export alarm configuration to file");
        System.out.println("-import    config.xml          - Import alarm configruation from file");
        System.out.println("-logging   logging.properties  - Load log settings");
        System.out.println();
    }


    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(AlarmServerMain.class.getResourceAsStream("/alarm_server_logging.properties"));

        String server = "localhost:9092";
        String config = "Accelerator";
        boolean use_shell = true;
        String  args_server = "";
        String  args_config = "";
        boolean use_args_server = false;
        boolean use_args_config = false;
        boolean use_settings = false;

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try
        {
            while (iter.hasNext())
            {
                final String cmd = iter.next();
                if ( cmd.equals("-h") || cmd.equals("-help"))
                {
                    help();
                    return;
                }
                else if (cmd.equals("-server"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -server name");
                    iter.remove();
                    args_server = iter.next();
                    use_args_server = true;
                    iter.remove();
                }
                else if (cmd.equals("-config"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -config name");
                    iter.remove();
                    args_config = iter.next();
                    use_args_config = true;
                    iter.remove();
                }
                else if (cmd.equals("-logging"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -logging file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    LogManager.getLogManager().readConfiguration(new FileInputStream(filename));
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

                    Preferences userPrefs  = Preferences.userRoot().node("org/phoebus/applications/alarm");
                    String pref_server     = userPrefs.get("server", server);
                    String pref_conf_names = userPrefs.get("config_names", config);
                    server = pref_server;
                    config = pref_conf_names;
                    use_settings = true;
                }
                else if (cmd.equals("-noshell"))
                {
                    use_shell = false;
                }
                else if (cmd.equals("-create_topics"))
                {
                    iter.remove();
                    logger.info("Discovering and creating any missing topics at " + server);
                    CreateTopics.discoverAndCreateTopics(server, true, List.of(config,
                                                                               config + AlarmSystem.COMMAND_TOPIC_SUFFIX,
                                                                               config + AlarmSystem.TALK_TOPIC_SUFFIX));
                }
                else if (cmd.equals("-import"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -import file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    logger.info("Import model from " + filename);
                    new AlarmConfigTool().importModel(filename, server, use_args_config ? args_config : config);
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
                    new AlarmConfigTool().exportModel(filename, server, use_args_config ? args_config : config);
                    return;
                }
                else
                    throw new Exception("Unknown option " + cmd);
            }

            if ( use_args_server ) {
                if ( use_settings ) {
                    logger.log(Level.WARNING,"Found the conflicted configurations : -settings/server:" + server + " and -server:" + args_server);
                    logger.log(Level.WARNING,"Force to use the argument -server instead of -settings");
                    logger.log(Level.WARNING,"Server : " + args_server);
                }
                server = args_server;
            }
            if ( use_args_config ) {
                if ( use_settings ) {
                    logger.log(Level.WARNING,"Found the conflicted configurations : -settings/config:" + config + " and -config:" + args_config);
                    logger.log(Level.WARNING,"Force to use the argument -config instead of -settings");
                    logger.log(Level.WARNING,"Config : " + args_config);
                }
                config = args_config;
            }

        }
        catch (final Exception ex)
        {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }

        logger.info("Alarm Server (PID " + ProcessHandle.current().pid() + ")");

        new AlarmServerMain(server, config, use_shell);
    }
}
