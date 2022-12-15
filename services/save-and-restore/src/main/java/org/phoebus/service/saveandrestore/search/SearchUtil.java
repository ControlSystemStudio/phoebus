package org.phoebus.service.saveandrestore.search;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.NestedSortValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
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
        List<String> nodeNameTerms = new ArrayList<>();
        List<String> nodeTypeTerms = new ArrayList<>();
        boolean temporalSearch = false;
        boolean includeEvents = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        // Default sort order
        SortOrder sortOrder = null;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                // Search for node name. List of names cannot be split on space char as it is allowed in a node name.
                case "name":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            nodeNameTerms.add(pattern.trim());
                        }
                    }
                    break;
                // Search for node type.
                case "type":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            // Convert to upper case as node type is stored using the NodeType enum string values.
                            nodeTypeTerms.add(pattern.trim().toUpperCase());
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
                        for (String pattern : value.split("[\\|,;]")) {
                            NestedQuery innerNestedQuery;
                            WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.userName").value(pattern));
                            innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                            userQueries.add(innerNestedQuery._toQuery());
                        }
                    }
                    userQuery.queries(userQueries);
                    boolQueryBuilder.must(userQuery.build()._toQuery());
                    break;
                case "tags":
                    DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
                    List<Query> tagsQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            tagsQueries.add(WildcardQuery.of(w -> w.field("tags.name").value(pattern.trim()))._toQuery());
                        }
                    }
                    Query tagsQuery = tagQuery.queries(tagsQueries).build()._toQuery();
                    NestedQuery nestedTagsQuery = NestedQuery.of(n -> n.path("tags").query(tagsQuery));
                    boolQueryBuilder.must(nestedTagsQuery._toQuery());
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
                case "properties":
                    DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String[] propertySearchFields;
                            propertySearchFields = Arrays.copyOf(pattern.split("\\."), 3);
                            Builder bqb = new Builder();
                            if (propertySearchFields[0] != null && !propertySearchFields[0].isEmpty()) {
                                bqb.must(WildcardQuery.of(w -> w.field("properties.name").value(propertySearchFields[0].trim()))._toQuery());
                            }

                            if (propertySearchFields[1] != null && !propertySearchFields[1].isEmpty()) {
                                Builder bqb2 = new Builder();
                                bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.name").value(propertySearchFields[1].trim()))._toQuery());
                                if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty()) {
                                    bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.value").value(propertySearchFields[2].trim()))._toQuery());
                                }
                                bqb.must(NestedQuery.of(n -> n.path("properties.attributes").query(bqb2.build()._toQuery()).scoreMode(ChildScoreMode.None))._toQuery());
                            }
                            propertyQuery.queries(q -> q.nested(NestedQuery.of(n -> n.path("properties").query(bqb.build()._toQuery()).scoreMode(ChildScoreMode.None))));
                        }
                    }
                    boolQueryBuilder.must(propertyQuery.build()._toQuery());
                    break;
                case "size":
                case "limit":
                    Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxSize.isPresent()) {
                        searchResultSize = Integer.valueOf(maxSize.get());
                    }
                    break;
                case "from":
                    Optional<String> maxFrom = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxFrom.isPresent()) {
                        from = Integer.valueOf(maxFrom.get());
                    }
                    break;
                case "sort": // Honor sort order if client specifies it
                    List<String> sortList = parameter.getValue();
                    if (sortList != null && sortList.size() > 0) {
                        String sort = sortList.get(0);
                        if (sort.toUpperCase().startsWith("ASC") || sort.toUpperCase().startsWith("UP")) {
                            sortOrder = SortOrder.Asc;
                        } else if (sort.toUpperCase().startsWith("DESC") || sort.toUpperCase().startsWith("DOWN")) {
                            sortOrder = SortOrder.Desc;
                        }
                    }
                    break;
                default:
                    // Unsupported search parameters are ignored
                    break;
            }
        }

        // Add the temporal queries
        if (temporalSearch) {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end)) {
                DisMaxQuery.Builder temporalQuery = new DisMaxQuery.Builder();
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                // Add a query based on the create time
                rangeQuery.field("createdDate").from(Long.toString(1000 * start.toEpochSecond()))
                        .to(Long.toString(1000 * end.toEpochSecond()));
                if (includeEvents) {
                    RangeQuery.Builder eventsRangeQuery = new RangeQuery.Builder();
                    // Add a query based on the time of the associated events
                    eventsRangeQuery.field("events.instant").from(Long.toString(1000 * start.toEpochSecond()))
                            .to(Long.toString(1000 * end.toEpochSecond()));
                    NestedQuery.Builder nestedQuery = new NestedQuery.Builder();
                    nestedQuery.path("events").query(eventsRangeQuery.build()._toQuery());

                    temporalQuery.queries(rangeQuery.build()._toQuery(), nestedQuery.build()._toQuery());
                    boolQueryBuilder.must(temporalQuery.build()._toQuery());
                } else {
                    boolQueryBuilder.must(rangeQuery.build()._toQuery());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to parse search parameters: " + searchParameters + ", CAUSE: Invalid start and end times");
            }
        }

        // Add the name query
        if (!nodeNameTerms.isEmpty()) {
            DisMaxQuery.Builder nodeNameQuery = new DisMaxQuery.Builder();
            List<Query> nodeNameQueries = new ArrayList<>();
            if (fuzzySearch) {
                nodeNameTerms.stream().forEach(searchTerm -> {
                    NestedQuery innerNestedQuery;
                    FuzzyQuery matchQuery = FuzzyQuery.of(m -> m.field("node.name").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                    nodeNameQueries.add(innerNestedQuery._toQuery());
                });
            } else {
                nodeNameTerms.stream().forEach(searchTerm -> {
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
            nodeTypeTerms.stream().forEach(searchTerm -> {
                NestedQuery innerNestedQuery;
                WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.nodeType").value(searchTerm));
                innerNestedQuery = NestedQuery.of(n1 -> n1.path("node").query(matchQuery._toQuery()));
                nodeTypeQueries.add(innerNestedQuery._toQuery());
            });
            nodeTypeQuery.queries(nodeTypeQueries);
            boolQueryBuilder.must(nodeTypeQuery.build()._toQuery());
        }

        int _searchResultSize = searchResultSize;
        int _from = from;

        FieldSort.Builder fb = new FieldSort.Builder();
        fb.field("node.created");
        fb.nested(NestedSortValue.of(n -> n.path("node")));
        fb.order(SortOrder.Desc);


        return SearchRequest.of(s -> s.index(ES_TREE_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }
}
