/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.util.elasticsearch;

import java.util.Collections;
import java.util.List;

/**
 * Wraps a few properties related to the Elasticsearch service.
 * @param connectStatus Indicates if the Elasticsearch connection is functional.
 * @param version Version of Elasticsearch.
 * @param indexStats Statistics for relevant indices as determined by the service.
 */
public record ElasticInfo(String connectStatus, String version, List<IndexStats> indexStats) {
    public ElasticInfo(){
        this("Disconnected", "n/a", Collections.emptyList());
    }
    public ElasticInfo(String version, List<IndexStats> indexStats){
        this("Connected", version, indexStats);
    }
}
