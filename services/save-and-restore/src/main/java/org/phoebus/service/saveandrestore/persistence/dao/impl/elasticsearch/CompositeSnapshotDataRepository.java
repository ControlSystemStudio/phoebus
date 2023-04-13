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
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class CompositeSnapshotDataRepository implements CrudRepository<CompositeSnapshotData, String> {

    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    public String ES_COMPOSITE_SNAPSHOT_INDEX;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private final Logger logger = Logger.getLogger(CompositeSnapshotDataRepository.class.getName());

    @Override
    public <S extends CompositeSnapshotData> S save(S entity) {
        try {
            IndexRequest<CompositeSnapshotData> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_COMPOSITE_SNAPSHOT_INDEX)
                                    .id(entity.getUniqueId())
                                    .document(entity)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_COMPOSITE_SNAPSHOT_INDEX).id(response.id()));
                GetResponse<CompositeSnapshotData> resp =
                        client.get(getRequest, CompositeSnapshotData.class);
                return (S) resp.source();
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
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_COMPOSITE_SNAPSHOT_INDEX).id(id));
            GetResponse<CompositeSnapshotData> resp =
                    client.get(getRequest, CompositeSnapshotData.class);

            if (!resp.found()) {
                return Optional.empty();
            }
            return Optional.of(resp.source());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to retrieve composite snapshot with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve composite snapshot with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_COMPOSITE_SNAPSHOT_INDEX).id(s));
            BooleanResponse existsResponse = client.exists(existsRequest);
            return existsResponse.value();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if CompositeSnapshot with id " + s + " exists");
        }
        return false;
    }

    /**
     * Retrieves all {@link CompositeSnapshotData} documents. Note that to work around the limits in
     * Elasticsearch (e.g. max 10000 documents in a search request), the implementation uses paginated search to repeatedly
     * query for next round of hits. A page size of 100 is used for each query.
     * @return An {@link Iterable} of {@link CompositeSnapshotData} objects, potentially empty.
     */
    @Override
    public Iterable<CompositeSnapshotData>  findAll() {
        List<CompositeSnapshotData> result = new ArrayList<>();
        int pageSize = 100;
        int from = 0;
        while(true){
            try {
                SearchResponse<CompositeSnapshotData> searchResponse = runPagedMatchAll(pageSize, from);
                result.addAll(searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList()));
                from += searchResponse.hits().hits().size();
                if(searchResponse.hits().hits().size() < pageSize){
                    break;
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to get all CompositeSnapshotData objects");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to get all CompositeSnapshotData objects");
            }
        }
        return result;
    }

    private SearchResponse<CompositeSnapshotData> runPagedMatchAll(int pageSize, int from) throws IOException{
        SearchRequest searchRequest =
                SearchRequest.of(s ->
                    s.index(ES_COMPOSITE_SNAPSHOT_INDEX)
                            .query(new MatchAllQuery.Builder().build()._toQuery())
                            .size(pageSize)
                            .from(from));
        return client.search(searchRequest, CompositeSnapshotData.class);
    }

    @Override
    public Iterable<CompositeSnapshotData> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public long count() {
        try{
            CountRequest countRequest = CountRequest.of(c ->
                    c.index(ES_COMPOSITE_SNAPSHOT_INDEX));
            CountResponse countResponse = client.count(countRequest);
            return countResponse.count();
        }
        catch(Exception e){
            logger.log(Level.SEVERE, "Failed to count CompositeSnapshot objects" , e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String s) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ES_COMPOSITE_SNAPSHOT_INDEX).id(s).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if(deleteResponse.result().equals(Result.Deleted)){
                logger.log(Level.WARNING, "Composite snapshot with id " + s + " deleted.");
            }
            else{
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
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(d ->
                    d.index(ES_COMPOSITE_SNAPSHOT_INDEX).query(new MatchAllQuery.Builder().build()._toQuery()).refresh(true));
            DeleteByQueryResponse deleteResponse = client.deleteByQuery(deleteRequest);
            logger.log(Level.INFO, "Deleted " + deleteResponse.deleted() + " CompositeSnapshot objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all CompositeSnapshot objects", e);
            throw new RuntimeException(e);
        }
    }
}
