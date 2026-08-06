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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Repository for {@link ConfigurationData}.
 */
@Repository
public class ConfigurationDataRepository implements CrudRepository<ConfigurationData, String> {

    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

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
            IndexRequest<ConfigurationData> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_CONFIGURATION_INDEX)
                                    .id(entity.getUniqueId())
                                    .document(entity)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                return (S) getConfigurationDataById(response.id()).orElse(null);
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
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_CONFIGURATION_INDEX).id(s));
            BooleanResponse existsResponse = client.exists(existsRequest);
            return existsResponse.value();
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
            CountRequest countRequest = CountRequest.of(c ->
                    c.index(ES_CONFIGURATION_INDEX));
            CountResponse countResponse = client.count(countRequest);
            return countResponse.count();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to count ConfigurationData objects", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String s) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ES_CONFIGURATION_INDEX).id(s).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if (deleteResponse.result().equals(Result.Deleted)) {
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
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(d ->
                    d.index(ES_CONFIGURATION_INDEX).query(new MatchAllQuery.Builder().build()._toQuery()).refresh(true));
            DeleteByQueryResponse deleteResponse = client.deleteByQuery(deleteRequest);
            logger.log(Level.INFO, "Deleted " + deleteResponse.deleted() + " ConfigurationData objects");
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
            SearchResponse<ConfigurationData> searchResponse = client.search(searchRequest, ConfigurationData.class);
            return searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
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
}
