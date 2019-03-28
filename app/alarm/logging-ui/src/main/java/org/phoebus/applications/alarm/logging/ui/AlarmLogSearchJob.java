package org.phoebus.applications.alarm.logging.ui;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import javafx.collections.ObservableMap;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableType;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.util.time.TimeParser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlarmLogSearchJob implements JobRunnable {
    private final RestHighLevelClient client;
    private final String pattern;
    private final ObservableMap<Keys, String> searchParameters;
    private final Consumer<List<AlarmLogTableType>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private final PreferencesReader prefs = new PreferencesReader(AlarmLogTableApp.class,
            "/alarm_logging_preferences.properties");

    public static Job submit(RestHighLevelClient client, final String pattern, ObservableMap<Keys, String> searchParameters,
            final Consumer<List<AlarmLogTableType>> alarmMessageHandler,
            final BiConsumer<String, Exception> errorHandler) {

        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogSearchJob(client, pattern, searchParameters, alarmMessageHandler, errorHandler));
    }

    private AlarmLogSearchJob(RestHighLevelClient client, String pattern, ObservableMap<Keys, String> searchParameters,
            Consumer<List<AlarmLogTableType>> alarmMessageHandler, BiConsumer<String, Exception> errorHandler) {
        super();
        this.client = client;
        this.pattern = pattern;
        this.searchParameters = searchParameters;
        this.alarmMessageHandler = alarmMessageHandler;
        this.errorHandler = errorHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(JobMonitor monitor) {
        monitor.beginTask("searching for alarm log entires : " + pattern);
        String from = "", to = "";
        String searchPattern = "*".concat(pattern).concat("*");

        BoolQueryBuilder boolQuery = new BoolQueryBuilder(); 
        boolQuery.must(QueryBuilders.wildcardQuery("config", searchPattern));
        for (Map.Entry<Keys, String> entry : searchParameters.entrySet()) {
            String key = entry.getKey().getName();
            String value = entry.getValue();
            if (key.equals("start")) {
                Object time = TimeParser.parseInstantOrTemporalAmount(value);
                if (time instanceof Instant) {
                    from = formatter.format((Instant)time);
                } else if (time instanceof TemporalAmount) {
                    from = formatter.format(Instant.now().minus((TemporalAmount)time));
                }
                continue;
            }
            if (key.equals("end")) {
                Object time = TimeParser.parseInstantOrTemporalAmount(value);
                if (time instanceof Instant) {
                    to = formatter.format((Instant)time);
                } else if (time instanceof TemporalAmount) {
                    to = formatter.format(Instant.now().minus((TemporalAmount)time));
                }
                continue;
            }
            if (!value.equals("*")) {
                if (key.equals("command")) {
                    if (value.equalsIgnoreCase("Enabled")) {
                        key = "enabled";
                        value = "true";
                    } else if (value.equalsIgnoreCase("Disabled")) {
                        key = "enabled";
                        value = "false";
                    }
                }
                boolQuery.must(QueryBuilders.wildcardQuery(key, value));
            }
        }

        boolQuery.must(QueryBuilders.rangeQuery("message_time").from(from).to(to));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder = sourceBuilder.query(boolQuery);
        sourceBuilder.size(prefs.getInt("es_max_size"));
        sourceBuilder.sort("message_time", SortOrder.DESC);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);
        List<AlarmLogTableType> result;
        try {
            result = Arrays.asList(client.search(searchRequest).getHits().getHits()).stream()
                    .map(new Function<SearchHit, AlarmLogTableType>() {
                        @Override
                        public AlarmLogTableType apply(SearchHit hit) {
                            try {
                                JsonNode root = objectMapper.readTree(hit.getSourceAsString());
                                JsonNode time = ((ObjectNode) root).remove("time");
                                JsonNode message_time = ((ObjectNode) root).remove("message_time");
                                AlarmLogTableType alarmMessage = objectMapper.readValue(root.traverse(),
                                        AlarmLogTableType.class);
                                if (time != null) {
                                    Instant instant = LocalDateTime.parse(time.asText(), formatter)
                                            .atZone(ZoneId.systemDefault()).toInstant();
                                    alarmMessage.setInstant(instant);
                                }
                                if (message_time != null) {
                                    Instant instant = LocalDateTime.parse(message_time.asText(), formatter)
                                            .atZone(ZoneId.systemDefault()).toInstant();
                                    alarmMessage.setMessage_time(instant);
                                }
                                return alarmMessage;
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
