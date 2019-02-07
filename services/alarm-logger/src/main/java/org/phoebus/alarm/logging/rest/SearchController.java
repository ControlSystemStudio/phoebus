package org.phoebus.alarm.logging.rest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.phoebus.alarm.logging.AlarmLoggingService;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.framework.preferences.PreferencesReader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * A REST service for quering the alarm message history
 * @author Kunal Shroff
 *
 */
@RestController
@RequestMapping("/alarm-history")
public class SearchController {

    private static final Logger logger = Logger.getLogger(SearchController.class.getName());

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final PreferencesReader prefs = new PreferencesReader(AlarmLoggingService.class, "/alarm_logging_service.properties");

    @RequestMapping(value = "/search/alarm", method = RequestMethod.GET)
    public List<AlarmStateMessage> search(@RequestParam Map<String, String> allRequestParams) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        allRequestParams.forEach((k, v) -> {
            query.must(QueryBuilders.wildcardQuery(k, v));
        });
        return esAlarmSearch(query);
    }

    @RequestMapping(value = "/search/alarm/{pv}", method = RequestMethod.GET)
    public List<AlarmStateMessage> searchPv(@PathVariable String pv) {
        QueryBuilder query = QueryBuilders.wildcardQuery("pv", pv);
        return esAlarmSearch(query);
    }

    @RequestMapping(value = "/search/alarm/{config}", method = RequestMethod.GET)
    public List<AlarmStateMessage> searchConfig(@PathVariable String config) {
        QueryBuilder query = QueryBuilders.wildcardQuery("config", config);
        return esAlarmSearch(query);
    }

    private List<AlarmStateMessage> esAlarmSearch(QueryBuilder query) {
        RestHighLevelClient client = ElasticClientHelper.getInstance().getClient();

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder = sourceBuilder.query(query);
        sourceBuilder.size(prefs.getInt("es_max_size"));
        sourceBuilder.sort("time", SortOrder.DESC);
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
                                JsonNode time = ((ObjectNode) root).remove("time");
                                JsonNode message_time = ((ObjectNode) root).remove("message_time");
                                AlarmStateMessage alarmStateMessage = objectMapper.readValue(root.traverse(),
                                        AlarmStateMessage.class);
                                if (time != null) {
                                    Instant instant = LocalDateTime.parse(time.asText(), formatter)
                                            .atZone(ZoneId.systemDefault()).toInstant();
                                    alarmStateMessage.setInstant(instant);
                                }
                                if (message_time != null) {
                                    Instant instant = LocalDateTime.parse(message_time.asText(), formatter)
                                            .atZone(ZoneId.systemDefault()).toInstant();
                                    alarmStateMessage.setMessage_time(instant);
                                }
                                return alarmStateMessage;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Failed to search for alarm logs ", e);
                                return null;
                            }
                        }
                    }).collect(Collectors.toList());
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for alarm logs ", e);
        }
        return Collections.emptyList();
    }

}
