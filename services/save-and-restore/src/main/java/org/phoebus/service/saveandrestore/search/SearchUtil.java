package org.phoebus.service.saveandrestore.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 * @author Georg Weiss
 */
public class SearchUtil {

    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final public static DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    public String ES_TREE_INDEX;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.default:100}")
    private int defaultSearchSize;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.max:1000}")
    private int maxSearchSize;

    /**
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters) {
        Builder boolQueryBuilder = new Builder();
        boolean fuzzySearch = false;
        List<String> descriptionTerms = new ArrayList<>();
        List<String> nodeNameTerms = new ArrayList<>();
        List<String> nodeTypeTerms = new ArrayList<>();
        boolean temporalSearch = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                // Search for node name. List of names cannot be split on space char as it is allowed in a node name.
                case "name":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            nodeNameTerms.add(pattern.trim().toLowerCase());
                        }
                    }
                    break;
                // Search in description/comment
                case "description":
                case "desc":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            descriptionTerms.add(pattern.trim());
                        }
                    }
                    break;
                // Search for node type.
                case "type":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            nodeTypeTerms.add(pattern.trim().toLowerCase());
                        }
                    }
                    break;
                case "fuzzy":
                    fuzzySearch = true;
                    break;
                case "user":
                    DisMaxQuery.Builder userQuery = new DisMaxQuery.Builder();
                    List<Query> userQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            NestedQuery innerNestedQuery;
                            WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.userName").value(pattern));
                            innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                            userQueries.add(innerNestedQuery._toQuery());
                        }
                    }
                    userQuery.queries(userQueries);
                    boolQueryBuilder.must(userQuery.build()._toQuery());
                    break;
                case "start":
                    // If there are multiple start times submitted select the earliest
                    ZonedDateTime earliestStartTime = ZonedDateTime.now();
                    for (String value : parameter.getValue()) {
                        ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                        earliestStartTime = earliestStartTime.isBefore(time) ? earliestStartTime : time;
                    }
                    temporalSearch = true;
                    start = earliestStartTime;
                    break;
                case "end":
                    // If there are multiple end times submitted select the latest
                    ZonedDateTime latestEndTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                    for (String value : parameter.getValue()) {
                        ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                        latestEndTime = latestEndTime.isBefore(time) ? time : latestEndTime;
                    }
                    temporalSearch = true;
                    end = latestEndTime;
                    break;
                case "tags":
                    DisMaxQuery.Builder tagsQuery = new DisMaxQuery.Builder();
                    tagsQuery.queries(Collections.emptyList());

                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            String[] tagsSearchFields;
                            tagsSearchFields = Arrays.copyOf(pattern.split("\\."), 2);
                            BoolQuery.Builder bqb = new BoolQuery.Builder();
                            // This handles a special case where search is done on name only, e.g. tags=golden
                            if (tagsSearchFields[0] != null && !tagsSearchFields[0].isEmpty() && (tagsSearchFields[1] == null || tagsSearchFields[1].isEmpty())) {
                                if(!tagsSearchFields[0].equalsIgnoreCase(Tag.GOLDEN)){
                                    bqb.must(WildcardQuery.of(w -> w.caseInsensitive(true).field("node.tags.name").value(tagsSearchFields[0].trim().toLowerCase()))._toQuery());
                                }
                                else{
                                    MatchQuery matchQuery = MatchQuery.of(m -> m.field("node.tags.name").query(Tag.GOLDEN));
                                    NestedQuery innerNestedQuery = NestedQuery.of(n1 -> n1.path("node.tags").query(matchQuery._toQuery()));
                                    NestedQuery outerNestedQuery = NestedQuery.of(n2 -> n2.path("node").query(innerNestedQuery._toQuery()));
                                    boolQueryBuilder.must(outerNestedQuery._toQuery());
                                    continue;
                                }
                            }
                            // This handles the "generic" case where user specifies a field of a tag, e.g. tags=comment.foo (where foo is the search term)
                            else {
                                bqb.must(WildcardQuery.of(w -> w.caseInsensitive(true).field("node.tags." + tagsSearchFields[0]).value(tagsSearchFields[1].trim().toLowerCase()))._toQuery());
                            }
                            NestedQuery innerNestedQuery = NestedQuery.of(n1 -> n1.path("node.tags").query(bqb.build()._toQuery()));
                            tagsQuery.queries(q -> q.nested(NestedQuery.of(n -> n.path("node").query(innerNestedQuery._toQuery()).scoreMode(ChildScoreMode.None))));
                        }
                    }
                    DisMaxQuery disMaxQuery = tagsQuery.build();
                    if(!disMaxQuery.queries().isEmpty()){
                        boolQueryBuilder.must(disMaxQuery._toQuery());
                    }
                    break;
                case "size":
                case "limit":
                    Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxSize.isPresent()) {
                        searchResultSize = Integer.parseInt(maxSize.get());
                    }
                    break;
                case "from":
                    Optional<String> maxFrom = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxFrom.isPresent()) {
                        from = Integer.parseInt(maxFrom.get());
                    }
                    break;
                default:
                    // Unsupported search parameters ignored
                    break;
            }
        }

        // Add the temporal queries
        if (temporalSearch) {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end)) {
                DisMaxQuery.Builder temporalQuery = new DisMaxQuery.Builder();
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                // Add a query based on the created time
                rangeQuery.field("node.lastModified").from(Long.toString(1000 * start.toEpochSecond()))
                        .to(Long.toString(1000 * end.toEpochSecond()));
                NestedQuery nestedQuery = NestedQuery.of(n1 -> n1.path("node").query(rangeQuery.build()._toQuery()));
                temporalQuery.queries(nestedQuery._toQuery());
                boolQueryBuilder.must(temporalQuery.build()._toQuery());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to parse search parameters: " + searchParameters + ", CAUSE: Invalid start and end times");
            }
        }

        // Add the description query
        if (!descriptionTerms.isEmpty()) {
            DisMaxQuery.Builder descQuery = new DisMaxQuery.Builder();
            List<Query> descQueries = new ArrayList<>();
            if (fuzzySearch) {
                descriptionTerms.stream().forEach(searchTerm -> {
                    Query fuzzyQuery = FuzzyQuery.of(f -> f.field("node.description").value(searchTerm))._toQuery();
                    NestedQuery nestedQuery =
                            NestedQuery.of(n1 -> n1.path("node")
                                    .query(fuzzyQuery));
                    descQueries.add(nestedQuery._toQuery());
                });
            } else {
                descriptionTerms.stream().forEach(searchTerm -> {
                    Query wildcardQuery =
                            WildcardQuery.of(w -> w.field("node.description").value(searchTerm))._toQuery();
                    NestedQuery nestedQuery =
                            NestedQuery.of(n1 -> n1.path("node")
                                    .query(wildcardQuery));
                    descQueries.add(nestedQuery._toQuery());
                });
            }
            descQuery.queries(descQueries);
            boolQueryBuilder.must(descQuery.build()._toQuery());
        }

        // Add the name query
        if (!nodeNameTerms.isEmpty()) {
            DisMaxQuery.Builder nodeNameQuery = new DisMaxQuery.Builder();
            List<Query> nodeNameQueries = new ArrayList<>();
            if (fuzzySearch) {
                nodeNameTerms.forEach(searchTerm -> {
                    NestedQuery innerNestedQuery;
                    FuzzyQuery matchQuery = FuzzyQuery.of(m -> m.field("node.name").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                    nodeNameQueries.add(innerNestedQuery._toQuery());
                });
            } else {
                nodeNameTerms.forEach(searchTerm -> {
                    NestedQuery innerNestedQuery;
                    WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.name").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                    nodeNameQueries.add(innerNestedQuery._toQuery());
                });
            }
            nodeNameQuery.queries(nodeNameQueries);
            boolQueryBuilder.must(nodeNameQuery.build()._toQuery());
        }

        // Add node type query. Fuzzy search not needed as node types are well-defined and limited in number.
        if (!nodeTypeTerms.isEmpty()) {
            DisMaxQuery.Builder nodeTypeQuery = new DisMaxQuery.Builder();
            List<Query> nodeTypeQueries = new ArrayList<>();
            nodeTypeTerms.forEach(searchTerm -> {
                NestedQuery innerNestedQuery;
                WildcardQuery matchQuery = WildcardQuery.of(m -> m.caseInsensitive(true).field("node.nodeType").value(searchTerm));
                innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                nodeTypeQueries.add(innerNestedQuery._toQuery());
            });
            nodeTypeQuery.queries(nodeTypeQueries);
            boolQueryBuilder.must(nodeTypeQuery.build()._toQuery());
        }

        int _searchResultSize = searchResultSize;
        int _from = from;

        return SearchRequest.of(s -> s.index(ES_TREE_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }
}
