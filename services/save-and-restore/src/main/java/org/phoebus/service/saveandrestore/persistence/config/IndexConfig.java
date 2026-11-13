/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.persistence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component to centralize values related to Elasticsearch index names.
 */
@Component
public class IndexConfig {

    @SuppressWarnings("unused")
    @Value("${elasticsearch.tree_node.index:saveandrestore_tree}")
    public String ES_TREE_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    public String ES_CONFIGURATION_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.snapshot_node.index:saveandrestore_snapshot}")
    public String ES_SNAPSHOT_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.composite_snapshot_node.index:saveandrestore_composite_snapshot}")
    public String ES_COMPOSITE_SNAPSHOT_INDEX;

    @SuppressWarnings("unused")
    @Value("${elasticsearch.filter.index:saveandrestore_filter}")
    public String ES_FILTER_INDEX;
}
