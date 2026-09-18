package org.phoebus.service.saveandrestore.persistence.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.*;
import org.apache.hc.core5.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.json.VTypeDeserializer;
import org.phoebus.applications.saveandrestore.model.json.VTypeSerializer;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

/**
 * Configures the Elasticsearch environment, e.g. creates indices if they do not exist.
 */
@Configuration
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore"})
@PropertySource("classpath:application.properties")
public class ElasticConfig {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    private String ES_TREE_INDEX;

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

    @SuppressWarnings("unused")
    @Value("${elasticsearch.network.host:localhost}")
    private String host;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    @Value("${elasticsearch.authorization.header:}")
    private String authorizationHeader;

    @Value("${elasticsearch.authorization.username:}")
    private String username;

    @Value("${elasticsearch.authorization.password}")
    private String password;

    @Value("${elasticsearch.http.protocol:http}")
    private String protocol;


    private ElasticsearchClient client;
    private Rest5Client restClient;
    private ObjectMapper objectMapper;

    private static final Node ROOT_NODE;

    static{
        Date now = new Date();
        ROOT_NODE = Node.builder().nodeType(NodeType.FOLDER).uniqueId(ROOT_FOLDER_UNIQUE_ID).name("Root folder")
                .userName("anonymous").created(now).lastModified(now).build();

    }

    /**
     *
     * @return The {@link ElasticsearchClient} bean.
     */
    @Bean({"client"})
    public ElasticsearchClient getClient() {
        if (client == null) {
            Rest5ClientBuilder clientBuilder =
                    Rest5Client.builder(new HttpHost(protocol, host, port));

            // Configure authentication
            if (!authorizationHeader.isEmpty()) {
                clientBuilder.setDefaultHeaders(
                        new Header[]{new BasicHeader("Authorization", authorizationHeader)});
                if (!username.isEmpty() || !password.isEmpty()) {
                    logger.warning("elasticsearch.authorization_header is set, ignoring elasticsearch.username and elasticsearch.password.");
                }
            } else if (!username.isEmpty() || !password.isEmpty()) {
                final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(new HttpHost(protocol, host, port)),
                        new UsernamePasswordCredentials(username, password.toCharArray()));
                clientBuilder.setHttpClientConfigCallback(
                        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            Rest5Client httpClient = clientBuilder.build();
            SimpleModule module = new SimpleModule();
            module.addSerializer(VType.class, new VTypeSerializer());
            module.addDeserializer(VType.class, new VTypeDeserializer());
            JsonMapper jsonMapper = JsonMapper.builder()
                    .addModule(module)
                    .build();
            objectMapper = jsonMapper;
            Jackson3JsonpMapper jackson3JsonpMapper = new Jackson3JsonpMapper(jsonMapper);

            RequestOptions.Builder restClientOptionsBuilder = RequestOptions.DEFAULT.toBuilder();

            restClientOptionsBuilder
                    .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .addHeader(HttpHeaders.ACCEPT, "application/json");

            Rest5ClientOptions restClientOptions = new Rest5ClientOptions.Builder(restClientOptionsBuilder).build();

            ElasticsearchTransport transport = new Rest5ClientTransport(
                    httpClient,
                    jackson3JsonpMapper,
                    restClientOptions);
            restClient = httpClient;
            client = new ElasticsearchClient(transport);
            // Use low-level requests for index/bootstrap operations to keep behavior stable
            // with ES 8 backends while staying on elasticsearch-java 9 + Jackson 3.
            elasticIndexValidation();
            elasticIndexInitialization();
        }
        return client;
    }

    /**
     * Create the indices and templates if they don't exist
     *
     */
    void elasticIndexValidation() {
        ensureIndex(ES_TREE_INDEX, "/tree_node_mapping.json");
        ensureIndex(ES_CONFIGURATION_INDEX, "/configuration_mapping.json");
        ensureIndex(ES_SNAPSHOT_INDEX, "/snapshot_mapping.json");
        ensureIndex(ES_COMPOSITE_SNAPSHOT_INDEX, "/composite_snapshot_mapping.json");
        ensureIndex(ES_FILTER_INDEX, "/filter_mapping.json");
    }

    /**
     * Create root node if it does not exist
     *
     */
    private void elasticIndexInitialization() {

        try {
            if (!documentExists(ES_TREE_INDEX, ROOT_FOLDER_UNIQUE_ID)) {
                String payload = buildRootNodePayload();

                Request request = new Request("PUT", "/" + encodePathSegment(ES_TREE_INDEX)
                        + "/_doc/" + encodePathSegment(ROOT_FOLDER_UNIQUE_ID));
                request.addParameter("refresh", "true");
                request.setJsonEntity(payload);

                int statusCode = restClient.performRequest(request).getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("Created root node in index '" + ES_TREE_INDEX + "'.");
                } else {
                    logger.warning("Failed to create root node in index '" + ES_TREE_INDEX
                            + "' (HTTP " + statusCode + "). endpoint=" + request.getEndpoint()
                            + ", payload=" + payload);
                }
            } else {
                logger.info("Root node already exists in index '" + ES_TREE_INDEX + "'.");
            }
        } catch (ResponseException e) {
            int statusCode = e.getResponse().getStatusCode();
            logger.warning("Failed to create root node in index '" + ES_TREE_INDEX
                    + "' (HTTP " + statusCode + "): " + readExceptionBody(e));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to initialize root node in index '" + ES_TREE_INDEX + "'.", e);
        }
    }

    private void ensureIndex(String indexName, String mappingResource) {
        try {
            if (indexExists(indexName)) {
                return;
            }

            String mapping = readResource(mappingResource);
            Request request = new Request("PUT", "/" + encodePathSegment(indexName));
            request.setJsonEntity(mapping);
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Created index '" + indexName + "'.");
            } else {
                logger.warning("Failed to create index '" + indexName + "' (HTTP " + statusCode + ").");
            }
        } catch (ResponseException e) {
            int statusCode = e.getResponse().getStatusCode();
            if (statusCode == 400 && e.getMessage().contains("resource_already_exists_exception")) {
                logger.info("Index '" + indexName + "' already exists.");
                return;
            }
            logger.log(Level.WARNING, "Failed to create index '" + indexName + "' (HTTP " + statusCode + ").", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index '" + indexName + "'.", e);
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        try {
            Request request = new Request("HEAD", "/" + encodePathSegment(indexName));
            int statusCode = restClient.performRequest(request).getStatusCode();
            return statusCode >= 200 && statusCode < 300;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private boolean documentExists(String indexName, String documentId) throws IOException {
        try {
            Request request = new Request("HEAD", "/" + encodePathSegment(indexName)
                    + "/_doc/" + encodePathSegment(documentId));
            int statusCode = restClient.performRequest(request).getStatusCode();
            return statusCode >= 200 && statusCode < 300;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private static String readResource(String resourcePath) throws IOException {
        try (InputStream is = ElasticConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildRootNodePayload() throws IOException {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("created", ROOT_NODE.getCreated().getTime());
        node.put("lastModified", ROOT_NODE.getLastModified().getTime());
        node.put("name", ROOT_NODE.getName());
        node.put("nodeType", ROOT_NODE.getNodeType().name());
        node.put("uniqueId", ROOT_NODE.getUniqueId());
        node.put("userName", ROOT_NODE.getUserName());

        Map<String, Object> rootDocument = new LinkedHashMap<>();
        rootDocument.put("childNodes", new ArrayList<>());
        rootDocument.put("node", node);
        return objectMapper.writeValueAsString(rootDocument);
    }

    private static String readExceptionBody(ResponseException e) {
        try {
            if (e.getResponse().getEntity() == null) {
                return "<no response body>";
            }
            return EntityUtils.toString(e.getResponse().getEntity(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "<failed to read response body: " + ex.getMessage() + ">";
        }
    }

    /**
     *
     * @return A {@link SearchUtil} instance.
     */
    @SuppressWarnings("unused")
    @Bean
    public SearchUtil searchUtil(){
        return new SearchUtil();
    }

    @Bean("restClient")
    public Rest5Client getRestClient() {
        if (restClient == null) {
            getClient();
        }
        return restClient;
    }

    @Bean("elasticObjectMapper")
    public ObjectMapper elasticObjectMapper() {
        if (objectMapper == null) {
            getClient();
        }
        return objectMapper;
    }
}
