package org.phoebus.alarm.logging.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.phoebus.alarm.logging.AlarmLoggingService;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.util.time.TimeParser;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.alarm.logging.rest.SearchController.logger;

/**
 * A Job to search for alarm messages logged by the alarm logging service
 * @author Kunal Shroff
 */
public class AlarmLogSearchUtil {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    private static final PreferencesReader prefs = new PreferencesReader(AlarmLoggingService.class, "/application.properties");

    private static ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    private static final String PV = "pv";
    private static final String SEVERITY = "severity";
    private static final String MESSAGE = "message";
    private static final String CURRENTSEVERITY = "current_severity";
    private static final String CURRENTMESSAGE = "current_message";
    private static final String USER = "user";
    private static final String HOST = "host";
    private static final String COMMAND = "command";
    private static final String STARTTIME = "start";
    private static final String ENDTIME = "end";

    public static List<AlarmLogMessage> search(ElasticsearchClient client,
                                               String pattern,
                                               Map<String, String> searchParameters) {
        logger.info("searching for alarm log entires : " + pattern);

        String from = formatter.format(Instant.now().minus(7, ChronoUnit.DAYS));
        String to = formatter.format(Instant.now());
        String searchPattern = "*".concat(pattern).concat("*");
        int size = prefs.getInt("es_max_size");
        Boolean configSet = false;

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        for (Map.Entry<String, String> entry : searchParameters.entrySet()) {
            String key = entry.getKey();
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
                    value = "*".concat(value).concat("*");
                    String finalValue = value; //Effectively final
                    boolQuery.must(Query.of(q -> q
                                    .wildcard(WildcardQuery.of(w -> w
                                                    .field("config")
                                                    .value(finalValue)
                                            )
                                    )
                            )
                    );
                    configSet = true;
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
        final List<AlarmLogMessage> result = new ArrayList<>();
        try {
            SearchResponse<JsonNode> strResponse = client.search(searchRequest, JsonNode.class);
            return strResponse.hits().hits().stream().map(hit -> {
                JsonNode jsonNode = hit.source();
                try {
                    return mapper.treeToValue(jsonNode, AlarmLogMessage.class);
                } catch (JsonProcessingException e) {
                    logger.log(Level.SEVERE, "Failed to parse the searched alarm log messages. " + hit, e);
                }
                return null;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm logs ", e);
        }
        return result;
    }

}
