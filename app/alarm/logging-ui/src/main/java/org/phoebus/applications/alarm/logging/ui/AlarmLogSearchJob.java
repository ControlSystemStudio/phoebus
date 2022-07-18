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
import org.phoebus.util.time.TimeParser;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A Job to search for alarm messages logged by the alarm logging service
 * @author Kunal Shroff
 */
public class AlarmLogSearchJob implements JobRunnable {
    private final String pattern;
    private final Boolean isNodeTable;
    private final ObservableMap<Keys, String> searchParameters;
    private final Consumer<List<AlarmLogTableType>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private final WebResource client;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));
    private final PreferencesReader prefs = new PreferencesReader(AlarmLogTableApp.class,
            "/alarm_logging_preferences.properties");

    public static Job submit(WebResource client,
                             final String pattern,
                             Boolean isNodeTable,
                             ObservableMap<Keys, String> searchParameters,
                             final Consumer<List<AlarmLogTableType>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogSearchJob(client, pattern, isNodeTable, searchParameters, alarmMessageHandler, errorHandler));
    }

    private AlarmLogSearchJob(WebResource client, String pattern, Boolean isNodeTable, ObservableMap<Keys, String> searchParameters,
            Consumer<List<AlarmLogTableType>> alarmMessageHandler, BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.pattern = pattern;
        this.isNodeTable = isNodeTable;
        this.searchParameters = searchParameters;
        this.alarmMessageHandler = alarmMessageHandler;
        this.errorHandler = errorHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(JobMonitor monitor) {
        monitor.beginTask("searching for alarm log entires : " + pattern);
        String searchPattern = "*".concat(pattern).concat("*");
        int size = prefs.getInt("es_max_size");
        Boolean configSet = false;

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.addFirst("pv", "*");
        //final List<AlarmLogTableType> result = new ArrayList<>();
        try {
            List<AlarmLogTableType> result = objectMapper.readValue(client.path("/search/alarm")
                    .queryParams(map)
                    .accept(MediaType.APPLICATION_JSON).get(String.class), new TypeReference<List<AlarmLogTableType>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

//        try {
//            SearchResponse<AlarmLogTableType> response = client.search(searchRequest, AlarmLogTableType.class);
//            response.hits().hits().forEach(hit->result.add(hit.source()));
//            alarmMessageHandler.accept(result);
//        } catch (IOException e) {
//            errorHandler.accept("Failed to search for alarm logs ", e);
//        }
    }
}
