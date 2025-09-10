package org.phoebus.service.saveandrestore.search;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 * @author Georg Weiss
 */
public class SearchUtil {

    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final private static DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    private String ES_TREE_INDEX;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    private String ES_COMPOSITE_SNAPSHOT_INDEX;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.default:100}")
    private int defaultSearchSize;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.max:1000}")
    private int maxSearchSize;

    private static final Logger LOG = LoggerFactory.getLogger(SearchUtil.class);

    /**
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters) {
        Builder boolQueryBuilder = new Builder();
        boolean fuzzySearch = false;
        List<String> descriptionTerms = new ArrayList<>();
        List<String> descriptionPhraseTerms = new ArrayList<>();
        List<String> nodeNameTerms = new ArrayList<>();
        List<String> nodeNamePhraseTerms = new ArrayList<>();
        List<String> nodeTypeTerms = new ArrayList<>();
        List<String> uniqueIdTerms = new ArrayList<>();
        boolean temporalSearch = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        LOG.info("buildSearchRequest() called");
        LOG.info("  searchParameters: " + searchParameters);

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            String s = parameter.getKey().strip().toLowerCase();
            switch (s) {
                case "uniqueid":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[|,;]")) {
                            uniqueIdTerms.add(pattern.trim());
                        }
                    }
                    break;

                // Search for node name. List of names cannot be split on space char as it is allowed in a node name.
                case "name":
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                nodeNamePhraseTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                // add wildcards inorder to search for sub-strings
                                nodeNameTerms.add("*" + term + "*");
                            }
                        }
                    }
                    break;

                // Search in description/comment
                case "desc":
                case "description":
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                descriptionPhraseTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                // add wildcards inorder to search for sub-strings
                                descriptionTerms.add("*" + term + "*");
                            }
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
                                if (!tagsSearchFields[0].equalsIgnoreCase(Tag.GOLDEN)) {
                                    bqb.must(WildcardQuery.of(w -> w.caseInsensitive(true).field("node.tags.name").value(tagsSearchFields[0].trim().toLowerCase()))._toQuery());
                                } else {
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
                    if (!disMaxQuery.queries().isEmpty()) {
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
                case "referenced":
                    return buildSearchRequestForContainedIn(searchParameters);
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

        // Add the description query. Multiple search terms will be AND:ed.
        if (!descriptionTerms.isEmpty()) {
            for (String searchTerm : descriptionTerms) {
                NestedQuery innerNestedQuery;
                if (fuzzySearch) {
                    FuzzyQuery matchQuery = FuzzyQuery.of(m -> m.field("node.description").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                } else {
                    WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.description").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                }
                boolQueryBuilder.must(innerNestedQuery._toQuery());
            }
        }

        // Add phrase queries for the description key. Multiple search terms will be AND:ed.
        if (!descriptionPhraseTerms.isEmpty()) {
            for (String searchTerm : descriptionPhraseTerms) {
                MatchPhraseQuery matchQuery = MatchPhraseQuery.of(m -> m.field("node.description").query(searchTerm));
                NestedQuery innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                boolQueryBuilder.must(innerNestedQuery._toQuery());
            }
        }

        // Add uniqueId query
        if (!uniqueIdTerms.isEmpty()) {
            boolQueryBuilder.must(IdsQuery.of(id -> id.values(uniqueIdTerms))._toQuery());
        }

        // Add the description query. Multiple search terms will be AND:ed.
        if (!nodeNameTerms.isEmpty()) {
            for (String searchTerm : nodeNameTerms) {
                NestedQuery innerNestedQuery;
                if (fuzzySearch) {
                    FuzzyQuery matchQuery = FuzzyQuery.of(m -> m.field("node.name").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                } else {
                    WildcardQuery matchQuery = WildcardQuery.of(m -> m.field("node.name").value(searchTerm));
                    innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                }
                boolQueryBuilder.must(innerNestedQuery._toQuery());
            }
        }

        // Add phrase queries for the nodeName key. Multiple search terms will be AND:ed.
        if (!nodeNamePhraseTerms.isEmpty()) {
            for (String searchTerm : nodeNamePhraseTerms) {
                MatchPhraseQuery matchQuery = MatchPhraseQuery.of(m -> m.field("node.name").query(searchTerm));
                NestedQuery innerNestedQuery = NestedQuery.of(n -> n.path("node").query(matchQuery._toQuery()));
                boolQueryBuilder.must(innerNestedQuery._toQuery());
            }
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
                .sort(SortOptions.of(o -> o
                                .field(FieldSort.of(f -> f
                                                .field("node.name.raw")
                                                .nested(n -> n.path("node"))
                                                .order(SortOrder.Asc)
                                        )
                                )
                        )
                )
                .timeout("60s")
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }

    /**
     * Builds a query on the configuration index to find {@link org.phoebus.applications.saveandrestore.model.ConfigurationData}
     * documents containing any of the PV names passed to this method. Both setpoint and readback PV names are considered.
     *
     * @param pvNames List of PV names. Query will user or-strategy.
     * @return A {@link SearchRequest} object, no limit on result size except maximum Elastic limit.
     */
    public SearchRequest buildSearchRequestForPvs(List<String> pvNames) {
        int searchResultSize = defaultSearchSize;
        Builder boolQueryBuilder = new Builder();
        DisMaxQuery.Builder pvQuery = new DisMaxQuery.Builder();
        List<Query> pvQueries = new ArrayList<>();
        for (String value : pvNames) {
            for (String pattern : value.split("[|,;]")) {
                pvQueries.add(MatchQuery.of(m -> m.field("pvList").query(pattern.trim()))._toQuery());
            }
        }
        Query pvsQuery = pvQuery.queries(pvQueries).build()._toQuery();
        boolQueryBuilder.must(pvsQuery);

        return SearchRequest.of(s -> s.index(ES_CONFIGURATION_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .size(Math.min(searchResultSize, maxSearchSize))
                .from(0));
    }

    /**
     * Parses a search query terms string into a string array. In particular,
     * quoted search terms must be maintained even if they contain the
     * separator chars used to tokenize the terms.
     *
     * @param searchQueryTerms String as specified by client
     * @return A {@link List} of search terms, some of which may be
     * quoted. Is void of any zero-length strings.
     */
    public List<String> getSearchTerms(String searchQueryTerms) {
        // Count double quote chars. Odd number of quote chars
        // is not supported -> throw exception
        long quoteCount = searchQueryTerms.chars().filter(c -> c == '\"').count();
        if (quoteCount == 0) {
            return Arrays.stream(searchQueryTerms.split("[\\|,;\\s+]")).filter(t -> t.length() > 0).collect(Collectors.toList());
        }
        if (quoteCount % 2 == 1) {
            throw new IllegalArgumentException("Unbalanced quotes in search query");
        }
        // If we come this far then at least one quoted term is
        // contained in user input
        List<String> terms = new ArrayList<>();
        int nextStartIndex = searchQueryTerms.indexOf('\"');
        while (nextStartIndex >= 0) {
            int endIndex = searchQueryTerms.indexOf('\"', nextStartIndex + 1);
            String quotedTerm = searchQueryTerms.substring(nextStartIndex, endIndex + 1);
            terms.add(quotedTerm);
            // Remove the quoted term from user input
            searchQueryTerms = searchQueryTerms.replace(quotedTerm, "");
            // Check next occurrence
            nextStartIndex = searchQueryTerms.indexOf('\"');
        }
        // Add remaining terms...
        List<String> remaining = Arrays.asList(searchQueryTerms.split("[\\|,;\\s+]"));
        //...but remove empty strings, which are "leftovers" when quoted terms are removed
        terms.addAll(remaining.stream().filter(t -> t.length() > 0).collect(Collectors.toList()));
        return terms;
    }

    /**
     * Constructs a {@link SearchRequest} search for composite snapshots containing a node id.
     *
     * @param searchParameters Map of search parameters where key &quot;containedin&quot; must be present and
     *                         contain at least one value. Note however that only first value is considered.
     *                         If no value is specified, an {@link IllegalArgumentException} is thrown.
     * @return A suitable {@link SearchRequest}
     */
    private SearchRequest buildSearchRequestForContainedIn(MultiValueMap<String, String> searchParameters) {
        List<String> value = searchParameters.get("referenced");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("At least one value must be specified for 'referenced'");
        }
        int from = 0;
        List<String> parameter = searchParameters.get("from");
        if (parameter != null) {
            Optional<String> maxFrom = parameter.stream().max(Comparator.comparing(Integer::valueOf));
            if (maxFrom.isPresent()) {
                from = Integer.parseInt(maxFrom.get());
            }
        }
        int size = maxSearchSize;
        parameter = searchParameters.get("size");
        if (parameter != null) {
            Optional<String> maxFrom = parameter.stream().max(Comparator.comparing(Integer::valueOf));
            if (maxFrom.isPresent()) {
                size = Integer.parseInt(maxFrom.get());
            }
        }

        // Only consider first node id value if multiple are specified
        String nodeId = value.get(0);
        int _from = from;
        int _size = size;
        MatchPhraseQuery matchPhraseQuery = MatchPhraseQuery.of(m ->
                m.field("referencedSnapshotNodes").query(nodeId));
        return SearchRequest.of(s -> s.index(ES_COMPOSITE_SNAPSHOT_INDEX)
                .query(matchPhraseQuery._toQuery())
                .timeout("60s")
                .from(_from)
                .size(_size));
    }
}
