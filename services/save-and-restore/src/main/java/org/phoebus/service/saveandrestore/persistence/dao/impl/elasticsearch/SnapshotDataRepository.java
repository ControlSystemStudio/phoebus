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
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    @Qualifier("client")
    private ElasticsearchClient client;

    private final Logger logger = Logger.getLogger(SnapshotDataRepository.class.getName());

    /**
     * Saves a {@link org.phoebus.applications.saveandrestore.model.SnapshotData}.
     * @param entity A {@link org.phoebus.applications.saveandrestore.model.SnapshotData} object.
     * @return The persisted {@link SnapshotData} object.
     */
    @Override
    public <S extends SnapshotData> S save(@NonNull S entity) {
        try {
            IndexRequest<SnapshotData> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_SNAPSHOT_INDEX)
                                    .id(entity.getUniqueId())
                                    .document(entity)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_SNAPSHOT_INDEX).id(response.id()));
                GetResponse<SnapshotData> resp =
                        client.get(getRequest, SnapshotData.class);
                return (S) resp.source();
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
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_SNAPSHOT_INDEX).id(id));
            GetResponse<SnapshotData> resp =
                    client.get(getRequest, SnapshotData.class);

            if (!resp.found()) {
                return Optional.empty();
            }
            return Optional.of(resp.source());
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
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(d ->
                    d.index(ES_SNAPSHOT_INDEX).query(new MatchAllQuery.Builder().build()._toQuery()).refresh(true));
            DeleteByQueryResponse deleteResponse = client.deleteByQuery(deleteRequest);
            logger.log(Level.INFO, "Deleted " + deleteResponse.deleted() + " Snapshot objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all Snapshot objects", e);
            throw new RuntimeException(e);
        }
    }
}
