/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import jakarta.json.stream.JsonGenerator;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for {@link ESTreeNode} objects.
 */
@Repository
public class ElasticsearchTreeRepository implements CrudRepository<ESTreeNode, String> {

    private static final Logger logger = Logger.getLogger(ElasticsearchTreeRepository.class.getName());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    private String ES_TREE_INDEX;

    /**
     * Used to determine if the {@link ESTreeNode} is saved or updated in connection to a migration
     * operation. If so, the last modified date should be preserved.
     */
    private boolean migrationContext;

    @Autowired
    @Qualifier("restClient")
    private Rest5Client restClient;

    @Autowired
    @Qualifier("elasticObjectMapper")
    private ObjectMapper objectMapper;

    @SuppressWarnings("unused")
    @Autowired
    private SearchUtil searchUtil;

    /**
     * Constructor checking if this is called in connection to a data migration (from legacy RDB) operation.
     */
    public ElasticsearchTreeRepository() {
        String isMigrationContext = System.getProperty("migrationContext");
        if (isMigrationContext != null) {
            try {
                migrationContext = Boolean.parseBoolean(isMigrationContext);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot parse migration context value " + isMigrationContext + " as boolean");
            }
        }
    }

    /**
     * Saves an {@link ESTreeNode} object.
     * The <code>uniqueId</code> field of the {@link org.phoebus.applications.saveandrestore.model.Node}
     * field is set if <code>null</code> or empty. This is needed for data migration purposes.
     * The same applies also to the <code>created</code> field.
     *
     * @param elasticTreeNode An {@link ESTreeNode} object
     * @return An {@link ESTreeNode} as persisted in Elasticsearch.
     */
    @Override
    public <S extends ESTreeNode> S save(@NonNull S elasticTreeNode) {
        Date now = new Date();
        try {
            if (elasticTreeNode.getNode().getCreated() == null) {
                elasticTreeNode.getNode().setCreated(now);
            }

            if (elasticTreeNode.getNode().getUniqueId() == null || elasticTreeNode.getNode().getUniqueId().isEmpty()) {
                elasticTreeNode.getNode().setUniqueId(UUID.randomUUID().toString());
            }

            // Set last modified date
            if (!migrationContext) {
                elasticTreeNode.getNode().setLastModified(now);
            }

            String id = elasticTreeNode.getNode().getUniqueId();
            Request request = new Request("PUT", "/" + encodePathSegment(ES_TREE_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            request.setJsonEntity(buildTreeNodePayload(elasticTreeNode));
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return (S) getTreeNodeById(id).orElse(null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save ESTreeNode object: " + elasticTreeNode, e);
            throw new RuntimeException("ESTreeNode object: " + elasticTreeNode.getNode().getName());
        }
        return null;
    }

    /**
     * Not implemented.
     *
     * @param entities {@link Iterable} of {@link ESTreeNode}s.
     * @throws RuntimeException as it is not implemented.
     */
    @Override
    public <S extends ESTreeNode> Iterable<S> saveAll(@NonNull Iterable<S> entities) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @param id Non-null unique id of a {@link ESTreeNode}.
     * @return Optional object.
     */
    @Override
    public Optional<ESTreeNode> findById(@NonNull String id) {
        try {
            Optional<ESTreeNode> result = getTreeNodeById(id);
            if (result.isEmpty()) {
                throw new NodeNotFoundException("ESTreeNode with id " + id + " not found.");
            }
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve ESTreeNode with id: " + id, e);
            throw new RuntimeException("Failed to ESTreeNode with id: " + id);
        }
    }

    /**
     * @param id Non-null unique id of a {@link ESTreeNode}.
     * @return <code>true</code> if document is found, otherwise <code>false</code>
     */
    @Override
    public boolean existsById(@NonNull String id) {

        try {
            return documentExists(ES_TREE_INDEX, id);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if ESTreeNode with id " + id + " exists");
        }
        return false;
    }

    /**
     *
     * @return Always <code>null</code>.
     */
    @Override
    public Iterable<ESTreeNode> findAll() {
        return null;
    }

    /**
     * Retrieves {@link ESTreeNode}s corresponding to the provided list of unique ids.
     * <p>
     * Note that if a unique id is not found in the index, this method will <b>not</b> throw an {@link Exception}. The returned
     * {@link List} of {@link ESTreeNode} may consequently be shorter than the input list of unique ids, or
     * even empty. It is hence up to the callee to determine how to handle a potential discrepancy.
     *
     * @param uniqueIds A {@link Iterable} of unique ids. If <code>null</code>, an empty {@link List} is
     *                  returned.
     * @return A (potentially empty) {@link List} of existing {@link ESTreeNode}s.
     */
    @Override
    public Iterable<ESTreeNode> findAllById(Iterable<String> uniqueIds) {
        if (uniqueIds == null || !uniqueIds.iterator().hasNext()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        uniqueIds.forEach(ids::add);
        try {
            return mgetTreeNodes(ids);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve multiple nodes");
            throw new NodeNotFoundException("Failed to retrieve multiple nodes");
        }
    }

    /**
     *
     * @return Always 0.
     */
    @Override
    public long count() {
        return 0;
    }

    /**
     * Deletes a {@link ESTreeNode}.
     * @param id unique {@link org.phoebus.applications.saveandrestore.model.Node} id.
     */
    @Override
    public void deleteById(@NonNull String id) {
        try {
            Request request = new Request("DELETE", "/" + encodePathSegment(ES_TREE_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            String result = body.path("result").asText("");
            if ("deleted".equalsIgnoreCase(result)) {
                logger.log(Level.WARNING, "Node with id " + id + " deleted.");
            } else {
                logger.log(Level.WARNING, "Node with id " + id + " NOT deleted.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete node with id: " + id, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ESTreeNode entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends ESTreeNode> entities) {

    }

    @Override
    public void deleteAll() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_TREE_INDEX) + "/_delete_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            logger.log(Level.INFO, "Deleted " + body.path("deleted").asLong(0L) + " ESTreeNode objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all ESTreeNode objects", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Locates the parent of a node.
     *
     * @param uniqueId The non-null unique id of the node for which to search for the parent.
     * @return An {@link ESTreeNode} object if the parent node can be located, otherwise
     * <code>null</code>. Note that if the unique id is found in multiple {@link ESTreeNode}
     * documents, then this indicates a data integrity problem. If this happens this method returns
     * <code>null</code>.
     */
    public ESTreeNode getParentNode(String uniqueId) {
        try {
            Map<String, Object> query = Map.of(
                    "query", Map.of("bool", Map.of("must", List.of(Map.of("term", Map.of("childNodes", uniqueId))))),
                    "timeout", "60s",
                    "size", 2
            );
            JsonNode response = executeSearch("/" + encodePathSegment(ES_TREE_INDEX) + "/_search",
                    objectMapper.writeValueAsString(query));
            List<ESTreeNode> result = parseSearchHits(response, ESTreeNode.class);
            if (!result.isEmpty()) {
                if (result.size() > 1) {
                    logger.log(Level.SEVERE, "Node " + uniqueId + " is child node of multiple nodes. Should not happen!");
                    throw new RuntimeException("Node " + uniqueId + " contained in multiple parent nodes. Should not happen!");
                }
                return result.get(0);
            } else {
                throw new NodeNotFoundException("Unable to locate parent node for unique id " + uniqueId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves all tags across all nodes in the tree index.
     *
     * @param goldenOnly If <code>true</code> only golden tags are considered.
     * @return A potentially empty list of {@link Tag}s.
     */
    public List<ESTreeNode> searchNodesForTag(boolean goldenOnly) {
        try {
            Map<String, Object> innerQuery;
            if (!goldenOnly) {
                innerQuery = Map.of("exists", Map.of("field", "node.tags"));
            } else {
                innerQuery = Map.of("match", Map.of("node.tags.name", Tag.GOLDEN));
            }

            Map<String, Object> query = Map.of(
                    "query", Map.of("bool", Map.of("must", List.of(
                            Map.of("nested", Map.of(
                                    "path", "node",
                                    "query", Map.of("nested", Map.of("path", "node.tags", "query", innerQuery))
                            ))
                    ))),
                    "timeout", "60s",
                    "size", 1000
            );

            JsonNode response = executeSearch("/" + encodePathSegment(ES_TREE_INDEX) + "/_search",
                    objectMapper.writeValueAsString(query));
            return parseSearchHits(response, ESTreeNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs search for {@link ESTreeNode}s matching the search parameters
     * @param searchParameters {@link MultiValueMap} of search parameters.
     * @return A {@link SearchResult} with {@link org.phoebus.applications.saveandrestore.model.Node} objects matching
     * the search criteria. May of course be empty.
     */
    public SearchResult search(MultiValueMap<String, String> searchParameters) {

        SearchRequest searchRequest = searchUtil.buildSearchRequest(searchParameters);
        try {
            String endpoint = "/" + encodePathSegment(ES_TREE_INDEX) + "/_search";
            String payload = serializeSearchRequest(searchRequest);
            JsonNode searchResponse = executeSearch(endpoint, payload);
            List<ESTreeNode> hits = parseSearchHits(searchResponse, ESTreeNode.class);
            SearchResult searchResult = new SearchResult();
            searchResult.setHitCount((int) getTotalHits(searchResponse));
            searchResult.setNodes(hits.stream().map(e -> e.getNode()).filter(Objects::nonNull).toList());
            return searchResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ESTreeNode> getTreeNodeById(String id) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_TREE_INDEX) + "/_doc/" + encodePathSegment(id);
        Request request = new Request("GET", endpoint);
        try {
            var response = restClient.performRequest(request);
            JsonNode body = objectMapper.readTree(response.getEntity().getContent());
            if (!body.path("found").asBoolean(false)) {
                return Optional.empty();
            }
            JsonNode source = body.get("_source");
            if (source == null || source.isNull()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.treeToValue(source, ESTreeNode.class));
        } catch (ResponseException e) {
            if (e.getResponse().getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private List<ESTreeNode> mgetTreeNodes(List<String> ids) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_TREE_INDEX) + "/_mget";
        Request request = new Request("POST", endpoint);
        request.setJsonEntity(objectMapper.writeValueAsString(Map.of("ids", ids)));

        var response = restClient.performRequest(request);
        JsonNode docs = objectMapper.readTree(response.getEntity().getContent()).path("docs");
        if (!docs.isArray()) {
            return Collections.emptyList();
        }

        List<ESTreeNode> treeNodes = new ArrayList<>();
        for (JsonNode doc : docs) {
            if (doc.path("found").asBoolean(false)) {
                JsonNode source = doc.get("_source");
                if (source != null && !source.isNull()) {
                    treeNodes.add(objectMapper.treeToValue(source, ESTreeNode.class));
                }
            }
        }
        return treeNodes;
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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

    private JsonNode executeSearch(String endpoint, String payload) throws IOException {
        Request request = new Request("POST", endpoint);
        request.setJsonEntity(payload);
        return objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
    }

    private <T> List<T> parseSearchHits(JsonNode response, Class<T> type) throws IOException {
        JsonNode hits = response.path("hits").path("hits");
        if (!hits.isArray()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.get("_source");
            if (source != null && !source.isNull()) {
                result.add(objectMapper.treeToValue(source, type));
            }
        }
        return result;
    }

    private long getTotalHits(JsonNode response) {
        JsonNode total = response.path("hits").path("total");
        if (total.isObject()) {
            return total.path("value").asLong(0L);
        }
        if (total.isNumber()) {
            return total.asLong(0L);
        }
        return 0L;
    }

    private String serializeSearchRequest(SearchRequest searchRequest) {
        try {
            StringWriter writer = new StringWriter();
            JsonMapper jsonMapper = objectMapper instanceof JsonMapper
                    ? (JsonMapper) objectMapper
                    : JsonMapper.builder().build();
            Jackson3JsonpMapper mapper = new Jackson3JsonpMapper(jsonMapper);
            JsonGenerator generator = mapper.jsonProvider().createGenerator(writer);
            searchRequest.serialize(generator, mapper);
            generator.close();
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize search request", e);
        }
    }

    private String buildTreeNodePayload(ESTreeNode treeNode) throws IOException {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("childNodes", treeNode.getChildNodes() == null ? new ArrayList<>() : treeNode.getChildNodes());

        Map<String, Object> nodeMap = new LinkedHashMap<>();
        var node = treeNode.getNode();
        if (node != null) {
            if (node.getUniqueId() != null) {
                nodeMap.put("uniqueId", node.getUniqueId());
            }
            if (node.getName() != null) {
                nodeMap.put("name", node.getName());
            }
            if (node.getDescription() != null) {
                nodeMap.put("description", node.getDescription());
            }
            if (node.getCreated() != null) {
                nodeMap.put("created", node.getCreated().getTime());
            }
            if (node.getLastModified() != null) {
                nodeMap.put("lastModified", node.getLastModified().getTime());
            }
            if (node.getNodeType() != null) {
                nodeMap.put("nodeType", node.getNodeType().name());
            }
            if (node.getUserName() != null) {
                nodeMap.put("userName", node.getUserName());
            }
            if (node.getTags() != null) {
                List<Map<String, Object>> tags = new ArrayList<>();
                for (Tag tag : node.getTags()) {
                    if (tag == null) {
                        continue;
                    }
                    Map<String, Object> tagMap = new LinkedHashMap<>();
                    if (tag.getName() != null) {
                        tagMap.put("name", tag.getName());
                    }
                    if (tag.getComment() != null) {
                        tagMap.put("comment", tag.getComment());
                    }
                    if (tag.getUserName() != null) {
                        tagMap.put("userName", tag.getUserName());
                    }
                    if (tag.getCreated() != null) {
                        tagMap.put("created", tag.getCreated().getTime());
                    }
                    tags.add(tagMap);
                }
                nodeMap.put("tags", tags);
            }
        }

        doc.put("node", nodeMap);
        return objectMapper.writeValueAsString(doc);
    }
}
