package org.phoebus.alarm.logging.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.phoebus.alarm.logging.AlarmLoggingService;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
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
@SuppressWarnings("unused")
@Tag(name = "Search controller")
public class SearchController {

    static final Logger logger = Logger.getLogger(SearchController.class.getName());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${version:1.0.0}")
    private String version;

    /**
     * @return Information about the alarm logging service
     */
    @Operation(summary = "Get Service Info")
    @GetMapping
    public String info() {

        Map<String, Object> alarmLoggingServiceInfo = new LinkedHashMap<String, Object>();
        alarmLoggingServiceInfo.put("name", "Alarm logging Service");
        alarmLoggingServiceInfo.put("version", version);

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

    @Operation(summary = "Search alarms")
    @Parameters({
            @Parameter(name = "pv", description = "PV name", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "severity", description = "Alarm severity", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "message", description = "Alarm message", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "current_severity", description = "PV severity", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "current_message", description = "PV message", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "user", description = "User", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "host", description = "Host", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "command", description = "Command", schema = @Schema(type = "string"), required = false, example = "*"),
            @Parameter(name = "start", description = "Start time", schema = @Schema(type = "string"), required = false, example = "2024-06-12"),
            @Parameter(name = "end", description = "End time", schema = @Schema(type = "string"), required = false, example = "2024-06-14"),
    })
    @RequestMapping(value = "/search/alarm", method = RequestMethod.GET)
    public List<AlarmLogMessage> search(@Parameter(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        List<AlarmLogMessage> result = AlarmLogSearchUtil.search(ElasticClientHelper.getInstance().getClient(), allRequestParams);
        return result;
    }

    @Operation(summary = "Search alarms by PV name")
    @RequestMapping(value = "/search/alarm/pv/{pv}", method = RequestMethod.GET)
    public List<AlarmLogMessage> searchPv(@Parameter(description = "PV name") @PathVariable String pv) {
        Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put("pv", pv);
        List<AlarmLogMessage> result = AlarmLogSearchUtil.search(ElasticClientHelper.getInstance().getClient(), searchParameters);
        return result;
    }

    @Operation(summary = "Search alarm config")
    @Schema(name = "config", example = "/Accelerator/compteur", required = true)
    @Parameters({
            @Parameter(name = "config", description = "Config path", schema = @Schema(type = "string"), required = false, example = "/Accelerator/pvname"),
    })
    @RequestMapping(value = "/search/alarm/config", method = RequestMethod.GET)
    public List<AlarmLogMessage> searchConfig(@Parameter(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        if (allRequestParams == null ||
                allRequestParams.isEmpty() ||
                !allRequestParams.containsKey("config") ||
                allRequestParams.get("config").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<AlarmLogMessage> result = AlarmLogSearchUtil.searchConfig(ElasticClientHelper.getInstance().getClient(), allRequestParams);
        return result;
    }

    /**
     * Handles the /swagger-ui URL: redirects to /swagger-ui/index.html to avoid 500 response.
     *
     * @param response The {@link HttpServletResponse} to configure with a redirect (301).
     */
    @GetMapping("/swagger-ui")
    public void api(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.addHeader("Location", "/swagger-ui/index.html");
    }

}
