package org.phoebus.alarm.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class AlarmConfigLoggingService {

    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmConfigLoggingService.class.getPackageName());
    private static final ExecutorService Scheduler = Executors.newScheduledThreadPool(4);

    public static void main(String[] args) {
        logger.info("Starting the AlarmConfigLoggingService....");

        Properties properties = PropertiesHelper.getProperties();
        // Read list of Topics
        logger.info("Starting logger for: " + properties.getProperty("alarm_topics"));

        List<String> topicNames = Arrays.asList(properties.getProperty("alarm_topics").split(":"));
        // Check all the topic index already exist.

        // Start a new stream consumer for each topic
        topicNames.forEach(topic -> {
            Scheduler.execute(new AlarmConfigLogger(topic));
        });

    }
}
