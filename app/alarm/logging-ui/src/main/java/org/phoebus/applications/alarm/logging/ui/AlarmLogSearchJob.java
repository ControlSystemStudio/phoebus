package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.WebResource;
import javafx.collections.ObservableMap;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.preferences.PreferencesReader;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp.logger;

/**
 * A Job to search for alarm messages logged by the alarm logging service
 *
 * @author Kunal Shroff
 */
public class AlarmLogSearchJob implements JobRunnable {
    private final ObservableMap<Keys, String> searchParameters;
    private final Consumer<List<AlarmLogTableItem>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private final WebResource client;

    private final PreferencesReader prefs = new PreferencesReader(AlarmLogTableApp.class,
            "/alarm_logging_preferences.properties");

    public static Job submit(WebResource client,
                             final String pattern,
                             Boolean isNodeTable,
                             ObservableMap<Keys, String> searchParameters,
                             final Consumer<List<AlarmLogTableItem>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogSearchJob(client, isNodeTable, searchParameters, alarmMessageHandler, errorHandler));
    }

    private AlarmLogSearchJob(WebResource client, Boolean isNodeTable, ObservableMap<Keys, String> searchParameters,
                              Consumer<List<AlarmLogTableItem>> alarmMessageHandler, BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.searchParameters = searchParameters;
        this.alarmMessageHandler = alarmMessageHandler;
        this.errorHandler = errorHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(JobMonitor monitor) {
        String searchString = "searching for alarm log entries : " +
                searchParameters.entrySet().stream().filter(e -> !e.getValue().equals("")).map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
        logger.log(Level.INFO, "Searching for alarm entries: " + searchString);
        monitor.beginTask(searchString);
        int size = prefs.getInt("results_max_size");

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        searchParameters.forEach((key, value) -> { if (!value.equals("")) map.add(key.getName(), value); });
        map.putIfAbsent("size", List.of(String.valueOf(size)));

        try {

            long start = System.currentTimeMillis();
            String resultStr = client.path("/search/alarm")
                    .queryParams(map)
                    .accept(MediaType.APPLICATION_JSON).get(String.class);

            logger.log(Level.FINE, "String response = " + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            List<AlarmLogTableItem> result = objectMapper.readValue(resultStr, new TypeReference<>() {
            });
            logger.log(Level.FINE, "Object mapper response = " + (System.currentTimeMillis() - start));
            alarmMessageHandler.accept(result);
        } catch (Exception e) {
            errorHandler.accept("Failed to search for alarm logs ", e);
        }
    }
}
