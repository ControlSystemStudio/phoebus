/**
 *
 */
package org.phoebus.alarm.logging;

import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

/**
 * @author Kunal Shroff {@literal <kunalshroff9@gmail.gov>}
 *
 */
public class ElasticClientHelper {
    Properties props = PropertiesHelper.getProperties();
    private static RestHighLevelClient client;
    private static ElasticClientHelper instance;
    private static Sniffer sniffer;

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);
    ScheduledFuture<?> job;
    // State messages to be indexed
    BlockingQueue<IndexRequest> stateMessagedQueue = new LinkedBlockingDeque<>();
    // State messages to be indexed
    BlockingQueue<IndexRequest> configMessagedQueue = new LinkedBlockingDeque<>();

    private ElasticClientHelper() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down the ElasticClientHelper.");
                if (client != null) {
                    try {
                        sniffer.close();
                        client.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to close the elastic rest client", e);
                    }
                }
            }));
            client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(props.getProperty("es_host"),Integer.parseInt(props.getProperty("es_port")))));
            if (props.getProperty("es_sniff").equals("true")) {
                sniffer = Sniffer.builder(client.getLowLevelClient()).build();
                logger.log(Level.INFO, "ES Sniff feature is enabled");
            }
            // Start the executor for periodically logging into es
            job = scheduledExecutorService.scheduleAtFixedRate(new flush2Elastic(stateMessagedQueue, configMessagedQueue),
                    0, 250, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            try {
                job.cancel(false);
                sniffer.close();
                client.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close the elastic rest client", ex);
            }
        }

    }

    public static ElasticClientHelper getInstance() {
        if (instance == null) {
            instance = new ElasticClientHelper();
        }
        return instance;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    /**
     * Check if an index exists with the given name 
     * Note: this is an synchronous call
     *
     * @param indexName elastic index name / pattern
     * @return true if index exists
     */
    public boolean indexExists(String indexName) {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        try {
            return client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to query elastic", e);
            return false;
        }
    }

    public void indexAlarmStateDocument(String indexName, AlarmStateMessage alarmStateMessage) {
        IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm");
        try {
            indexRequest.source(alarmStateMessage.sourceMap());
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmStateMessage + " to index " + indexName, e);
        }
    }


    public void indexAlarmStateDocuments(String indexName, AlarmStateMessage alarmStateMessage) {
        try {
            IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm");
            indexRequest.source(alarmStateMessage.sourceMap());
            stateMessagedQueue.put(indexRequest);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmStateMessage + " to index " + indexName, e);
        }
    }

    public boolean indexAlarmCmdDocument(String indexName, AlarmCommandMessage alarmCommandMessage) {
        IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm_cmd");
        try {
            indexRequest.source(alarmCommandMessage.sourceMap());
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            return indexResponse.getResult().equals(Result.CREATED);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmCommandMessage + " to index " + indexName, e);
            return false;
        }
    }

    public boolean indexAlarmConfigDocument(String indexName, AlarmConfigMessage alarmConfigMessage) {
        IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm_config");
        try {
            indexRequest.source(alarmConfigMessage.sourceMap());
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            return indexResponse.getResult().equals(Result.CREATED);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmConfigMessage + " to index " + indexName, e);
            return false;
        }
    }

    public void indexAlarmConfigDocuments(String indexName, AlarmConfigMessage alarmConfigMessage) {
        try {
            IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm_config");
            indexRequest.source(alarmConfigMessage.sourceMap());
            configMessagedQueue.put(indexRequest);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmConfigMessage + " to index " + indexName, e);
        }
    }

    private static class flush2Elastic implements Runnable {

        private BlockingQueue<IndexRequest> stateMessagedQueue;
        private BlockingQueue<IndexRequest> configMessagedQueue;

        public flush2Elastic(BlockingQueue<IndexRequest> stateMessagedQueue, BlockingQueue<IndexRequest> configMessagedQueue) {
            this.stateMessagedQueue = stateMessagedQueue;
            this.configMessagedQueue = configMessagedQueue;
        }

        @Override
        public void run() {
            if(stateMessagedQueue.size() + configMessagedQueue.size() > 0){
                System.out.println("batch execution of : " + stateMessagedQueue.size() + " state messages and " + configMessagedQueue.size() + " config messages");
                BulkRequest bulkRequest = new BulkRequest();
                Collection<IndexRequest> stateIndexRequests = new ArrayList<>();
                stateMessagedQueue.drainTo(stateIndexRequests);
                stateIndexRequests.stream().forEach(bulkRequest::add);
                Collection<IndexRequest> configIndexRequests = new ArrayList<>();
                configMessagedQueue.drainTo(configIndexRequests);
                configIndexRequests.stream().forEach(bulkRequest::add);
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                try {
                    BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "failed to log messages to index ", e);
                }
            }
        }
    }
}
