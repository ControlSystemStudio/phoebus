/**
 *
 */
package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

/**
 * @author Kunal Shroff {@literal <kunalshroff9@gmail.gov>}
 *
 */
public class ElasticClientHelper {
    Properties props = PropertiesHelper.getProperties();
    private static RestHighLevelClient client;
    private static ElasticClientHelper instance;
    private static Sniffer sniffer;

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
        } catch (Exception e) {
            try {
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

    public boolean indexAlarmStateDocument(String indexName, AlarmStateMessage alarmStateMessage) {
        IndexRequest indexRequest = new IndexRequest(indexName.toLowerCase(), "alarm");
        try {
            indexRequest.source(alarmStateMessage.sourceMap());
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            return indexResponse.getResult().equals(Result.CREATED);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmStateMessage + " to index " + indexName, e);
            return false;
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
}
