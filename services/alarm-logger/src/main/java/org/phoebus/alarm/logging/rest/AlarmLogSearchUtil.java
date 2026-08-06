package org.phoebus.alarm.logging.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
import java.util.Arrays;
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
        mapper = JsonMapper.builder().build();
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
     * @param restClient       low-level Rest5Client for performing requests
     * @param searchParameters search parameters
     * @return list of alarm state and config messages
     */
    public static List<AlarmLogMessage> search(ElasticsearchClient client,
                                               Rest5Client restClient,
                                               Map<String, String> searchParameters) {
        logger.fine("searching for alarm log entires : " +
                searchParameters.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining()));

        Instant fromInstant = Instant.EPOCH;
        Instant toInstant = Instant.now();

        // The maximum search result size
        int maxSize = prefs.getInt("es_max_size");
        final String indexDateSpanUnits = prefs.get("date_span_units");

        boolean configSet = false;
        boolean temporalSearch = false;

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        List<String> indexList = new ArrayList<>();
        List<String> alarmConfigs = new ArrayList<>();

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
                        DisMaxQuery.Builder alarmConfigQuery = new DisMaxQuery.Builder();
                        List<Query> alarmConfigQueries = new ArrayList<>();
                        // Construct a list of alarm config names
                        alarmConfigs =
                                Arrays.stream(parameter.getValue().split(",")).map(s -> s.trim()).collect(Collectors.toList());
                        for (String alarmConfig : alarmConfigs) {
                            alarmConfigQueries.add(Query.of(b -> b.bool(s -> s.should(
                                    Query.of(q -> q
                                            .wildcard(WildcardQuery.of(w -> w
                                                            .field("config").value("state:/" + alarmConfig + "*")
                                                    )
                                            )
                                    ),
                                    Query.of(q -> q
                                            .wildcard(WildcardQuery.of(w -> w
                                                            .field("config").value("config:/" + alarmConfig + "*")
                                                    )
                                            )
                                    )
                            ))));
                        }
                        Query configsQuery = alarmConfigQuery.queries(alarmConfigQueries).build()._toQuery();
                        boolQuery.must(configsQuery);
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
                                    .longNumber(n -> n
                                            .field("message_time")
                                            .gte(finalFromInstant.toEpochMilli())
                                            .lte(finalToInstant.toEpochMilli())
                                    )
                            ))
                    )
            );

            try {
                // "root" is empty string unless user specifies a list of alarm configs, in which case we can narrow down to
                // only matching alarm config indices.
                for(String alarmConfig : alarmConfigs){
                    indexList.addAll(findIndexNames(alarmConfig, fromInstant, toInstant, indexDateSpanUnits));
                }
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
            // Build the search request body as JSON for low-level API
            String requestBody = buildSearchJson(searchRequest);

            // Determine the target indices
            String indexParam = indexList.isEmpty() ? "" : String.join(",", indexList);
            String endpoint = indexParam.isEmpty() ? "/_search" : "/" + indexParam + "/_search";

            logger.fine("Search endpoint: " + endpoint);
            logger.fine("Search body: " + requestBody);

            // Execute search via low-level API (use POST for requests with body)
            Request request = new Request("POST", endpoint);
            request.setJsonEntity(requestBody);
            var response = restClient.performRequest(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // Parse the response
                JsonNode responseJson = mapper.readTree(response.getEntity().getContent());
                JsonNode hits = responseJson.get("hits").get("hits");

                List<AlarmLogMessage> results = new ArrayList<>();
                if (hits.isArray()) {
                    for (JsonNode hit : hits) {
                        JsonNode source = hit.get("_source");
                        if (source != null) {
                            try {
                                results.add(mapper.treeToValue(source, AlarmLogMessage.class));
                            } catch (JacksonException e) {
                                logger.log(Level.SEVERE, "Failed to parse the searched alarm log messages. " + source, e);
                            }
                        }
                    }
                }
                return results;
            } else {
                // Log the error response body
                String errorBody = "";
                try {
                    errorBody = new String(response.getEntity().getContent().readAllBytes());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not read error response body", e);
                }
                logger.log(Level.SEVERE, "Search failed with status code: " + response.getStatusCode() +
                    "\nEndpoint: " + endpoint +
                    "\nRequest body: " + requestBody +
                    "\nError response: " + errorBody);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm logs ", e);
        }
        return Collections.emptyList();
    }

    /**
     * Return the latest alarm config message associated with 'config'
     *
     * @param client           elastic client
     * @param restClient       low-level Rest5Client for performing requests
     * @param allRequestParams the wildcard pattern which matches the 'config'
     * @return last alarm config message for the given 'config'
     */
    public static List<AlarmLogMessage> searchConfig(ElasticsearchClient client,
                                                      Rest5Client restClient,
                                                      Map<String, String> allRequestParams) {
        String configString = allRequestParams.get("config");
        // Determine which alarm config to specify as Elasticsearch index, convert to lower case as
        // indices are created using lower case.
        String alarmConfig = configString.split("/")[1].toLowerCase();

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
            // Build the search request body as JSON for low-level API
            String requestBody = buildSearchJson(searchRequest);
            String endpoint = "/" + alarmConfig + "_alarms_config_*/_search";

            logger.fine("Search config endpoint: " + endpoint);
            logger.fine("Search config body: " + requestBody);

            // Execute search via low-level API (use POST for requests with body)
            Request request = new Request("POST", endpoint);
            request.setJsonEntity(requestBody);
            var response = restClient.performRequest(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // Parse the response
                JsonNode responseJson = mapper.readTree(response.getEntity().getContent());
                JsonNode hits = responseJson.get("hits").get("hits");

                List<AlarmLogMessage> results = new ArrayList<>();
                if (hits.isArray()) {
                    for (JsonNode hit : hits) {
                        JsonNode source = hit.get("_source");
                        if (source != null) {
                            try {
                                results.add(mapper.treeToValue(source, AlarmLogMessage.class));
                            } catch (JacksonException e) {
                                logger.log(Level.SEVERE, "Failed to parse the searched alarm config messages. " + source, e);
                            }
                        }
                    }
                }
                return results;
            } else {
                // Log the error response body
                String errorBody = "";
                try {
                    errorBody = new String(response.getEntity().getContent().readAllBytes());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not read error response body", e);
                }
                logger.log(Level.SEVERE, "Search config failed with status code: " + response.getStatusCode() +
                    "\nEndpoint: " + endpoint +
                    "\nRequest body: " + requestBody +
                    "\nError response: " + errorBody);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm config logs ", e);
        }
        return Collections.emptyList();
    }

    /**
     * return a list of index names between the from and to instant
     * @param baseIndexName A lower case index base name, which should be same as an alarm config name.
     * @param fromInstant        From time
     * @param toInstant          To time
     * @param indexDateSpanUnits Date span unit (Y, M, D...)
     * @return List of index names
     * @throws Exception If index names cannot be determined
     */
    public static List<String> findIndexNames(String baseIndexName, Instant fromInstant, Instant toInstant, String indexDateSpanUnits) throws Exception {
        List<String> indexList = new ArrayList<>();

        IndexNameHelper fromIndexNameHelper = new IndexNameHelper(baseIndexName.toLowerCase() + "*", true, indexDateSpanUnits);
        IndexNameHelper toIndexNameHelper = new IndexNameHelper(baseIndexName.toLowerCase() + "*", true, indexDateSpanUnits);

        String fromIndex = fromIndexNameHelper.getIndexName(fromInstant);
        String toIndex = toIndexNameHelper.getIndexName(toInstant);

        if (fromInstant.isBefore(toInstant)) {
            if (fromIndex.equalsIgnoreCase(toIndex)) {
                indexList.add(fromIndex);
            } else {
                int indexDateSpanDayValue = getDateSpanInDays(indexDateSpanUnits);
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

    /**
     *
     * @param indexDateSpanUnits A single char string from [Y, M, W, D]
     * @return Number of days corresponding to the unit, or -1 if the input does not match
     * supported chars.
     */
    public static int getDateSpanInDays(String indexDateSpanUnits){
        switch (indexDateSpanUnits) {
            case "Y":
                return 365;
            case "M":
                return 30;
            case "W":
                return 7;
            case "D":
                return 1;
            default:
                return -1;
        }
    }

    /**
     * Helper method to convert SearchRequest to JSON string for low-level API
     * This works by using Jackson to serialize the SearchRequest object tree to JSON
     * NOTE: We exclude 'index' from the JSON since indices are specified in the URL path
     */
    private static String buildSearchJson(SearchRequest searchRequest) throws IOException {
        // Serialize the entire request first to get all fields
        String fullJson = mapper.writeValueAsString(searchRequest);
        JsonNode fullNode = mapper.readTree(fullJson);

        // Build a new JSON object with only the fields Elasticsearch expects in the body
        Map<String, Object> searchBody = new java.util.LinkedHashMap<>();

        // Query - only include if present
        if (fullNode.has("query") && !fullNode.get("query").isNull()) {
            searchBody.put("query", mapper.convertValue(fullNode.get("query"), Object.class));
        }

        // Size - only include if present
        if (fullNode.has("size") && !fullNode.get("size").isNull()) {
            searchBody.put("size", fullNode.get("size").asInt());
        }

        // Sort - only include if present
        if (fullNode.has("sort") && !fullNode.get("sort").isNull() && fullNode.get("sort").isArray()) {
            searchBody.put("sort", mapper.convertValue(fullNode.get("sort"), Object.class));
        }

        String result = mapper.writeValueAsString(searchBody);
        logger.fine("Built search JSON: " + result);
        return result;
    }

    private static Object toMap(Query query) throws IOException {
        // Serialize the Query object to a map via JSON round-trip
        String json = mapper.writeValueAsString(query);
        return mapper.readValue(json, Object.class);
    }

    private static List<?> toList(java.util.List<?> list) throws IOException {
        // Serialize the list to map via JSON round-trip
        String json = mapper.writeValueAsString(list);
        return mapper.readValue(json, List.class);
    }
}
