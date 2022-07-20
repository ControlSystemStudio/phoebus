package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.WebResource;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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

    private final Consumer<AlarmLogTableItem> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    public static Job submit(WebResource client,
                             final String pattern,
                             final Consumer<AlarmLogTableItem> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogConfigSearchJob(client, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogConfigSearchJob(WebResource client,
                                    String pattern,
                                    Consumer<AlarmLogTableItem> alarmMessageHandler,
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
            AlarmLogTableApp.logger.info("searching for alarm log entires : " +
                    "config: " + pattern);

            try {
                MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
                map.put("config", Arrays.asList(pattern));
                map.put("size", Arrays.asList(String.valueOf(1)));
                List<AlarmLogTableItem> result = objectMapper
                        .readValue(client.path("/search/alarm/").queryParams(map).accept(MediaType.APPLICATION_JSON).get(String.class),
                                new TypeReference<List<AlarmLogTableItem>>() {
                                });
                if (result.size() >= 1) {
                    alarmMessageHandler.accept(result.get(0));
                }
            } catch (JsonProcessingException e) {
                errorHandler.accept("Failed to search for alarm logs ", e);
            }
        };
    }
}
