/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.csstudio.archive.engine.config.RDBConfig;
import org.csstudio.archive.engine.config.XMLConfig;
import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.server.EngineWebServer;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.util.shell.CommandShell;

/** Archive engine 'main'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Engine
{
    public static final String VERSION = "4.0.0";

    /** Logger for all engine code */
    public static final Logger logger = Logger.getLogger(Engine.class.getPackageName());

    private static EngineModel model = null;

    public static void help()
    {
        // http://patorjk.com/software/taag/#p=display&f=Epic&t=Archive%20Engine
        System.out.println("        _______  _______  _______          _________          _______    _______  _        _______ _________ _        _______ ");
        System.out.println("       (  ___  )(  ____ )(  ____ \\|\\     /|\\__   __/|\\     /|(  ____ \\  (  ____ \\( (    /|(  ____ \\\\__   __/( (    /|(  ____ \\");
        System.out.println("       | (   ) || (    )|| (    \\/| )   ( |   ) (   | )   ( || (    \\/  | (    \\/|  \\  ( || (    \\/   ) (   |  \\  ( || (    \\/");
        System.out.println("       | (___) || (____)|| |      | (___) |   | |   | |   | || (__      | (__    |   \\ | || |         | |   |   \\ | || (__    ");
        System.out.println("       |  ___  ||     __)| |      |  ___  |   | |   ( (   ) )|  __)     |  __)   | (\\ \\) || | ____    | |   | (\\ \\) ||  __)   ");
        System.out.println("       | (   ) || (\\ (   | |      | (   ) |   | |    \\ \\_/ / | (        | (      | | \\   || | \\_  )   | |   | | \\   || (      ");
        System.out.println("       | )   ( || ) \\ \\__| (____/\\| )   ( |___) (___  \\   /  | (____/\\  | (____/\\| )  \\  || (___) |___) (___| )  \\  || (____/\\");
        System.out.println("       |/     \\||/   \\__/(_______/|/     \\|\\_______/   \\_/   (_______/  (_______/|/    )_)(_______)\\_______/|/    )_)(_______/");

        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                         This text");
        System.out.println("-engine demo                  Engine configuration name");
        System.out.println("-host localhost               HTTP Host name");
        System.out.println("-port 4812                    HTTP Server port");
        System.out.println("-skip_last                    Skip reading last sample time from RDB on start-up");
        System.out.println("-list                         List engine names");
        System.out.println("-delete_config                Delete existing engine config");
        System.out.println("-export engine_config.xml     Export configuration to XML");
        System.out.println("-import engine_config.xml     Import configuration from XML");
        System.out.println("-description \"Some Info\"      Import: Description for the engine");
        System.out.println("-replace_engine               Import: Replace existing engine config, or stop?");
        System.out.println("-steal_channels               Import: Reassign channels that belong to other engine?");
        System.out.println("-settings settings.ini        Import preferences (PV connectivity, archive URL, ...) from property format file");
        System.out.println("-noshell                      Disable the command shell for running without a terminal");
        System.out.println("-logging logging.properties   Load log settings");
        System.out.println();
    }

    private static final String COMMANDS =
        "Archive Engine Commands:\n" +
        "help            -  Show commands\n" +
        "disconnected    -  Show disconnected channels\n" +
        "restart         -  Restart archive engine\n" +
        "shutdown        -  Stop the archive engine";

    public static EngineModel getModel()
    {
        return model;
    }

    private static boolean handleShellCommands(final String... args) throws Throwable
    {
        if (args == null)
            model.requestStop();
        else if (args.length == 1)
        {
            if (args[0].startsWith("dis"))
            {
                System.out.println("Disconnected channels:");
                int disconnected = 0;
                final int group_count = model.getGroupCount();
                for (int i=0; i<group_count; ++i)
                {
                    final ArchiveGroup group = model.getGroup(i);
                    final int channel_count = group.getChannelCount();
                    for (int j=0; j<channel_count; ++j)
                    {
                        final ArchiveChannel channel = group.getChannel(j);
                        if (! channel.isConnected())
                        {
                            ++disconnected;
                            System.out.println(channel.getName());
                       }
                    }
                }
                System.out.println("Total: " + disconnected);
            }
            else if (args[0].startsWith("shut"))
                model.requestStop();
            else if (args[0].startsWith("restart"))
                model.requestRestart();
            else
                return false;
        }
        else
            return false;
        return true;
    }

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(Engine.class.getResourceAsStream("/engine_logging.properties"));

        String config_name = "Demo";
        String host_name = "localhost";
        String description = "";

        int port = 4812;
        boolean skip_last = false;
        boolean list = false, delete = false, replace_engine = false, steal_channels = false, use_shell = true;
        File import_file = null, export_file = null;

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try
        {
            while (iter.hasNext())
            {
                final String cmd = iter.next();
                if (cmd.equals("-host"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -host name");
                    iter.remove();
                    host_name = iter.next();
                    iter.remove();
                }
                else if (cmd.startsWith("-h"))
                {
                    help();
                    return;
                }
                else if (cmd.equals("-engine"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -engine config name");
                    iter.remove();
                    config_name = iter.next();
                    iter.remove();
                }
                else if (cmd.equals("-host"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -host name");
                    iter.remove();
                    host_name = iter.next();
                    iter.remove();
                }
                else if (cmd.equals("-port"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -port number");
                    iter.remove();
                    port = Integer.parseInt(iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-skip_last"))
                {
                    iter.remove();
                    skip_last = true;
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
                else if (cmd.equals("-noshell"))
                {
                    use_shell = false;
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
                else if (cmd.equals("-list"))
                {
                    list = true;
                    iter.remove();
                }
                else if (cmd.equals("-delete_config"))
                {
                    delete = true;
                    iter.remove();
                }
                else if (cmd.equals("-export"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -export file name");
                    iter.remove();
                    export_file = new File(iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-import"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -import file name");
                    iter.remove();
                    import_file = new File(iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-description"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -description text");
                    iter.remove();
                    description = iter.next();
                    iter.remove();
                }
                else if (cmd.equals("-replace_engine"))
                {
                    replace_engine = true;
                    iter.remove();
                }
                else if (cmd.equals("-steal_channels"))
                {
                    steal_channels = true;
                    iter.remove();
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

        logger.info("Archive Engine " + VERSION + " (PID " + ProcessHandle.current().pid() + ")");
        logger.info("Engine Configuration: " + config_name);


        if (list)
        {
            try
            (
                RDBConfig config = new RDBConfig();
            )
            {
                System.out.println("Archive Engine Configurations:");
                System.out.println("ID  Name                 Description                              URL");
                for (String cfg : config.list())
                    System.out.println(cfg);
            }
            return;
        }

        if (delete)
        {
            try
            (
                RDBConfig config = new RDBConfig();
            )
            {
                logger.log(Level.INFO, "Deleting engine config '" + config_name + "' ...");
                config.delete(config_name, true);
                logger.log(Level.INFO, "Done.");
            }
            return;
        }

        if (import_file != null)
        {
            final String url = "http://" + host_name + ":" + port + "/main";
            logger.log(Level.INFO, "Importing config    : " + import_file);
            logger.log(Level.INFO, "Description         : " + description);
            logger.log(Level.INFO, "URL                 : " + url);
            logger.log(Level.INFO, "Replace engine      : " + replace_engine);
            logger.log(Level.INFO, "Steal channels      : " + steal_channels);

            try
            (
                RDBConfig config = new RDBConfig();
            )
            {
                final int engine_id = config.createEngine(config_name, description, replace_engine, url);
                new XMLConfig().read(import_file, config, engine_id, steal_channels);
            }
            return;
        }

        if (export_file != null)
        {
            model = new EngineModel();
            try
            (
                RDBConfig config = new RDBConfig();
            )
            {
                config.read(model, config_name, port, true);
            }
            logger.log(Level.INFO, "Saving configuration to " + export_file);
            new XMLConfig().write(model, export_file);
            return;
        }

        final CommandShell shell;
        if (use_shell)
        {
            shell = new CommandShell(COMMANDS, Engine::handleShellCommands);
            shell.start();
        }
        else
            shell = null;
        boolean run = true;
        while (run)
        {
            logger.log(Level.INFO, "Reading configuration");
            model = new EngineModel();
            try
            (
                RDBConfig config = new RDBConfig();
            )
            {
                config.read(model, config_name, port, skip_last);
            }

            logger.log(Level.INFO, "Archive Engine web interface on http://localhost:" + port + "/index.html");
            final EngineWebServer httpd = new EngineWebServer(port);
            try
            {
                httpd.start();
                model.start();

                // Main thread could do other things while web server & shell are running...
                while (true)
                {
                    if (model.getState() == EngineModel.State.SHUTDOWN_REQUESTED)
                    {
                        run = false;
                        break;
                    }
                    if (model.getState() == EngineModel.State.RESTART_REQUESTED)
                        break;
                    TimeUnit.MILLISECONDS.sleep(100);
                }

                model.stop();
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot start", ex);
                run = false;
            }
            httpd.shutdown();
        }

        if (shell != null)
            shell.stop();
        logger.info("Done.");
        System.exit(0);
    }
}
