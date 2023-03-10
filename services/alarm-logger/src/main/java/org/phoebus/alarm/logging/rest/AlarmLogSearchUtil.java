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
import org.phoebus.util.indexname.IndexNameHelper;
import org.phoebus.util.time.TimeParser;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.alarm.logging.rest.SearchController.logger;

/**
 * A Job to search for alarm messages logged by the alarm logging service
 *
 * @author Kunal Shroff
 */
public class AlarmLogSearchUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    private static final PreferencesReader prefs = new PreferencesReader(AlarmLoggingService.class, "/application.properties");

    private static final ObjectMapper mapper;

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

    private static final String ROOT = "root";

    /**
     * Find all the log (state and config) messages which match the search criteria
     *
     * @param client           elastic client
     * @param searchParameters search parameters
     * @return list of alarm state and config messages
     */
    public static List<AlarmLogMessage> search(ElasticsearchClient client,
                                               Map<String, String> searchParameters) {
        logger.info("searching for alarm log entires : " +
                searchParameters.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining()));

        Instant fromInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant toInstant = Instant.now();

        // The maximum search result size
        int maxSize = prefs.getInt("es_max_size");
        final String indexDateSpanUnits = prefs.get("date_span_units");

        boolean configSet = false;
        boolean temporalSearch = false;

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        List<String> indexList = new ArrayList<>();

        String root = "";

        for (Map.Entry<String, String> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                case STARTTIME:
                    Object startTime = TimeParser.parseInstantOrTemporalAmount(parameter.getValue().strip());
                    if (startTime instanceof Instant) {
                        fromInstant = (Instant) startTime;
                    } else if (startTime instanceof TemporalAmount) {
                        fromInstant = Instant.now().minus((TemporalAmount) startTime);
                    }
                    temporalSearch = true;
                    break;
                case ENDTIME:
                    Object endTime = TimeParser.parseInstantOrTemporalAmount(parameter.getValue().strip());
                    if (endTime instanceof Instant) {
                        toInstant = (Instant) endTime;
                    } else if (endTime instanceof TemporalAmount) {
                        toInstant = Instant.now().minus((TemporalAmount) endTime);
                    }
                    temporalSearch = true;
                    break;
                case "size":
                    maxSize = Math.min(maxSize, Integer.parseInt(parameter.getValue().strip()));
                    break;
                case COMMAND:
                    if (parameter.getValue().strip().equalsIgnoreCase("Enabled")) {
                        boolQuery.must(WildcardQuery.of(w -> w.field("enabled").value("true"))._toQuery());
                    } else if (parameter.getValue().strip().equalsIgnoreCase("Disabled")) {
                        boolQuery.must(WildcardQuery.of(w -> w.field("enabled").value("false"))._toQuery());
                    }
                    break;
                case PV:
                    boolQuery.must(Query.of(q -> q
                                    .wildcard(WildcardQuery.of(w -> w
                                                    .field("config")
                                                    .value("*" + parameter.getValue().strip() + "*")
                                            )
                                    )
                            )
                    );
                    configSet = true;
                    break;
                case ROOT:
                    if (!parameter.getValue().equalsIgnoreCase("*")) {
                        root = parameter.getValue().strip();
                        String _root = root;
                        boolQuery.must(
                                Query.of(b -> b.bool(s -> s.should(
                                        Query.of(q -> q
                                                .wildcard(WildcardQuery.of(w -> w
                                                                .field("config").value("state:/" + _root + "*")
                                                        )
                                                )
                                        ),
                                        Query.of(q -> q
                                                .wildcard(WildcardQuery.of(w -> w
                                                                .field("config").value("config:/" + _root + "*")
                                                        )
                                                )
                                        )
                                )))
                        );
                        configSet = true;
                    }
                    break;
                case SEVERITY:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(SEVERITY)
                                .value(parameter.getValue().strip().toUpperCase()))._toQuery()
                        );
                    break;
                case CURRENTSEVERITY:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(CURRENTSEVERITY)
                                .value(parameter.getValue().strip().toUpperCase()))._toQuery()
                        );
                    break;
                case MESSAGE:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(MESSAGE)
                                .value(parameter.getValue().strip()))._toQuery()
                        );
                    break;
                case CURRENTMESSAGE:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(CURRENTMESSAGE)
                                .value(parameter.getValue().strip()))._toQuery()
                        );
                    break;
                case USER:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(USER)
                                .value(parameter.getValue().strip()))._toQuery()
                        );
                    break;
                case HOST:
                    if (!parameter.getValue().equalsIgnoreCase("*"))
                        boolQuery.must(WildcardQuery.of(w -> w
                                .field(HOST)
                                .value(parameter.getValue().strip()))._toQuery()
                        );
                    break;
                default:
                    // Unsupported search parameters are ignored
                    break;
            }
        }

        if (!configSet) {
            boolQuery.must(Query.of(q -> q
                            .wildcard(WildcardQuery.of(w -> w
                                            .field("config")
                                            .value("*")
                                    )
                            )
                    )
            );
        }

        // Add the temporal queries
        if (temporalSearch) {
            // TODO check that the start is before the end
            if (fromInstant.isBefore(toInstant)) {
            } else {
                //
                logger.log(Level.SEVERE,
                        "Failed to search for alarm logs: invalid time range from: " + formatter.format(fromInstant) + " to: " + formatter.format(toInstant));
            }
            //Effectively final
            Instant finalFromInstant = fromInstant;
            Instant finalToInstant = toInstant;
            boolQuery.must(
                    Query.of(q -> q
                            .range(RangeQuery.of(r -> r
                                            .field("message_time")
                                            .from(formatter.format(finalFromInstant))
                                            .to(formatter.format(finalToInstant))
                                    )
                            )
                    )
            );

            try {
                // "root" is empty string unless user specifies one, in which case we can narrow down to
                // only matching alarm config indices.
                indexList = findIndexNames(root.toLowerCase() + "*", fromInstant, toInstant, indexDateSpanUnits);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Failed to search for alarm logs:" + e.getMessage(), e);
            }
        }

        int finalSize = maxSize; //Effectively final
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.query(Query.of(q -> q
                .bool(boolQuery.build())
        ));
        searchRequestBuilder.size(finalSize);
        searchRequestBuilder.sort(SortOptions.of(o -> o
                        .field(FieldSort.of(f -> f
                                        .field("message_time")
                                        .order(SortOrder.Desc)
                                )
                        )
                )
        );
        if (!indexList.isEmpty()) {
            searchRequestBuilder.index(indexList);
        }
        SearchRequest searchRequest = searchRequestBuilder.build();
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
        return Collections.emptyList();
    }

    /**
     * Return the latest alarm config message associated with 'config'
     *
     * @param client           elastic client
     * @param allRequestParams the wildcard pattern which matches the 'config'
     * @return last alarm config message for the given 'config'
     */
    public static List<AlarmLogMessage> searchConfig(ElasticsearchClient client, Map<String, String> allRequestParams) {
        String configString = allRequestParams.get("config");
        // Determine which alarm config to specify as Elasticsearch index
        String alarmConfig = configString.split("/")[1];

        String searchPattern = "*".concat(configString).concat("*");
        int size = 1;

        SearchRequest searchRequest = SearchRequest.of(r -> r
                .query(Query.of(q -> q.wildcard(WildcardQuery.of(w -> w.field("config").value(searchPattern)))))
                .size(size)
                .sort(SortOptions.of(o -> o
                                .field(FieldSort.of(f -> f
                                                .field("message_time")
                                                .order(SortOrder.Desc)
                                        )
                                )
                        )
                )
                .index(alarmConfig + "_alarms_config_*")
        );

        try {
            SearchResponse<JsonNode> strResponse = client.search(searchRequest, JsonNode.class);
            return strResponse.hits().hits().stream().map(hit -> {
                JsonNode jsonNode = hit.source();
                try {
                    return mapper.treeToValue(jsonNode, AlarmLogMessage.class);
                } catch (JsonProcessingException e) {
                    logger.log(Level.SEVERE, "Failed to parse the searched alarm config messages. " + hit, e);
                }
                return null;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm config logs ", e);
        }
        return Collections.emptyList();
    }

    /**
     * return a list of index names between the from and to instant
     *
     * @param fromInstant From time
     * @param toInstant To time
     * @param indexDateSpanUnits Date span unit (Y, M, D...)
     * @return List of index names
     * @throws Exception If index names cannot be determined
     */
    public static List<String> findIndexNames(String baseIndexName, Instant fromInstant, Instant toInstant, String indexDateSpanUnits) throws Exception {

        IndexNameHelper fromIndexNameHelper = new IndexNameHelper(baseIndexName, true, indexDateSpanUnits);
        IndexNameHelper toIndexNameHelper = new IndexNameHelper(baseIndexName, true, indexDateSpanUnits);

        String fromIndex = fromIndexNameHelper.getIndexName(fromInstant);
        String toIndex = toIndexNameHelper.getIndexName(toInstant);

        List<String> indexList = new ArrayList<>();
        if (fromInstant.isBefore(toInstant)) {
            if (fromIndex.equalsIgnoreCase(toIndex)) {
                indexList.add(fromIndex);
            } else {
                int indexDateSpanDayValue = -1;
                switch (indexDateSpanUnits) {
                    case "Y":
                        indexDateSpanDayValue = 365;
                        break;
                    case "M":
                        indexDateSpanDayValue = 30;
                        break;
                    case "W":
                        indexDateSpanDayValue = 7;
                        break;
                    case "D":
                        indexDateSpanDayValue = 1;
                        break;
                }
                indexList.add(fromIndex);
                while (!fromIndex.equalsIgnoreCase(toIndex)) {
                    fromInstant = fromInstant.plus(indexDateSpanDayValue, ChronoUnit.DAYS);
                    fromIndex = fromIndexNameHelper.getIndexName(fromInstant);
                    indexList.add(fromIndex);
                }
            }
        }
        return indexList;
    }
}
