package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.WebResource;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A Job to retrieve the latest alarm configuration details
 * @author Kunal Shroff
 */
public class AlarmLogConfigSearchJob extends JobRunnableWithCancel {
    private final WebResource client;
    private final String pattern;

    private final Consumer<List<AlarmConfigMessage>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    public static Job submit(WebResource client,
                             final String pattern,
                             final Consumer<List<AlarmConfigMessage>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogConfigSearchJob(client, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogConfigSearchJob(WebResource client,
                                    String pattern,
                                    Consumer<List<AlarmConfigMessage>> alarmMessageHandler,
                                    BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.pattern = pattern;
        this.alarmMessageHandler = alarmMessageHandler;
        this.errorHandler = errorHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getName() {
        return "Search for Alarm Config Info for " + pattern;
    }

    @Override
    public Runnable getRunnable() {
        return () -> {
            List<String> result;
            List<AlarmConfigMessage> cResult = new ArrayList<>();

        };
    }
}
