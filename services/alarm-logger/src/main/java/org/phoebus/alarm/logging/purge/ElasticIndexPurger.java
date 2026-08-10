/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.alarm.logging.purge;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.phoebus.alarm.logging.rest.AlarmLogSearchUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class purging Elasticsearch from indices considered obsolete based on the date_span_units and retain_indices_count
 * application properties. If these result in a value below 100 (days), this {@link Component} will not be instantiated.
 * To determine last updated date of an index, each Elasticsearch index considered related to alarms is queried for last
 * inserted document. The message_time field of that document is compared to the retention period to determine
 * if the index should be deleted.
 * A cron expression application property is used to define when to run the purging process.
 */
@Component
// Enable only of retention period is >= 100 days
@ConditionalOnExpression("#{T(org.phoebus.alarm.logging.purge.ElasticIndexPurger.EnableCondition).getRetentionDays('${date_span_units}', '${retain_indices_count}') >= 100}")
public class ElasticIndexPurger {

    private static final Logger logger = Logger.getLogger(ElasticIndexPurger.class.getName());

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    @SuppressWarnings("unused")
    @Value("${retention_period_days:0}")
    private int retentionPeriod;

    @SuppressWarnings("unused")
    @PostConstruct
    public void init() {
        // Rest5Client is obtained lazily from ElasticClientHelper when needed.
    }

    /**
     * Deletes Elasticsearch indices based on the last document's message_time for each alarm index found.
     * Uses the low-level Rest5Client to avoid media-type header issues with ES 8 backends.
     */
    @SuppressWarnings("unused")
    @Scheduled(cron = "${purge_cron_expr}")
    public void purgeElasticIndices() {
        try {
            Rest5Client restClient = ElasticClientHelper.getInstance().getRestClient();

            // Get all indices via _cat/indices API (returns JSON array)
            Request catRequest = new Request("GET", "/_cat/indices?format=json");
            var catResponse = restClient.performRequest(catRequest);
            JsonNode indicesArray = objectMapper.readTree(catResponse.getEntity().getContent());

            Instant toInstant = Instant.now().minus(retentionPeriod, ChronoUnit.DAYS);

            if (!indicesArray.isArray()) {
                logger.log(Level.WARNING, "Unexpected response from _cat/indices endpoint.");
                return;
            }

            for (JsonNode indexRecord : indicesArray) {
                String indexName = indexRecord.path("index").asText(null);
                if (indexName == null) {
                    continue;
                }
                // Only consider alarm-related indices
                if (!indexName.startsWith("_alarms") && (indexName.contains("_alarms_state") ||
                        indexName.contains("_alarms_cmd") ||
                        indexName.contains("_alarms_config"))) {
                    // Find most recent document based on message_time
                    String searchBody = "{\"query\":{\"match_all\":{}},\"size\":1,\"sort\":[{\"message_time\":{\"order\":\"desc\"}}]}";
                    Request searchRequest = new Request("POST",
                            "/" + URLEncoder.encode(indexName, StandardCharsets.UTF_8) + "/_search");
                    searchRequest.setJsonEntity(searchBody);

                    try {
                        var searchResponse = restClient.performRequest(searchRequest);
                        JsonNode searchResult = objectMapper.readTree(searchResponse.getEntity().getContent());
                        JsonNode hits = searchResult.path("hits").path("hits");

                        if (hits.isArray() && hits.size() > 0) {
                            JsonNode source = hits.get(0).get("_source");
                            if (source != null) {
                                long messageTimeMillis = source.path("message_time").asLong(0L);
                                Instant messageInstant = Instant.ofEpochMilli(messageTimeMillis);
                                if (messageInstant.isBefore(toInstant)) {
                                    Request deleteRequest = new Request("DELETE",
                                            "/" + URLEncoder.encode(indexName, StandardCharsets.UTF_8));
                                    var deleteResponse = restClient.performRequest(deleteRequest);
                                    JsonNode deleteResult = objectMapper.readTree(
                                            deleteResponse.getEntity().getContent());
                                    boolean acknowledged = deleteResult.path("acknowledged").asBoolean(false);
                                    logger.log(Level.INFO,
                                            "Delete index " + indexName + " acknowledged: " + acknowledged);
                                }
                            }
                        } else {
                            logger.log(Level.WARNING,
                                    "Index " + indexName + " cannot be evaluated for removal as document count is zero.");
                        }
                    } catch (ResponseException e) {
                        logger.log(Level.WARNING, "Failed to query index " + indexName + " for purge evaluation.", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Elastic query failed during index purge.", e);
        }
    }

    /**
     * Helper class used to determine whether this service should be enabled or not
     */
    public static class EnableCondition {

        /**
         *
         * @param dateSpanUnits Any of the values Y, M, W, D
         * @param retainIndicesCountString String value of the retain_indices_count preference
         * @return A number computed from input. In case input arguments are invalid (e.g. non-numerical value
         * for retain_indices_coun), then 0 is returned to indicate that this {@link Component} should not be enabled.
         */
        @SuppressWarnings("unused")
        public static int getRetentionDays(String dateSpanUnits, String retainIndicesCountString) {
            int days = AlarmLogSearchUtil.getDateSpanInDays(dateSpanUnits);
            if (days == -1) {
                return 0;
            }
            try {
                int retainIndicesCount = Integer.parseInt(retainIndicesCountString);
                return days * retainIndicesCount;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
