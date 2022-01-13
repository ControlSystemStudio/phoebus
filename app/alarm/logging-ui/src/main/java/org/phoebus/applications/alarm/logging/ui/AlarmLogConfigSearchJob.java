package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.phoebus.framework.jobs.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Job to retrieve the latest alarm configuration details
 * @author Kunal Shroff
 */
public class AlarmLogConfigSearchJob extends JobRunnableWithCancel {
    private final RestHighLevelClient client;
    private final String pattern;
    private final Consumer<List<String>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    public static Job submit(RestHighLevelClient client,
                             final String pattern,
                             final Consumer<List<String>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogConfigSearchJob(client, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogConfigSearchJob(RestHighLevelClient client,
                                    String pattern,
                                    Consumer<List<String>> alarmMessageHandler,
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
            String searchPattern = "*".concat(pattern).concat("*");
            int size = 1;
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.must(QueryBuilders.wildcardQuery("config", searchPattern));
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder = sourceBuilder.query(boolQuery);
            sourceBuilder.size(size);
            sourceBuilder.sort("message_time", SortOrder.DESC);

            SearchRequest searchRequest = new SearchRequest("*alarms_config*");
            searchRequest.source(sourceBuilder);
            List<String> result;
            try {
                result = Arrays.asList(client.search(searchRequest, RequestOptions.DEFAULT).getHits().getHits()).stream()
                        .map(hit -> {
                            try {
                                String source = hit.getSourceAsString();
                                JsonNode root = objectMapper.readTree(source);
                                JsonNode time = ((ObjectNode) root).remove("time");
                                JsonNode message_time = ((ObjectNode) root).remove("message_time");
                                JsonNode message = ((ObjectNode) root).get("config_msg");

                                String alarmSource = message.asText().trim();
                                // Backwards compatibility for the old invalid json representation of alarm messages
                                if (alarmSource.startsWith("AlarmConfigMessage")) {
                                    return alarmSource.replace("AlarmConfigMessage","");
                                }
                                Object json = objectMapper.readValue(alarmSource, Object.class);
                                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            } catch (Exception e) {
                                errorHandler.accept("Failed to search for alarm config ", e);
                                return null;
                            }
                        }).collect(Collectors.toList());
                alarmMessageHandler.accept(result);
            } catch (IOException e) {
                errorHandler.accept("Failed to search for alarm config ", e);
            }
        };
    }
}
