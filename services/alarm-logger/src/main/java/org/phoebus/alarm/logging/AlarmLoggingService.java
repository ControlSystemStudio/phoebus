package org.phoebus.alarm.logging;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.util.shell.CommandShell;

@SuppressWarnings("nls")
public class AlarmLoggingService {

    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmLoggingService.class.getPackageName());
    private static final ExecutorService Scheduler = Executors.newScheduledThreadPool(4);

    // Defaults
    private static String server = "localhost:9092";
    private static String config = "Accelerator";
    private static String es = "localhost:9200";

    private static void help()
    {
        // http://patorjk.com/software/taag/#p=display&f=Epic&t=Alarm%20Logger
        System.out.println(" _______  _        _______  _______  _______    _        _______  _______  _______  _______  _______ ");
        System.out.println("(  ___  )( \\      (  ___  )(  ____ )(       )  ( \\      (  ___  )(  ____ \\(  ____ \\(  ____ \\(  ____ )");
        System.out.println("| (   ) || (      | (   ) || (    )|| () () |  | (      | (   ) || (    \\/| (    \\/| (    \\/| (    )|");
        System.out.println("| (___) || |      | (___) || (____)|| || || |  | |      | |   | || |      | |      | (__    | (____)|");
        System.out.println("|  ___  || |      |  ___  ||     __)| |(_)| |  | |      | |   | || | ____ | | ____ |  __)   |     __)");
        System.out.println("| (   ) || |      | (   ) || (\\ (   | |   | |  | |      | |   | || | \\_  )| | \\_  )| (      | (\\ (   ");
        System.out.println("| )   ( || (____/\\| )   ( || ) \\ \\__| )   ( |  | (____/\\| (___) || (___) || (___) || (____/\\| ) \\ \\__");
        System.out.println("|/     \\|(_______/|/     \\||/   \\__/|/     \\|  (_______/(_______)(_______)(_______)(_______/|/   \\__/");
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                       - This text");
        System.out.println("-server  " + server + "     - Kafka server for reading alarm updates");
        System.out.println("-config  " + config + "        - Alarm configuration to monitor (more than one separated by ':')");
        System.out.println("-es      " + es + "     - Elastic Search for writing alarm updates");
        System.out.println("-logging logging.properties - Load log settings");
        System.out.println();
    }

    private static final String COMMANDS =
            "Commands:\n" +
            "\thelp             - Show help.\n" +
            "\tshutdown         - Shut alarm logger down and exit.\n";

    private static final CountDownLatch done = new CountDownLatch(1);

    private static boolean handleShellCommands(final String... args) throws Throwable
    {
        if (args.length == 1  &&  args[0].startsWith("shut"))
        {
            done.countDown();
            return true;
        }
        return false;
    }

    public static void main(final String[] original_args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(AlarmLoggingService.class.getResourceAsStream("/alarm_logger_logging.properties"));

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
                else if (cmd.equals("-es"))
                {
                    if (! iter.hasNext())
                        throw new Exception("Missing -es name");
                    iter.remove();
                    es = iter.next();
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

        logger.info("Alarm Logging Service (PID " + ProcessHandle.current().pid() + ")");
        final List<String> topicNames = Arrays.stream(config.split(":"))
                                              .map(String::trim)
                                              .collect(Collectors.toList());

        logger.info("Starting logger for '..State' of " + config);

        final int sep = es.lastIndexOf(':');
        if (sep < 0)
        {
            System.err.println("Cannot parse host:port from -es " + es);
            help();
            return;
        }
        final String es_host = es.substring(0, sep);
        final int es_port = Integer.parseInt(es.substring(sep+1));

        // Create an elastic client
        ElasticClientHelper.initialize(es_host, es_port);

        // Check all the topic index already exist.
        if (topicNames.stream().allMatch(topic -> {
            return ElasticClientHelper.getInstance().indexExists(topic.toLowerCase() + "_alarms");
        })) {
            logger.info("found elastic indexes for all alarm topics");
        } else {
            logger.severe("ERROR: elastic index missing for the configured topics.");
        }

        // Start a new stream consumer for each topic
        topicNames.forEach(topic -> {
            Scheduler.execute(new AlarmStateLogger(server, topic));
        });

        // Wait in command shell until closed

        final CommandShell shell = new CommandShell(COMMANDS, AlarmLoggingService::handleShellCommands);
        shell.start();
        done.await();
        shell.stop();

        System.out.println("\nDone.");
        // TODO Shut AlarmStateLoggers down?
        System.exit(0);
    }
}
