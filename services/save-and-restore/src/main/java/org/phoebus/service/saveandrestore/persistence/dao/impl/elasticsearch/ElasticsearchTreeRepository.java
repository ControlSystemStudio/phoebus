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
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
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
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Repository
public class ElasticsearchTreeRepository implements CrudRepository<ESTreeNode, String> {

    private static final Logger logger = Logger.getLogger(ElasticsearchTreeRepository.class.getName());

    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    public String ES_TREE_INDEX;

    /**
     * Used to determine if the {@link ESTreeNode} is saved or updated in connection to a migration
     * operation. If so, the last modified date should be preserved.
     */
    private boolean migrationContext;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    @SuppressWarnings("unused")
    @Autowired
    private SearchUtil searchUtil;

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
    public <S extends ESTreeNode> S save(S elasticTreeNode) {
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

            IndexRequest<ESTreeNode> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_TREE_INDEX)
                                    .id(elasticTreeNode.getNode().getUniqueId())
                                    .document(elasticTreeNode)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_TREE_INDEX).id(response.id()));
                GetResponse<ESTreeNode> resp =
                        client.get(getRequest, ESTreeNode.class);
                return (S) resp.source();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save ESTreeNode object: " + elasticTreeNode, e);
            throw new RuntimeException("ESTreeNode object: " + elasticTreeNode.getNode().getName());
        }
        return null;
    }

    @Override
    public <S extends ESTreeNode> Iterable<S> saveAll(Iterable<S> entities) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<ESTreeNode> findById(String id) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_TREE_INDEX).id(id));
            GetResponse<ESTreeNode> resp =
                    client.get(getRequest, ESTreeNode.class);

            if (!resp.found()) {
                throw new NodeNotFoundException("ESTreeNode with id " + id + " not found.");
            }
            return Optional.of(resp.source());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve ESTreeNode with id: " + id, e);
            throw new RuntimeException("Failed to ESTreeNode with id: " + id);
        }
    }

    @Override
    public boolean existsById(String s) {

        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_TREE_INDEX).id(s));
            BooleanResponse existsResponse = client.exists(existsRequest);
            return existsResponse.value();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to query if ESTreeNode with id " + s + " exists");
        }
        return false;
    }

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
        if (!uniqueIds.iterator().hasNext()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        uniqueIds.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(m -> m.index(ES_TREE_INDEX).ids(ids));
        try {
            List<ESTreeNode> treeNodes = new ArrayList<>();
            MgetResponse<ESTreeNode> resp = client.mget(mgetRequest, ESTreeNode.class);
            resp.docs().forEach(doc -> {
                if (doc.result().found()) { // Only add elements that actually exist
                    treeNodes.add(doc.result().source());
                }
            });
            return treeNodes;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to retrieve multiple nodes");
            throw new NodeNotFoundException("Failed to retrieve multiple nodes");
        }
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String s) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ES_TREE_INDEX).id(s).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if (deleteResponse.result().equals(Result.Deleted)) {
                logger.log(Level.WARNING, "Node with id " + s + " deleted.");
            } else {
                logger.log(Level.WARNING, "Node with id " + s + " NOT deleted.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to delete node with id: " + s, e);
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
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(d ->
                    d.index(ES_TREE_INDEX).query(new MatchAllQuery.Builder().build()._toQuery()).refresh(true));
            DeleteByQueryResponse deleteResponse = client.deleteByQuery(deleteRequest);
            logger.log(Level.INFO, "Deleted " + deleteResponse.deleted() + " ESTreeNode objects");
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
        Builder bqb = new Builder();
        bqb.must(TermQuery.of(w -> w.field("childNodes").value(uniqueId))._toQuery());
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ES_TREE_INDEX)
                .query(bqb.build()._toQuery())
                .timeout("60s"));
        try {
            SearchResponse<ESTreeNode> searchResponse = client.search(searchRequest, ESTreeNode.class);
            if (!searchResponse.hits().hits().isEmpty()) {
                if (searchResponse.hits().hits().size() > 1) {
                    logger.log(Level.SEVERE, "Node " + uniqueId + " is child node of multiple nodes. Should not happen!");
                    throw new RuntimeException("Node " + uniqueId + " contained in multiple parent nodes. Should not happen!");
                }
                List<ESTreeNode> result =
                        searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
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
        BoolQuery.Builder boolQueryBuilder = new Builder();
        NestedQuery innerNestedQuery;
        if (!goldenOnly) {
            ExistsQuery existsQuery = ExistsQuery.of(e -> e.field("node.tags"));
            innerNestedQuery = NestedQuery.of(n1 -> n1.path("node.tags").query(existsQuery._toQuery()));
        } else {
            MatchQuery matchQuery = MatchQuery.of(m -> m.field("node.tags.name").query(Tag.GOLDEN));
            innerNestedQuery = NestedQuery.of(n1 -> n1.path("node.tags").query(matchQuery._toQuery()));
        }
        NestedQuery outerNestedQuery = NestedQuery.of(n2 -> n2.path("node").query(innerNestedQuery._toQuery()));
        boolQueryBuilder.must(outerNestedQuery._toQuery());
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ES_TREE_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .size(1000));
        try {
            SearchResponse<ESTreeNode> esTreeNodeSearchResponse = client.search(searchRequest, ESTreeNode.class);
            return esTreeNodeSearchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SearchResult search(MultiValueMap<String, String> searchParameters) {

        SearchRequest searchRequest = searchUtil.buildSearchRequest(searchParameters);
        try {
            SearchResponse<ESTreeNode> searchResponse = client.search(searchRequest, ESTreeNode.class);
            SearchResult searchResult = new SearchResult();
            searchResult.setHitCount((int) searchResponse.hits().total().value());
            searchResult.setNodes(searchResponse.hits().hits().stream().map(e -> e.source().getNode()).collect(Collectors.toList()));
            return searchResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ESTreeNode> getByNodeName(String nodeName) {
        BoolQuery.Builder boolQueryBuilder = new Builder();
        NestedQuery innerNestedQuery;
        MatchQuery matchQuery = MatchQuery.of(m -> m.field("node.name").query(nodeName));
        innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
        boolQueryBuilder.must(innerNestedQuery._toQuery());
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ES_TREE_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .size(1000));
        try {
            SearchResponse<ESTreeNode> esTreeNodeSearchResponse = client.search(searchRequest, ESTreeNode.class);
            return esTreeNodeSearchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
