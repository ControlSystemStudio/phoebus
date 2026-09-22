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
import org.phoebus.applications.saveandrestore.model.SnapshotData;
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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Repository} class for {@link SnapshotData} objects.
 */
@Repository
public class SnapshotDataRepository implements CrudRepository<SnapshotData, String> {

    @Value("${elasticsearch.snapshot_node.index:saveandrestore_snapshot}")
    private String ES_SNAPSHOT_INDEX;

    @Autowired
    @Qualifier("restClient")
    private Rest5Client restClient;

    @Autowired
    @Qualifier("elasticObjectMapper")
    private ObjectMapper objectMapper;

    private final Logger logger = Logger.getLogger(SnapshotDataRepository.class.getName());

    /**
     * Saves a {@link org.phoebus.applications.saveandrestore.model.SnapshotData}.
     * @param entity A {@link org.phoebus.applications.saveandrestore.model.SnapshotData} object.
     * @return The persisted {@link SnapshotData} object.
     */
    @Override
    public <S extends SnapshotData> S save(@NonNull S entity) {
        try {
            String id = entity.getUniqueId();
            Request request = new Request("PUT", "/" + encodePathSegment(ES_SNAPSHOT_INDEX)
                    + "/_doc/" + encodePathSegment(id));
            request.addParameter("refresh", "true");
            request.setJsonEntity(objectMapper.writeValueAsString(entity));
            int statusCode = restClient.performRequest(request).getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return (S) getSnapshotDataById(id).orElse(null);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save snapshot for config id " + entity.getUniqueId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save snapshot for config id " + entity.getUniqueId());
        }
        return null;
    }

    @Override
    public <S extends SnapshotData> Iterable<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<SnapshotData> findById(String id) {
        try {
            return getSnapshotDataById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve snapshot with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve snapshot with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {
        return false;
    }

    @Override
    public Iterable<SnapshotData> findAll() {
        return null;
    }

    @Override
    public Iterable<SnapshotData> findAllById(Iterable<String> strings) {
        return null;
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
     * Not implemented, i.e. does nothing
     * @param id Unique id of a {@link SnapshotData} object.
     */
    @Override
    public void deleteById(String id) {

    }

    /**
     * Not implemented, i.e. does nothing
     * @param entity A {@link SnapshotData} object.
     */
    @Override
    public void delete(SnapshotData entity) {

    }

    /**
     * Not implemented, i.e. does nothing
     * @param strings A list of {@link SnapshotData} ids.
     */
    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    /**
     * Not implemented, i.e. does nothing
     * @param entities A list of {@link SnapshotData} objects.
     */
    @Override
    public void deleteAll(Iterable<? extends SnapshotData> entities) {

    }

    @Override
    public void deleteAll() {
        try {
            Request request = new Request("POST", "/" + encodePathSegment(ES_SNAPSHOT_INDEX) + "/_delete_by_query");
            request.addParameter("refresh", "true");
            request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
            JsonNode body = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            logger.log(Level.INFO, "Deleted " + body.path("deleted").asLong(0L) + " Snapshot objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all Snapshot objects", e);
            throw new RuntimeException(e);
        }
    }

    private Optional<SnapshotData> getSnapshotDataById(String id) throws IOException {
        String endpoint = "/" + encodePathSegment(ES_SNAPSHOT_INDEX) + "/_doc/" + encodePathSegment(id);
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
            return Optional.of(objectMapper.treeToValue(source, SnapshotData.class));
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
