package org.phoebus.alarm.logging.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.alarm.logging.AlarmLoggingService;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A REST service for querying the alarm message history
 *
 * @author Kunal Shroff
 */
@RestController
public class SearchController {

    static final Logger logger = Logger.getLogger(SearchController.class.getName());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
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
    public List<AlarmLogMessage> search(@RequestParam Map<String, String> allRequestParams) {
        List<AlarmLogMessage> result = AlarmLogSearchUtil.search(ElasticClientHelper.getInstance().getClient(), allRequestParams);
        return result;
    }

    @RequestMapping(value = "/search/alarm/pv/{pv}", method = RequestMethod.GET)
    public List<AlarmLogMessage> searchPv(@PathVariable String pv) {
        Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put("pv", pv);
        List<AlarmLogMessage> result = AlarmLogSearchUtil.search(ElasticClientHelper.getInstance().getClient(), searchParameters);
        return result;
    }

    @RequestMapping(value = "/search/alarm/config/{config}", method = RequestMethod.GET)
    public List<AlarmLogMessage> searchConfig(@PathVariable String config) {
        List<AlarmLogMessage> result = AlarmLogSearchUtil.searchConfig(ElasticClientHelper.getInstance().getClient(), config);
        return result;
    }

}
