package org.phoebus.applications.alarm.logging.ui;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.time.temporal.TemporalAmount;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import javafx.collections.ObservableMap;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
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

/**
 * A Job to search for alarm messages logged by the alarm logging service
 * @author Kunal Shroff
 */
public class AlarmLogSearchJob implements JobRunnable {
    private final ElasticsearchClient client;
    private final String pattern;
    private final Boolean isNodeTable;
    private final ObservableMap<Keys, String> searchParameters;
    private final Consumer<List<AlarmLogTableType>> alarmMessageHandler;
    private final BiConsumer<String, Exception> errorHandler;

    private final ObjectMapper objectMapper;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));
    private final PreferencesReader prefs = new PreferencesReader(AlarmLogTableApp.class,
            "/alarm_logging_preferences.properties");

    public static Job submit(ElasticsearchClient client,
                             final String pattern,
                             Boolean isNodeTable,
                             ObservableMap<Keys, String> searchParameters,
                             final Consumer<List<AlarmLogTableType>> alarmMessageHandler,
                             final BiConsumer<String, Exception> errorHandler) {
        return JobManager.schedule("searching alarm log messages for : " + pattern,
                new AlarmLogSearchJob(client, pattern, isNodeTable, searchParameters, alarmMessageHandler, errorHandler));
    }

    private AlarmLogSearchJob(ElasticsearchClient client, String pattern, Boolean isNodeTable, ObservableMap<Keys, String> searchParameters,
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
        String from = "";
        String to = "";
        String searchPattern = "*".concat(pattern).concat("*");
        int size = prefs.getInt("es_max_size");
        Boolean configSet = false;

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

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
            if (key.equals("size")) {
                size = Math.min(size, Integer.parseInt(value));
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
                if (key.equals("pv")) {
                    if (isNodeTable) {
                        value = "*".concat(value).concat("*");
                        String finalValue = value; //Effectively final
                        boolQuery.must(Query.of(q->q
                                .wildcard(WildcardQuery.of(w->w
                                        .field("config")
                                        .value(finalValue)
                                        )
                                    )
                                )
                        );
                        configSet = true;
                    }
                    continue;
                }
                //Effectively final
                String finalVal2 = value;
                String finalKey2 = key;
                //
                boolQuery.must(Query.of(q->q
                        .wildcard(WildcardQuery.of(w->w
                                .field(finalKey2)
                                .value(finalVal2)
                            )
                        )
                    )
                );
            }
        }
        if (!configSet) {
            boolQuery.must(Query.of(q->q
                    .wildcard(WildcardQuery.of(w->w
                            .field("config")
                            .value(searchPattern)
                            )
                        )
                    )
            );
        }
        //Effectively final
        String finalFrom = from;
        String finalTo = to;
        //
        boolQuery.must(
                Query.of(q->q
                        .range(RangeQuery.of(r->r
                                .field("message_time")
                                .from(finalFrom)
                                .to(finalTo)
                        )
                        )
                )
        );
        int finalSize = size; //Effectively final
        SearchRequest searchRequest = SearchRequest.of(r->r
                .query(Query.of(q->q
                                .bool(boolQuery.build())
                    )
                )
                .size(finalSize)
                .sort(SortOptions.of(o->o
                        .field(FieldSort.of(f->f
                                .field("message_time")
                                .order(SortOrder.Desc)
                            )
                        )
                    )
                )
        );
        final List<AlarmLogTableType> result = new ArrayList<>();
        try {
            SearchResponse<AlarmLogTableType> response = client.search(searchRequest, AlarmLogTableType.class);
            response.hits().hits().forEach(hit->result.add(hit.source()));
            alarmMessageHandler.accept(result);
        } catch (IOException e) {
            errorHandler.accept("Failed to search for alarm logs ", e);
        }
    }
}
