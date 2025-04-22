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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import jakarta.annotation.PostConstruct;
import org.phoebus.alarm.logging.ElasticClientHelper;
import org.phoebus.alarm.logging.rest.AlarmLogMessage;
import org.phoebus.alarm.logging.rest.AlarmLogSearchUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    private ElasticsearchClient elasticsearchClient;

    @SuppressWarnings("unused")
    @Value("${retention_period_days:0}")
    private int retentionPeriod;

    @SuppressWarnings("unused")
    @PostConstruct
    public void init() {
        elasticsearchClient = ElasticClientHelper.getInstance().getClient();
    }

    /**
     * Deletes Elasticsearch indices based on the {@link AlarmLogMessage#getMessage_time()} for each index found
     * by the client. The message time {@link Instant} is compared to current time minus the number of days specified as
     * application property.
     */
    @SuppressWarnings("unused")
    @Scheduled(cron = "${purge_cron_expr}")
    public void purgeElasticIndices() {
        try {
            IndicesResponse indicesResponse = elasticsearchClient.cat().indices();
            List<IndicesRecord> indicesRecords = indicesResponse.valueBody();
            Instant toInstant = Instant.now().minus(retentionPeriod, ChronoUnit.DAYS);
            for (IndicesRecord indicesRecord : indicesRecords) {
                // Elasticsearch may contain indices other than alarm indices...
                String indexName = indicesRecord.index();
                if (indexName != null && !indexName.startsWith("_alarms") && (indexName.contains("_alarms_state") ||
                        indexName.contains("_alarms_cmd") ||
                        indexName.contains("_alarms_config"))) {
                    // Find most recent document - based on message_time - in the alarm index.
                    SearchRequest searchRequest = SearchRequest.of(s ->
                            s.index(indexName)
                                    .query(new MatchAllQuery.Builder().build()._toQuery())
                                    .size(1)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("message_time").order(SortOrder.Desc))))));
                    SearchResponse<AlarmLogMessage> searchResponse = elasticsearchClient.search(searchRequest, AlarmLogMessage.class);
                    if (!searchResponse.hits().hits().isEmpty()) {
                        AlarmLogMessage alarmLogMessage = searchResponse.hits().hits().get(0).source();
                        if (alarmLogMessage != null && alarmLogMessage.getMessage_time().isBefore(toInstant)) {
                            DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.of(d -> d.index(indexName));
                            DeleteIndexResponse deleteIndexResponse = elasticsearchClient.indices().delete(deleteIndexRequest);
                            logger.log(Level.INFO, "Delete index " + indexName + " acknowledged: " + deleteIndexResponse.acknowledged());
                        }
                    } else {
                        logger.log(Level.WARNING, "Index " + indexName + " cannot be evaluated for removal as document count is zero.");
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Elastic query failed", e);
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
