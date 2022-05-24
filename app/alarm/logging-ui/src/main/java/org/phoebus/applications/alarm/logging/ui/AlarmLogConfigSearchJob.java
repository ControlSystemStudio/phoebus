package org.phoebus.applications.alarm.logging.ui;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.framework.jobs.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Job to retrieve the latest alarm configuration details
 * @author Kunal Shroff
 */
public class AlarmLogConfigSearchJob extends JobRunnableWithCancel {
    private final ElasticsearchClient client;
    private final String pattern;

    private final Consumer<List<AlarmConfigMessage>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    public static Job submit(ElasticsearchClient client,
                             final String pattern,
                             final Consumer<List<AlarmConfigMessage>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogConfigSearchJob(client, pattern, alarmMessageHandler, errorHandler));
    }

    private AlarmLogConfigSearchJob(ElasticsearchClient client,
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
            String searchPattern = "*".concat(pattern).concat("*");
            int size = 1;
            BoolQuery boolQuery = BoolQuery.of(bq->bq
                    .must(Query.of(q->q
                            .wildcard(WildcardQuery.of(w->w
                                    .field("config")
                                    .value(searchPattern)
                                )
                            )
                        )
                    )
            );

            SearchRequest searchRequest = SearchRequest.of(req->req
                    .query(Query.of(q->q
                            .bool(boolQuery)
                        )
                    )
                    .index("*alarms_config*")
                    .size(size)
                    .sort(
                            SortOptions.of(s->s
                                    .field(FieldSort.of(f->f
                                            .field("message_time")
                                            .order(SortOrder.Desc)
                                        )
                                    )
                            )
                    )
            );
            List<String> result;
            List<AlarmConfigMessage> cResult = new ArrayList<>();
            SearchResponse<AlarmConfigMessage> response;
            try {
                response = client.search(searchRequest, AlarmConfigMessage.class);
                response.hits().hits().forEach(hit->cResult.add(hit.source()));
                /*result = Arrays.asList(client.search(searchRequest, AlarmConfigMessage.class).hits().hits()).stream()
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
                        }).collect(Collectors.toList());*/
                alarmMessageHandler.accept(cResult);
            } catch (IOException e) {
                errorHandler.accept("Failed to search for alarm config ", e);
            }
        };
    }
}
