package org.phoebus.service.saveandrestore.web.controllers;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

/**
 * Controller implementing endpoints to retrieve service info
 */
@RestController
public class InfoController extends BaseController {

    private final Logger logger = Logger.getLogger(InfoController.class.getName());

    @Value("${app.name:4.7.0}")
    private String name;

    @Value("${app.version:4.7.0}")
    private String version;

    @Autowired
    @Qualifier("restClient")
    Rest5Client restClient;

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     *
     * @return Information about the Save and Restore service
     */
    @GetMapping
    public String info() {

        Map<String, Object> saveRestoreServiceInfo = new LinkedHashMap<>();
        saveRestoreServiceInfo.put("name", name);
        saveRestoreServiceInfo.put("version", version);

        Map<String, String> elasticInfo = new LinkedHashMap<>();
        try {
            Request request = new Request("GET", "/");
            JsonNode response = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            elasticInfo.put("status", "Connected");
            elasticInfo.put("clusterName", response.path("cluster_name").asText(""));
            elasticInfo.put("clusterUuid", response.path("cluster_uuid").asText(""));
            elasticInfo.put("version", response.path("version").path("number").asText(""));
            //elasticInfo.put("elasticHost", host);
            //elasticInfo.put("elasticPort", String.valueOf(port));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create Save and Restore service info resource.", e);
            elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
        }
        saveRestoreServiceInfo.put("elastic", elasticInfo);

        saveRestoreServiceInfo.put("rootNodeID", ROOT_FOLDER_UNIQUE_ID);

         try {
            return objectMapper.writeValueAsString(saveRestoreServiceInfo);
        } catch (JacksonException e) {
            logger.log(Level.WARNING, "Failed to create Save and Restore service info resource.", e);
            return "Failed to gather Save and Restore service info";
        }
    }
}
