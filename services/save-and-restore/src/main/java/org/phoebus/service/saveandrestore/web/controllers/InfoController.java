package org.phoebus.service.saveandrestore.web.controllers;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import jakarta.annotation.PostConstruct;
import org.phoebus.applications.saveandrestore.model.ServiceInfo;
import org.phoebus.util.elasticsearch.ElasticInfo;
import org.phoebus.util.elasticsearch.IndexStats;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Controller implementing endpoints to retrieve service info
 */
@RestController
public class InfoController extends BaseController {

    private final Logger logger = Logger.getLogger(InfoController.class.getName());

    @Value("${app.version:4.7.0}")
    private String version;

    @Autowired
    @Qualifier("restClient")
    Rest5Client restClient;

    @Value("${serverRestoreAllowed:false}")
    private boolean serverRestoreAllowed;


    @SuppressWarnings("unused")
    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.snapshot_node.index:saveandrestore_snapshot}")
    private String ES_SNAPSHOT_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    private String ES_COMPOSITE_SNAPSHOT_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.filter.index:saveandrestore_filter}")
    private String ES_FILTER_INDEX;

    private String[] indices;

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @PostConstruct
    public void init(){
        indices = new String[]{ES_TREE_INDEX,
                ES_CONFIGURATION_INDEX,
                ES_SNAPSHOT_INDEX,
                ES_COMPOSITE_SNAPSHOT_INDEX,
                ES_FILTER_INDEX};
    }

    /**
     *
     * @return Information about the Save and Restore service and Elasticsearch index metrics
     */
    @GetMapping
    public ServiceInfo info() {

        List<IndexStats> indexMetrics = new ArrayList<>(indices.length);

        try {
            Request request;
            for(String indexName : indices){
                request = new Request("GET", "/" + indexName + "/_stats");
                JsonNode docsNode = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent())
                        .path("indices").path(indexName).path("primaries").path("docs");
                int documentCount = docsNode.path("count").asInt();
                int deletedDocumentCount = docsNode.path("deleted").asInt();
                long indexSize = docsNode.path("total_size_in_bytes").asLong();
                indexMetrics.add(new IndexStats(indexName, documentCount, deletedDocumentCount, indexSize));
            }
            request = new Request("GET", "/");
            JsonNode response = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            String elasticVersion = response.path("version").path("number").asString();
            ElasticInfo elasticInfo = new ElasticInfo(elasticVersion, indexMetrics);
            return new ServiceInfo(version, serverRestoreAllowed, elasticInfo);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create query for Elasticsearch statistics.", e);
            return new ServiceInfo(version, serverRestoreAllowed, new ElasticInfo());
        }
    }
}
