package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobRunnableWithCancel;
import org.phoebus.util.http.QueryParamsHelper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A Job to retrieve the latest alarm configuration details
 *
 * @author Kunal Shroff
 */
public class AlarmLogConfigSearchJob extends JobRunnableWithCancel {
    private final HttpClient httpClient;
    private final String pattern;

    private final Consumer<AlarmLogTableItem> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;

    public static Job submit(HttpClient httpClient,
                             final String pattern,
                             final Consumer<AlarmLogTableItem> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogConfigSearchJob(httpClient, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogConfigSearchJob(HttpClient httpClient,
                                    String pattern,
                                    Consumer<AlarmLogTableItem> alarmMessageHandler,
                                    BiConsumer<String, Exception> errorHandler) {
        super();
        this.httpClient = httpClient;
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
            AlarmLogTableApp.logger.info("searching for config log entries : " +
                    "config: " + pattern);

            try {
                MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
                map.put("config", Arrays.asList(pattern));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.service_uri + "/search/alarm/config?" + QueryParamsHelper.mapToQueryParams(map)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                List<AlarmLogTableItem> result = objectMapper
                        .readValue(response.body(),
                                new TypeReference<List<AlarmLogTableItem>>() {
                                });
                if (result.size() >= 1) {
                    alarmMessageHandler.accept(result.get(0));
                } else {
                    alarmMessageHandler.accept(null);
                }
            } catch (Exception e) {
                errorHandler.accept("Failed to search for alarm logs ", e);
            }
        };
    }
}
