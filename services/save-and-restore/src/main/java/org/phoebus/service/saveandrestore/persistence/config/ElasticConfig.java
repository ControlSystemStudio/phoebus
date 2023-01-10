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
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.json.VTypeDeserializer;
import org.phoebus.applications.saveandrestore.model.json.VTypeSerializer;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore"})
@PropertySource("classpath:application.properties")
public class ElasticConfig {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    public String ES_TREE_INDEX;

    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    public String ES_CONFIGURATION_INDEX;

    @Value("${elasticsearch.snapshot_node.index:saveandrestore_snapshot}")
    public String ES_SNAPSHOT_INDEX;

    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    public String ES_COMPOSITE_SNAPSHOT_INDEX;

    @Value("${elasticsearch.filter.index:saveandrestore_filter}")
    public String ES_FILTER_INDEX;

    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    private ElasticsearchClient client;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    public static final Node ROOT_NODE;

    static{
        Date now = new Date();
        ROOT_NODE = Node.builder().nodeType(NodeType.FOLDER).uniqueId(ROOT_FOLDER_UNIQUE_ID).name("Root folder")
                .userName("anonymous").created(now).lastModified(now).build();

    }

    @Bean({"client"})
    public ElasticsearchClient getClient() {
        if (client == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(VType.class, new VTypeSerializer());
            module.addDeserializer(VType.class, new VTypeDeserializer());

            jacksonJsonpMapper.objectMapper().registerModule(module);


            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(
                    httpClient,
                    jacksonJsonpMapper
            );
            client = new ElasticsearchClient(transport);
            //esInitialized.set(!Boolean.parseBoolean(createIndices));
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

        // Configuration index
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

        // SnapshotData index
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

        // Composite snapshot index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/composite_snapshot_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_COMPOSITE_SNAPSHOT_INDEX)));
            if (!exits.value()) {
                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_COMPOSITE_SNAPSHOT_INDEX).withJson(is)));
                logger.info("Created index: " + ES_COMPOSITE_SNAPSHOT_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_COMPOSITE_SNAPSHOT_INDEX, e);
        }

        // Filter index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/filter_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_FILTER_INDEX)));
            if (!exits.value()) {
                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_FILTER_INDEX).withJson(is)));
                logger.info("Created index: " + ES_FILTER_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_FILTER_INDEX, e);
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
                ESTreeNode elasticsearchTreeNode = new ESTreeNode();
                elasticsearchTreeNode.setNode(ROOT_NODE);

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
            } else {
                logger.log(Level.INFO, "Root node found, not creating it");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create root folder", e);
        }
    }

    @Bean
    public SearchUtil searchUtil(){
        return new SearchUtil();
    }
}
