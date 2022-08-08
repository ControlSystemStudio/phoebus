package org.phoebus.service.saveandrestore.persistence.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

@Configuration
@PropertySource("classpath:application.properties")
public class ElasticConfig {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    // Read the Elastic index and type from the application.properties
    @Value("${elasticsearch.node.index:saveandrestore_node}")
    public String ES_NODE_INDEX;

    // Read the Elastic index and type from the application.properties
    @Value("${elasticsearch.folder_node.index:saveandrestore_folder_node}")
    public String ES_FOLDER_NODE_INDEX;

    @Value("${elasticsearch.folder_node.index:saveandrestore_tree}")
    public String ES_TREE_INDEX;

    @Value("${elasticsearch.folder_node.index:saveandrestore_configuration}")
    public String ES_CONFIGURATION_INDEX;

    @Value("${elasticsearch.folder_node.index:saveandrestore_snapshot}")
    public String ES_SNAPSHOT_INDEX;

    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;
    @Value("${elasticsearch.create.indices:true}")
    private String createIndices;

    private ElasticsearchClient client;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();
    private static final ObjectMapper mapper = new ObjectMapper();


    @Bean({"client"})
    public ElasticsearchClient getClient() {
        if (client == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(
                    httpClient,
                    new JacksonJsonpMapper()
            );
            client = new ElasticsearchClient(transport);
            esInitialized.set(!Boolean.parseBoolean(createIndices));
            if (esInitialized.compareAndSet(false, true)) {
                elasticIndexValidation(client);
                elasticIndexInitialization(client);
            }
        }
        return client;
    }

    /**
     * Create the indices and templates if they don't exist
     *
     * @param client
     */
    void elasticIndexValidation(ElasticsearchClient client) {

        // Tree index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/tree_node_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_TREE_INDEX)));
            if (!exits.value()) {
                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_TREE_INDEX).withJson(is)));
                logger.info("Created index: " + ES_TREE_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_TREE_INDEX, e);
        }

        // Save set index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/configuration_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_CONFIGURATION_INDEX)));
            if (!exits.value()) {
                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_CONFIGURATION_INDEX).withJson(is)));
                logger.info("Created index: " + ES_CONFIGURATION_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_CONFIGURATION_INDEX, e);
        }

        // Snapshot index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/snapshot_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_SNAPSHOT_INDEX)));
            if (!exits.value()) {
                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_SNAPSHOT_INDEX).withJson(is)));
                logger.info("Created index: " + ES_SNAPSHOT_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_SNAPSHOT_INDEX, e);
        }
    }

    /**
     * Create root node if it does not exist
     *
     * @param indexClient the elastic client instance used to create the default resources
     */
    private void elasticIndexInitialization(ElasticsearchClient indexClient) {

        try {
            if (!indexClient.exists(e -> e.index(ES_TREE_INDEX).id(ROOT_FOLDER_UNIQUE_ID)).value()) {
                Date now = new Date();
                Node node = Node.builder().nodeType(NodeType.FOLDER).uniqueId(ROOT_FOLDER_UNIQUE_ID).name("Root folder")
                        .userName("olog").created(now).lastModified(now).build();
                ESTreeNode elasticsearchTreeNode = new ESTreeNode();
                elasticsearchTreeNode.setNode(node);

                IndexRequest<ESTreeNode> indexRequest =
                        IndexRequest.of(i ->
                                i.index(ES_TREE_INDEX)
                                        .id(ROOT_FOLDER_UNIQUE_ID)
                                        .document(elasticsearchTreeNode)
                                        .refresh(Refresh.True));
                IndexResponse response = client.index(indexRequest);

                if (response.result().equals(Result.Created)) {
                    logger.log(Level.INFO, "Root node created");
                }
            }
            else{
                logger.log(Level.INFO, "Root node found, not creating it");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create root folder", e);
        }
    }
}
