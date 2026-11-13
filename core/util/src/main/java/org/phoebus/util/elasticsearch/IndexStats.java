/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.util.elasticsearch;

/**
 * Wraps a few properties for an Elasticsearch index.
 * @param indexName Name of the index, e.g. saveandrestore_configuration
 * @param documentCount Number of indexed documents
 * @param deletedDocuments Number of deleted documents
 * @param indexSize Index size in bytes
 */
public record IndexStats(String indexName, int documentCount, int deletedDocuments, long indexSize) {
}
