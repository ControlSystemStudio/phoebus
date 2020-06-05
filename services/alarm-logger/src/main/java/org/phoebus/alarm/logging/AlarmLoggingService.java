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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.phoebus.util.shell.CommandShell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:alarm_logger.properties")
public class AlarmLoggingService {

    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmLoggingService.class.getPackageName());
    private static ExecutorService Scheduler;

    private static ConfigurableApplicationContext context;

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
        System.out.println("-help                                    - This text");
        System.out.println("-noshell                                 - Disable the command shell for running without a terminal");
        System.out.println("-topics   Accelerator                    - Alarm topics to be logged, they can be defined as a comma separated list");
        System.out.println("-es_host  localhost                      - elastic server host");
        System.out.println("-es_port  9200                           - elastic server port");
        System.out.println("-es_sniff  false                         - elastic server sniff feature");
        System.out.println("-bootstrap.servers localhost:9092        - Kafka server address");
        System.out.println("-properties /opt/alarm_logger.properties - Properties file to be used (instead of command line arguments)");
        System.out.println("-date_span_units M                       - Date units for the time based index to span.");
        System.out.println("-date_span_value 1                       - Date value for the time based index to span.");
        System.out.println("-logging logging.properties              - Load log settings");
        System.out.println();
    }

    private static final String COMMANDS =
            "Commands:\n" +
            "\thelp             - Show help.\n" +
            "\tshutdown         - Shut alarm logger down and exit.\n";

    private static final CountDownLatch done = new CountDownLatch(1);

    private static boolean handleShellCommands(final String... args) throws Throwable
    {
        if (args == null  ||  (args.length == 1  &&  args[0].startsWith("shut")))
        {
            done.countDown();
            return true;
        }
        return false;
    }

    public static void main(final String[] original_args) throws Exception {
        context = SpringApplication.run(AlarmLoggingService.class, original_args);
        LogManager.getLogManager().readConfiguration(AlarmLoggingService.class.getResourceAsStream("/alarm_logger.properties"));

        // load the default properties
        final Properties properties = PropertiesHelper.getProperties();

        // Use interactive shell by default 
        boolean use_shell = true;

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try {
            while (iter.hasNext()) {

                final String cmd = iter.next();
		if ( cmd.equals("-h") || cmd.equals("-help")) {
		    use_shell = false;
                    help();
		    // Do we need the exit code for help?
		    System.exit(SpringApplication.exit(context));
                    return;
                } else if (cmd.equals("-noshell")) {
                    use_shell = false;
                    iter.remove();
                } else if (cmd.equals("-properties")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -properties properties file");
                    iter.remove();
                    try(FileInputStream file = new FileInputStream(iter.next());){
                        properties.load(file);
                    } catch(FileNotFoundException e) {
                        logger.log(Level.SEVERE, "failed to load server properties", e);
                    }
                    iter.remove();
                } else if (cmd.equals("-topics")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -topics topic name");
                    iter.remove();
                    properties.put("alarm_topics",iter.next());
                    iter.remove();
                } else if (cmd.equals("-es_host")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -es_host hostname");
                    iter.remove();
                    properties.put("es_host",iter.next());
                    iter.remove();
                } else if (cmd.equals("-es_port")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -es_port port number");
                    iter.remove();
                    properties.put("es_port",iter.next());
                    iter.remove();
                } else if (cmd.equals("-es_sniff")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -es_sniff sniff feature true/false");
                    iter.remove();
                    properties.put("es_sniff",iter.next());
                    iter.remove();
                } else if (cmd.equals("-bootstrap.servers")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -bootstrap.servers kafaka server addresss");
                    iter.remove();
                    properties.put("bootstrap.servers",iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-date_span_units"))
                {
                    if (!iter.hasNext())
                        throw new Exception("Missing -date_span_units unit type");
                    iter.remove();
                    properties.put("date_span_units",iter.next());
                    iter.remove();
                }
                else if (cmd.equals("-date_span_value"))
                {
                    if (!iter.hasNext())
                        throw new Exception("Missing -date_span_value amount");
                    iter.remove();
                    properties.put("date_span_value",iter.next());
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
                else if(cmd.equals("-thread_pool_size")){
                    if (! iter.hasNext()){
                        throw new Exception("Missing -thread_pool_size value");
                    }
                    iter.remove();
                    try {
                        String size = iter.next();
                        Integer threadPoolSize = Integer.valueOf(size);
                        properties.put("thread_pool_size", size);
                    } catch (NumberFormatException e) {
                        logger.warning("Specified thread pool size is not a number, will use value from properties or default value");
                    }
                    iter.remove();
                }
                else
                    throw new Exception("Unknown option " + cmd);
            }
        } catch (Exception ex) {
	    System.out.println("\n>>>> Print StackTrace ....");
	    ex.printStackTrace();
	    System.out.println("\n>>>> Please check available arguments of alarm-logger as follows:");
	    help();
	    System.exit(SpringApplication.exit(context));
	    return;
        }

        logger.info("Alarm Logging Service (PID " + ProcessHandle.current().pid() + ")");

        // Create scheduler with configured or default thread pool size
        Integer threadPoolSize;
        try {
            threadPoolSize = Integer.valueOf(properties.getProperty("thread_pool_size"));
        } catch (NumberFormatException e) {
            logger.info("Specified thread pool size is not a number, will default to 4");
            threadPoolSize = 4;
        }
        Scheduler = Executors.newScheduledThreadPool(threadPoolSize);

        logger.info("Properties:");
        properties.forEach((k, v) -> { logger.info(k + ":" + v); });

        // Read list of Topics
        final List<String> topicNames = Arrays.asList(properties.getProperty("alarm_topics").split(","));
        logger.info("Starting logger for '..State': " + topicNames);

        // Start a new stream consumer for each topic
        topicNames.forEach(topic -> {
            try
            {
                Scheduler.execute(new AlarmMessageLogger(topic));
                Scheduler.execute(new AlarmCmdLogger(topic));
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Creation of alarm logging service for '" + topic + "' failed", ex);
            }
        });

        // Wait in command shell until closed
        if(use_shell)
        {
            final CommandShell shell = new CommandShell(COMMANDS, AlarmLoggingService::handleShellCommands);
            shell.start();
            done.await();
            shell.stop();
        }
        else
        {
            Thread.currentThread().join();
        }

        close();
        System.exit(0);
    }

    /**
     * Clear all the resources associated with this service.
     */
    private static void close() {
        System.out.println("\n Shutdown");
        shutdownAndAwaitTermination(Scheduler);
        if (context != null) {
            context.close();
        }
    }

    static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
