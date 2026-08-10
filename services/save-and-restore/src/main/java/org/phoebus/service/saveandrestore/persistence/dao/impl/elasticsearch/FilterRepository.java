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

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository class for {@link Filter} objects.
 */
@Repository
public class FilterRepository implements CrudRepository<Filter, String> {

    private static final Logger logger = Logger.getLogger(FilterRepository.class.getName());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.filter.index:saveandrestore_filter}")
    private String ES_FILTER_INDEX;

    @Autowired
    @Qualifier("restClient")
    private Rest5Client restClient;

    @Autowired
    @Qualifier("elasticObjectMapper")
    private ObjectMapper objectMapper;

    /**
     * Saves an {@link Filter} object.
     *
     * @param filter A {@link Filter} object
     * @return A {@link Filter} as persisted in Elasticsearch.
     */
    @Override
    public <S extends Filter> S save(S filter) {
        try {
            filter.setLastUpdated(new Date());
            String id = filter.getName();
            Request request = new Request("PUT", "/" + encodePathSegment(ES_FILTER_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            request.setJsonEntity(objectMapper.writeValueAsString(filter));
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return (S) getFilterById(id).orElse(null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save Filter object: " + filter.getName(), e);
            throw new RuntimeException("Filter object: " + filter.getName());
        }
        return null;
    }

    /**
     * Not implemented, will always throw {@link RuntimeException}
     * @param entities must not be {@literal null} nor must it contain {@literal null}.
     * @throws RuntimeException always
     */
    @Override
    public <S extends Filter> Iterable<S> saveAll(@NonNull Iterable<S> entities) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @param name Unique {@link Filter} name.
     * @return {@link Optional}, may be empty.
     */
    @Override
    public Optional<Filter> findById(@NonNull String name) {
        try {
            Optional<Filter> result = getFilterById(name);
            if (result.isEmpty()) {
                throw new NodeNotFoundException("Filter with name " + name + " not found.");
            }
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve Filter with name: " + name, e);
            throw new RuntimeException("Failed to Filter with name: " + name);
        }
    }

    @Override
    public boolean existsById(String name) {

        try {
            return documentExists(ES_FILTER_INDEX, name);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if Filter with name " + name + " exists");
        }
        return false;
    }

    @Override
    public Iterable<Filter> findAll() {
        List<Filter> result = new ArrayList<>();
        int pageSize = 1000;
        int from = 0;
        while(true){
            try {
                List<Filter> batch = runPagedMatchAll(pageSize, from);
                result.addAll(batch);
                from += batch.size();
                if(batch.size() < pageSize){
                    break;
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to get all Filter objects");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to get all Filter objects");
            }
        }
        return result;
    }

    private List<Filter> runPagedMatchAll(int pageSize, int from) throws IOException{
        Request request = new Request("POST", "/" + encodePathSegment(ES_FILTER_INDEX) + "/_search");
        request.setJsonEntity("{\"query\":{\"match_all\":{}},\"size\":" + pageSize + ",\"from\":" + from + "}");
        JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
        return parseSearchHits(body, Filter.class);
    }

    /**
     * Retrieves {@link Filter}s corresponding to the provided list of unique id, i.e. filter names.
     * <p>
     * Note that if a unique name is not found in the index, this method will <b>not</b> throw an {@link Exception}. The returned
     * {@link List} of {@link Filter} may consequently be shorter than the input list of unique ids, or
     * even empty. It is hence up to the callee to determine how to handle a potential discrepancy.
     *
     * @param uniqueNames A {@link Iterable} of unique ids. If <code>null</code>, an empty {@link List} is
     *                    returned.
     * @return A (potentially empty) {@link List} of existing {@link Filter}s.
     */
    @Override
    public Iterable<Filter> findAllById(Iterable<String> uniqueNames) {
        if (!uniqueNames.iterator().hasNext()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        uniqueNames.forEach(ids::add);
        try {
            return mgetFilters(ids);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve multiple filters");
            throw new RuntimeException("Failed to retrieve multiple filters");
        }
    }

    @Override
    public long count() {
        return 0;
    }

    /**
     * Deletes a {@link Filter}. If there is no {@link Filter} matching the specified name, no
     * excpetion is thrown.
     * @param name Name of {@link Filter} to delete.
     */
    @Override
    public void deleteById(String name) {
        try {
            Request request = new Request("DELETE", "/" + encodePathSegment(ES_FILTER_INDEX)
                    + "/_doc/" + encodePathSegment(name));
            request.addParameter("refresh", "true");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            if ("deleted".equalsIgnoreCase(body.path("result").asText(""))) {
                logger.log(Level.WARNING, "Filter with name " + name + " deleted.");
            } else {
                logger.log(Level.WARNING, "Filter with id " + name + " NOT deleted.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete Filter with name: " + name, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Filter filter) {
        deleteById(filter.getName());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {
    }

    @Override
    public void deleteAll(Iterable<? extends Filter> filters) {

    }

    @Override
    public void deleteAll() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_FILTER_INDEX) + "/_delete_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            logger.log(Level.INFO, "Deleted " + body.path("deleted").asLong(0L) + " Filter objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all Filter objects", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<Filter> getFilterById(String id) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_FILTER_INDEX) + "/_doc/" + encodePathSegment(id);
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
            return Optional.of(objectMapper.treeToValue(source, Filter.class));
        } catch (ResponseException e) {
            if (e.getResponse().getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private List<Filter> mgetFilters(List<String> ids) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_FILTER_INDEX) + "/_mget";
        Request request = new Request("POST", endpoint);
        request.setJsonEntity(objectMapper.writeValueAsString(java.util.Map.of("ids", ids)));

        var response = restClient.performRequest(request);
        JsonNode docs = objectMapper.readTree(response.getEntity().getContent()).path("docs");
        if (!docs.isArray()) {
            return Collections.emptyList();
        }

        List<Filter> filters = new ArrayList<>();
        for (JsonNode doc : docs) {
            if (doc.path("found").asBoolean(false)) {
                JsonNode source = doc.get("_source");
                if (source != null && !source.isNull()) {
                    filters.add(objectMapper.treeToValue(source, Filter.class));
                }
            }
        }
        return filters;
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
}
