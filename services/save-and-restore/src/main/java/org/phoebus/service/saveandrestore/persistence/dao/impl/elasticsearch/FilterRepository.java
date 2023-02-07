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
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Repository
public class FilterRepository implements CrudRepository<Filter, String> {

    private static final Logger logger = Logger.getLogger(FilterRepository.class.getName());

    @Value("${elasticsearch.filter.index:saveandrestore_filter}")
    public String ES_FILTER_INDEX;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

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
            IndexRequest<Filter> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_FILTER_INDEX)
                                    .id(filter.getName())
                                    .document(filter)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_FILTER_INDEX).id(response.id()));
                GetResponse<Filter> resp =
                        client.get(getRequest, Filter.class);
                return (S) resp.source();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save Filter object: " + filter.getName(), e);
            throw new RuntimeException("Filter object: " + filter.getName());
        }
        return null;
    }

    @Override
    public <S extends Filter> Iterable<S> saveAll(Iterable<S> entities) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<Filter> findById(String name) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_FILTER_INDEX).id(name));
            GetResponse<Filter> resp =
                    client.get(getRequest, Filter.class);

            if (!resp.found()) {
                throw new NodeNotFoundException("Filter with name " + name + " not found.");
            }
            return Optional.of(resp.source());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve Filter with name: " + name, e);
            throw new RuntimeException("Failed to Filter with name: " + name);
        }
    }

    @Override
    public boolean existsById(String name) {

        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_FILTER_INDEX).id(name));
            BooleanResponse existsResponse = client.exists(existsRequest);
            return existsResponse.value();
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
                SearchResponse<Filter> searchResponse = runPagedMatchAll(pageSize, from);
                result.addAll(searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList()));
                from += searchResponse.hits().hits().size();
                if(searchResponse.hits().hits().size() < pageSize){
                    break;
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to get all Filter objects");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to get all Filter objects");
            }
        }
        return result;
    }

    private SearchResponse<Filter> runPagedMatchAll(int pageSize, int from) throws IOException{
        SearchRequest searchRequest =
                SearchRequest.of(s ->
                        s.index(ES_FILTER_INDEX)
                                .query(new MatchAllQuery.Builder().build()._toQuery())
                                .size(pageSize)
                                .from(from));
        return client.search(searchRequest, Filter.class);
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
        MgetRequest mgetRequest = MgetRequest.of(m -> m.index(ES_FILTER_INDEX).ids(ids));
        try {
            List<Filter> filters = new ArrayList<>();
            MgetResponse<Filter> resp = client.mget(mgetRequest, Filter.class);
            resp.docs().forEach(doc -> {
                if (doc.result().found()) { // Only add elements that actually exist
                    filters.add(doc.result().source());
                }
            });
            return filters;
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
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ES_FILTER_INDEX).id(name).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if (deleteResponse.result().equals(Result.Deleted)) {
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
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(d ->
                    d.index(ES_FILTER_INDEX).query(new MatchAllQuery.Builder().build()._toQuery()).refresh(true));
            DeleteByQueryResponse deleteResponse = client.deleteByQuery(deleteRequest);
            logger.log(Level.INFO, "Deleted " + deleteResponse.deleted() + " Filter objects");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete all Filter objects", e);
            throw new RuntimeException(e);
        }
    }
}
