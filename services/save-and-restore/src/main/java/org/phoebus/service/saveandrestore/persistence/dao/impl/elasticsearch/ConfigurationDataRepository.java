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
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for {@link ConfigurationData}.
 */
@Repository
public class ConfigurationDataRepository implements CrudRepository<ConfigurationData, String> {

    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;

    @Autowired
    @Qualifier("restClient")
    private Rest5Client restClient;

    @Autowired
    @Qualifier("elasticObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private SearchUtil searchUtil;

    private final Logger logger = Logger.getLogger(ConfigurationDataRepository.class.getName());

    @Override
    public <S extends ConfigurationData> S save(S entity) {
        try {
            String id = entity.getUniqueId();
            Request request = new Request("PUT", "/" + encodePathSegment(ES_CONFIGURATION_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            request.setJsonEntity(objectMapper.writeValueAsString(entity));
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return (S) getConfigurationDataById(id).orElse(null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save configuration for config id " + entity.getUniqueId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save configuration for config id " + entity.getUniqueId());
        }
        return null;
    }

    @Override
    public <S extends ConfigurationData> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<ConfigurationData> findById(String id) {
        try {
            return getConfigurationDataById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve configuration with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve configuration with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {
        try {
            return documentExists(ES_CONFIGURATION_INDEX, s);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if ConfigurationData with id " + s + " exists");
        }
        return false;
    }

    @Override
    public Iterable<ConfigurationData> findAll() {
        return null;
    }

    @Override
    public Iterable<ConfigurationData> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_CONFIGURATION_INDEX) + "/_count");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            return body.path("count").asLong(0L);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to count ConfigurationData objects", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String s) {
        try {
            Request request = new Request("DELETE", "/" + encodePathSegment(ES_CONFIGURATION_INDEX)
                    + "/_doc/" + encodePathSegment(s));
            request.addParameter("refresh", "true");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            if ("deleted".equalsIgnoreCase(body.path("result").asText(""))) {
                logger.log(Level.WARNING, "Configuration with id " + s + " deleted.");
            } else {
                logger.log(Level.WARNING, "Configuration with id " + s + " NOT deleted.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete configuration with id: " + s, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ConfigurationData entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends ConfigurationData> entities) {

    }

    @Override
    public void deleteAll() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_CONFIGURATION_INDEX) + "/_delete_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            logger.log(Level.INFO, "Deleted " + body.path("deleted").asLong(0L) + " ConfigurationData objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all ConfigurationData objects", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs a search on a list of PV names. An OR strategy is used, i.e. {@link ConfigurationData} document need
     * only contain one of the listed PV names.
     * @param searchParameters Search parameters provided by client.
     * @return Potentially empty {@link List} of {@link ConfigurationData} objects contain any of the listed PV names.
     */
    public List<ConfigurationData> searchOnPvName(MultiValueMap<String, String> searchParameters) {
        Optional<Map.Entry<String, List<String>>> optional =
                searchParameters.entrySet().stream().filter(e -> e.getKey().strip().equalsIgnoreCase("pvs")).findFirst();
        if (optional.isEmpty()) {
            return Collections.emptyList();
        }
        SearchRequest searchRequest = searchUtil.buildSearchRequestForPvs(optional.get().getValue());
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_CONFIGURATION_INDEX) + "/_search");
            request.setJsonEntity(serializeSearchRequest(searchRequest));
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            return parseSearchHits(body, ConfigurationData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ConfigurationData> getConfigurationDataById(String id) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_CONFIGURATION_INDEX) + "/_doc/" + encodePathSegment(id);
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
            return Optional.of(objectMapper.treeToValue(source, ConfigurationData.class));
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

        List<T> result = new java.util.ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.get("_source");
            if (source != null && !source.isNull()) {
                result.add(objectMapper.treeToValue(source, type));
            }
        }
        return result;
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
