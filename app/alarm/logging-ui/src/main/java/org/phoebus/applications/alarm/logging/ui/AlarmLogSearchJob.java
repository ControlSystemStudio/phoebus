package org.phoebus.applications.alarm.logging.ui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.preferences.PreferencesReader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlarmLogSearchJob implements JobRunnable {
    private final RestHighLevelClient client;
    private final String pattern;
    private final Consumer<List<AlarmStateMessage>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private final PreferencesReader prefs = new PreferencesReader(AlarmLogTableApp.class,
            "/alarm_logging_preferences.properties");

    public static Job submit(RestHighLevelClient client, final String pattern,
            final Consumer<List<AlarmStateMessage>> alarmMessageHandler,
            final BiConsumer<String, Exception> errorHandler) {

        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogSearchJob(client, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogSearchJob(RestHighLevelClient client, String pattern,
            Consumer<List<AlarmStateMessage>> alarmMessageHandler, BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.pattern = pattern;
        this.alarmMessageHandler = alarmMessageHandler;
        this.errorHandler = errorHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(JobMonitor monitor) {
        monitor.beginTask("searching for alarm log entires : " + pattern);
        // TODO get the search pattern from the user
        QueryBuilder matchQueryBuilder = QueryBuilders.wildcardQuery("pv", "*");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder = sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.size(prefs.getInt("es_max_size"));
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);
        List<AlarmStateMessage> result;
        try {
            result = Arrays.asList(client.search(searchRequest).getHits().getHits()).stream()
                    .map(new Function<SearchHit, AlarmStateMessage>() {
                        @Override
                        public AlarmStateMessage apply(SearchHit hit) {
                            try {
                                JsonNode root = objectMapper.readTree(hit.getSourceAsString());
                                JsonNode value = ((ObjectNode) root).remove("time");
                                AlarmStateMessage alarmStateMessage = objectMapper.readValue(root.traverse(),
                                        AlarmStateMessage.class);
                                if (value != null) {
                                    Instant instant = LocalDateTime.parse(value.asText(), formatter)
                                            .atZone(ZoneId.systemDefault()).toInstant();
                                    alarmStateMessage.setInstant(instant);
                                }
                                return alarmStateMessage;
                            } catch (Exception e) {
                                errorHandler.accept("Failed to search for alarm logs ", e);
                                return null;
                            }
                        }
                    }).collect(Collectors.toList());
            alarmMessageHandler.accept(result);
        } catch (IOException e) {
            errorHandler.accept("Failed to search for alarm logs ", e);
        }
    }
}
