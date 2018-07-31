/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.csstudio.archive.engine.config.RDBConfig;
import org.csstudio.archive.engine.model.EngineModel;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;

@SuppressWarnings("nls")
public class Engine
{
    /** Logger for all engine code */
    public static final Logger logger = Logger.getLogger(Engine.class.getPackageName());

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
        System.out.println("-help                       - This text");
        System.out.println("-engine demo                - Engine configuration name");
        System.out.println("-port 4812                  - HTTP Server port");
        System.out.println("-skip_last                  - Skip reading last sample time from RDB on start-up");
        System.out.println("-import engine_config.xml   - Import configuration from XML");
        System.out.println("-export engine_config.xml   - Export configuration to XML");
        System.out.println("-settings settings.xml      - Import preferences (PV connectivity) from property format file");
        System.out.println("-logging logging.properties - Load log settings");
        System.out.println();
    }

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(Engine.class.getResourceAsStream("/engine_logging.properties"));

        String config_name = "Demo";
        int port = 4812;
        boolean skip_last = false;

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
                else if (cmd.equals("-engine"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -engine config name");
                    iter.remove();
                    config_name = iter.next();
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
                else if (cmd.equals("-logging"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -logging file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    LogManager.getLogManager().readConfiguration(new FileInputStream(filename));
                }

                // TODO -import engine_config.xml
                // TODO -export engine_config.xml

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

        logger.info("Archive Engine (PID " + ProcessHandle.current().pid() + ")");

        logger.info("Engine Configuration: " + config_name);


        // TODO Command to check RDBConnectionPool

        // TODO Web server and CommandShell, see ScanServerInstance

        final EngineModel model = new EngineModel();
        new RDBConfig().readConfig(config_name, port, skip_last, model);
    }
}
