package org.phoebus.alarm.logging;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.phoebus.util.shell.CommandShell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlarmConfigLoggingService {

    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmConfigLoggingService.class.getPackageName());
    private static final ExecutorService Scheduler = Executors.newScheduledThreadPool(4);

    private static void help() {
        System.out.println("\r\n"
                + " _______  _        _______  _______  _______    _______  _______  _        _______ _________ _______    _        _______  _______  _______  _______  _______ \r\n"
                + "(  ___  )( \\      (  ___  )(  ____ )(       )  (  ____ \\(  ___  )( (    /|(  ____ \\\\__   __/(  ____ \\  ( \\      (  ___  )(  ____ \\(  ____ \\(  ____ \\(  ____ )\r\n"
                + "| (   ) || (      | (   ) || (    )|| () () |  | (    \\/| (   ) ||  \\  ( || (    \\/   ) (   | (    \\/  | (      | (   ) || (    \\/| (    \\/| (    \\/| (    )|\r\n"
                + "| (___) || |      | (___) || (____)|| || || |  | |      | |   | ||   \\ | || (__       | |   | |        | |      | |   | || |      | |      | (__    | (____)|\r\n"
                + "|  ___  || |      |  ___  ||     __)| |(_)| |  | |      | |   | || (\\ \\) ||  __)      | |   | | ____   | |      | |   | || | ____ | | ____ |  __)   |     __)\r\n"
                + "| (   ) || |      | (   ) || (\\ (   | |   | |  | |      | |   | || | \\   || (         | |   | | \\_  )  | |      | |   | || | \\_  )| | \\_  )| (      | (\\ (   \r\n"
                + "| )   ( || (____/\\| )   ( || ) \\ \\__| )   ( |  | (____/\\| (___) || )  \\  || )      ___) (___| (___) |  | (____/\\| (___) || (___) || (___) || (____/\\| ) \\ \\__\r\n"
                + "|/     \\|(_______/|/     \\||/   \\__/|/     \\|  (_______/(_______)|/    )_)|/       \\_______/(_______)  (_______/(_______)(_______)(_______)(_______/|/   \\__/\r\n"
                + "                                                                                                                                                             \r\n"
                + "");
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                                      - This text");
        System.out.println("-topics   Accelerator                      - Alarm topics whoes assocaited configuration is to be logged, they can be defined as a comma separated list");
        System.out.println("-bootstrap.servers localhost:9092          - Kafka server address");
        System.out.println("-repo.location /tmp/alarm_repo             - Location of the alarm configuration repository");
        System.out.println("-remote.location https://remote.git/repo   - Location of the remote git alarm configuration repository");
        System.out.println("-username username                         - username for remote git repo");
        System.out.println("-password password                         - password for remote git repo");
        System.out.println("-properties alarm_config_logger.properties - Properties file to be used (instead of command line arguments)");
        System.out.println("-logging logging.properties                - Load log settings");
        System.out.println();
    }

    private static final String COMMANDS = "Commands:\n"
                                                + "\thelp             - Show help.\n"
                                                + "\tshutdown         - Shut alarm logger down and exit.\n";

    private static final CountDownLatch done = new CountDownLatch(1);

    private static boolean handleShellCommands(final String... args) throws Throwable {
        if (args == null || (args.length == 1 && args[0].startsWith("shut"))) {
            done.countDown();
            return true;
        }
        return false;
    }

    public static void main(String[] original_args) throws InterruptedException {
	SpringApplication.run(AlarmConfigLoggingService.class, original_args);
        logger.info("Starting the AlarmConfigLoggerService....");

        Properties properties = PropertiesHelper.getProperties();

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try {
            while (iter.hasNext()) {

                final String cmd = iter.next();
		if ( cmd.equals("-h") || cmd.equals("-help")) {
		    help();
                    return;
                } else if (cmd.equals("-properties")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -properties properties file");
                    iter.remove();
                    try (FileInputStream file = new FileInputStream(iter.next());) {
                        properties.load(file);
                    } catch (FileNotFoundException e) {
                        logger.log(Level.SEVERE, "failed to load server properties", e);
                    }
                    iter.remove();
                } else if (cmd.equals("-topics")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -topics topic name");
                    iter.remove();
                    properties.put("alarm_topics", iter.next());
                    iter.remove();
                } else if (cmd.equals("-bootstrap.servers")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -bootstrap.servers kafaka server addresss");
                    iter.remove();
                    properties.put("bootstrap.servers", iter.next());
                    iter.remove();
                } else if (cmd.equals("-repo.location")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -repo.location local checkout location for alarm confing repo");
                    iter.remove();
                    properties.put("local.location", iter.next());
                    iter.remove();
                } else if (cmd.equals("-remote.location")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -remote.location URL to remote git repo");
                    iter.remove();
                    properties.put("remote.location", iter.next());
                    iter.remove();
                } else if (cmd.equals("-username")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -username username for remote git repo");
                    iter.remove();
                    properties.put("username", iter.next());
                    iter.remove();
                } else if (cmd.equals("-password")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -paassword password for remote git repo");
                    iter.remove();
                    properties.put("password", iter.next());
                    iter.remove();
                } else if (cmd.equals("-logging")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -logging file name");
                    iter.remove();
                    final String filename = iter.next();
                    iter.remove();
                    LogManager.getLogManager().readConfiguration(new FileInputStream(filename));
                } else
                    throw new Exception("Unknown option " + cmd);
            }
        } catch (Exception ex) {
            System.out.println();
	    System.out.println("\n>>>> Print StackTrace ....");
            ex.printStackTrace();
	    System.out.println("\n>>>> Please check available arguments of alarm-config-logger as follows:");
	    help();
            return;
        }

        logger.info("Alarm Logging Service (PID " + ProcessHandle.current().pid() + ")");
        // Read list of Topics
        logger.info("Starting logger for: " + properties.getProperty("alarm_topics"));
        List<String> topicNames = Arrays.asList(properties.getProperty("alarm_topics").split(":"));

        String location = properties.getProperty("local.location");
        String remoteLocation = properties.getProperty("remote.location");

        // Start a new stream consumer for each topic
        topicNames.forEach(topic -> {
            Scheduler.execute(new AlarmConfigLogger(topic, location, remoteLocation));
        });
        // Wait in command shell until closed
        final CommandShell shell = new CommandShell(COMMANDS, AlarmConfigLoggingService::handleShellCommands);
        shell.start();
        done.await();
        shell.stop();

        System.out.println("\nDone.");
    }
}
