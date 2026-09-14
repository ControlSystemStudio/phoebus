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
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Repository} class for {@link CompositeSnapshotData}.
 */
@Repository
public class CompositeSnapshotDataRepository implements CrudRepository<CompositeSnapshotData, String> {

    @SuppressWarnings("unused")
    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    private String ES_COMPOSITE_SNAPSHOT_INDEX;

    @Autowired
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @Autowired
    @Qualifier("restClient")
    private Rest5Client restClient;

    @Autowired
    @Qualifier("elasticObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private SearchUtil searchUtil;

    private final Logger logger = Logger.getLogger(CompositeSnapshotDataRepository.class.getName());

    @Override
    public <S extends CompositeSnapshotData> S save(S entity) {
        try {
            String id = entity.getUniqueId();
            Request request = new Request("PUT", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            request.setJsonEntity(objectMapper.writeValueAsString(entity));
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return (S) getCompositeSnapshotDataById(id).orElse(null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save composite snapshot for unique id " + entity.getUniqueId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save composite snapshot for unique id " + entity.getUniqueId());
        }
        return null;
    }

    @Override
    public <S extends CompositeSnapshotData> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<CompositeSnapshotData> findById(String id) {
        try {
            return getCompositeSnapshotDataById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve composite snapshot with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve composite snapshot with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {
        try {
            return documentExists(ES_COMPOSITE_SNAPSHOT_INDEX, s);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if CompositeSnapshot with id " + s + " exists");
        }
        return false;
    }

    /**
     * Retrieves all {@link CompositeSnapshotData} documents. Note that to work around the limits in
     * Elasticsearch (e.g. max 10000 documents in a search request), the implementation uses paginated search to repeatedly
     * query for next round of hits. A page size of 100 is used for each query.
     *
     * @return An {@link Iterable} of {@link CompositeSnapshotData} objects, potentially empty.
     */
    @Override
    public Iterable<CompositeSnapshotData> findAll() {
        List<CompositeSnapshotData> result = new ArrayList<>();
        int pageSize = 100;
        int from = 0;
        while (true) {
            try {
                List<CompositeSnapshotData> batch = runPagedMatchAll(pageSize, from);
                result.addAll(batch);
                from += batch.size();
                if (batch.size() < pageSize) {
                    break;
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to get all CompositeSnapshotData objects");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to get all CompositeSnapshotData objects");
            }
        }
        return result;
    }

    private List<CompositeSnapshotData> runPagedMatchAll(int pageSize, int from) throws IOException {
        Request request = new Request("POST", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX) + "/_search");
        request.setJsonEntity("{\"query\":{\"match_all\":{}},\"size\":" + pageSize + ",\"from\":" + from + "}");
        JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
        return parseSearchHits(body, CompositeSnapshotData.class);
    }

    @Override
    public Iterable<CompositeSnapshotData> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX) + "/_count");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            return body.path("count").asLong(0L);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to count CompositeSnapshot objects", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String s) {
        try {
            Request request = new Request("DELETE", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX)
                    + "/_doc/" + encodePathSegment(s));
            request.addParameter("refresh", "true");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            if ("deleted".equalsIgnoreCase(body.path("result").asText(""))) {
                logger.log(Level.WARNING, "Composite snapshot with id " + s + " deleted.");
            } else {
                logger.log(Level.WARNING, "Composite snapshot with id " + s + " NOT deleted.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete composite snapshot with id: " + s, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(CompositeSnapshotData entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends CompositeSnapshotData> entities) {

    }

    @Override
    public void deleteAll() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX) + "/_delete_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            logger.log(Level.INFO, "Deleted " + body.path("deleted").asLong(0L) + " CompositeSnapshot objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all CompositeSnapshot objects", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds {@link Node}s of type {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT} referencing
     * the node id present in the search parameters.
     *
     * @param searchParameters {@link MultiValueMap} that should contain an element keyed &quot;referenced&quot;
     * @return A potentially empty {@link SearchResult}
     */
    public SearchResult referenced(MultiValueMap<String, String> searchParameters) {

        SearchRequest searchRequest = searchUtil.buildSearchRequest(searchParameters);
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX) + "/_search");
            request.setJsonEntity(serializeSearchRequest(searchRequest));
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            List<CompositeSnapshotData> compositeSnapshotDataList = parseSearchHits(body, CompositeSnapshotData.class);
            Iterable<ESTreeNode> esTreeNodes = elasticsearchTreeRepository.findAllById(compositeSnapshotDataList.stream().map(CompositeSnapshotData::getUniqueId).toList());
            List<Node> list = new ArrayList<>();
            esTreeNodes.iterator().forEachRemaining(es -> list.add(es.getNode()));
            return new SearchResult((int) getTotalHits(body), list);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to search for referenced snapshot nodes", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<CompositeSnapshotData> getCompositeSnapshotDataById(String id) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_COMPOSITE_SNAPSHOT_INDEX) + "/_doc/" + encodePathSegment(id);
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
            return Optional.of(objectMapper.treeToValue(source, CompositeSnapshotData.class));
        } catch (ResponseException e) {
            if (e.getResponse().getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
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
}
