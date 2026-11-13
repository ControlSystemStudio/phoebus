/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.applications.saveandrestore.model;

import org.phoebus.util.elasticsearch.ElasticInfo;
/**
 * POJO for server side information, e.g. versions and enabled/disabled features.
 * @param serviceVersion Version of the service
 * @param serverRestoreEnabled Indicates if restore endpoints are enabled
 * @param elasticInfo Information on Elasticsearch and its indices
 */
public record ServiceInfo(String serviceVersion, boolean serverRestoreEnabled, ElasticInfo elasticInfo) {
}
