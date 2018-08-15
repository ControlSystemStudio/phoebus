package org.phoebus.alarm.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class AlarmLoggingService {

    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmLoggingService.class.getPackageName());
    private static final ExecutorService Scheduler = Executors.newScheduledThreadPool(4);

    public static void main(String[] original_args) {
        logger.info("Starting the AlarmLoggingService....");

        // load the default properties
        Properties properties = PropertiesHelper.getProperties();

        // Handle arguments
        final List<String> args = new ArrayList<>(List.of(original_args));
        final Iterator<String> iter = args.iterator();
        try {
            while (iter.hasNext()) {

                final String cmd = iter.next();
                if (cmd.startsWith("-h")) {
                    help();
                    return;
                } else if (cmd.equals("-properties")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -properties properties file");
                    iter.remove();
                    try(FileInputStream file = new FileInputStream(iter.next());){
                        properties.load(file);
                    } catch(FileNotFoundException e) {
                        System.out.println();
                        e.printStackTrace();
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
                } else if (cmd.equals("-bootstrap.servers")) {
                    if (!iter.hasNext())
                        throw new Exception("Missing -bootstrap.servers kafaka server addresss");
                    iter.remove();
                    properties.put("bootstrap.servers",iter.next());
                    iter.remove();
                } else
                    throw new Exception("Unknown option " + cmd);
            }
        } catch (Exception ex) {
            help();
            System.out.println();
            ex.printStackTrace();
            return;
        }
        
        // Read list of Topics
        logger.info("Starting logger for: " + properties.getProperty("alarm_topics"));
        properties.forEach((k, v) -> { logger.info(k + ":" + v); });

        List<String> topicNames = Arrays.asList(properties.getProperty("alarm_topics").split(":"));
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
            Scheduler.execute(new AlarmStateLogger(topic));
        });

    }

    private static void help() {
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                       - This text");
        System.out.println("-topics   Accelerator       - Alarm topics to be logged, they can be defined as a comma separated list");
        System.out.println("-es_host  localhost         - elastic server host");
        System.out.println("-es_port  9200              - elastic server port");
        System.out.println("-bootstrap.servers localhost:9092 - Kafka server address");
        System.out.println("-properties /opt/alarm_logger.propertier - properties file to be used for this instance of the alarm logging service");
        System.out.println();
    }
}
