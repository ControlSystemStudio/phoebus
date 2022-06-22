package org.phoebus.alarm.logging.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.alarm.logging.AlarmLoggingService;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.framework.preferences.PreferencesReader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A REST service for quering the alarm message history
 *
 * @author Kunal Shroff
 */
@RestController
@RequestMapping("/alarm-history")
public class SearchController {

    private static final Logger logger = Logger.getLogger(SearchController.class.getName());
    private final PreferencesReader prefs = new PreferencesReader(AlarmLoggingService.class, "/application.properties");

    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     *
     * @return Information about the alarm logging service
     */
    @GetMapping
    public String info() {

        Map<String, Object> alarmLoggingServiceInfo = new LinkedHashMap<String, Object>();
        alarmLoggingServiceInfo.put("name", "Alarm logging Service");
        //alarmLoggingServiceInfo.put("version", version);

        Map<String, String> elasticInfo = new LinkedHashMap<String, String>();
        try {
            ElasticsearchClient client = ElasticClientHelper.getInstance().getClient();
            InfoResponse response = client.info();

            elasticInfo.put("status", "Connected");
            elasticInfo.put("clusterName", response.clusterName());
            elasticInfo.put("clusterUuid", response.clusterUuid());
            ElasticsearchVersionInfo version = response.version();
            elasticInfo.put("version", version.toString());
        } catch (IOException e) {
            AlarmLoggingService.logger.log(Level.WARNING, "Failed to create Alarm Logging service info resource.", e);
            elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
        }
        alarmLoggingServiceInfo.put("elastic", elasticInfo);
        try {
            return objectMapper.writeValueAsString(alarmLoggingServiceInfo);
        } catch (JsonProcessingException e) {
            AlarmLoggingService.logger.log(Level.WARNING, "Failed to create Alarm Logging service info resource.", e);
            return "Failed to gather Alarm Logging service info";
        }
    }

    @RequestMapping(value = "/search/alarm", method = RequestMethod.GET)
    public List<AlarmStateMessage> search(@RequestParam Map<String, String> allRequestParams) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        List<Query> queries = new ArrayList<>();
        allRequestParams.forEach((key, value) -> queries.add(
                        Query.of(q -> q
                                .wildcard(w -> w
                                        .field(key)
                                        .value(value)
                                )
                        )
                )
        );
        boolQuery.must(queries);
        return esAlarmSearch(boolQuery.build());
    }

    @RequestMapping(value = "/search/alarm/{pv}", method = RequestMethod.GET)
    public List<AlarmStateMessage> searchPv(@PathVariable String pv) {
        return esAlarmSearch(BoolQuery.of(b -> b
                        .must(
                                Query.of(q -> q
                                        .wildcard(w -> w
                                                .field("pv")
                                                .value(pv)
                                        )
                                )
                        )
                )
        );
    }

    @RequestMapping(value = "/search/alarm/{config}", method = RequestMethod.GET)
    public List<AlarmStateMessage> searchConfig(@PathVariable String config) {
        return esAlarmSearch(BoolQuery.of(b -> b
                        .must(
                                Query.of(q -> q
                                        .wildcard(w -> w
                                                .field("config")
                                                .value(config)
                                        )
                                )
                        )
                )
        );
    }

    private List<AlarmStateMessage> esAlarmSearch(BoolQuery query) {
        ElasticsearchClient client = ElasticClientHelper.getInstance().getClient();
        List<AlarmStateMessage> result = new ArrayList<>();
        try {
            SearchResponse<AlarmStateMessage> response = client.search(
                    SearchRequest.of(req -> req
                            .size(prefs.getInt("es_max_size"))
                            .query(Query.of(
                                            q -> q.bool(query)
                                    )
                            )
                            .sort(SortOptions.of(s -> s
                                            .field(FieldSort.of(f -> f
                                                            .field("time")
                                                            .order(SortOrder.Desc)
                                                    )
                                            )
                                    )
                            )
                    ),
                    AlarmStateMessage.class
            );
            response.hits().hits().forEach(hit -> result.add(hit.source()));
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm logs ", e);
        }
        return Collections.emptyList();
    }

}
