package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;

public class CreateTopics
{
    // Default topics.
    private static String ACCELERATOR       = "Accelerator",
                            ACCELERATOR_STATE = "AcceleratorState",
                            ACCELERATOR_CMD   = "AcceleratorCommand";
   
    // Default configuration for each topic.
    private static short REPLICATION_FACTOR = 1;
    private static int PARTITIONS = 1;
    private static String cleanup_policy = "cleanup.policy",
                            policy         = "compact",
                            segment_time   = "ms.segment",
                            time           = "10000",
                            dirty2clean    = "min.cleanable.dirty.ratio",
                            ratio          = "0.01";
                            
    private static AdminClient client = null;
    
    public static void discoverAndCreateTopics (String kafka_servers)
    {
        // Connect to Kafka server.
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        client = AdminClient.create(props);
      
        List<String> topics_to_create = discoverTopics();
        createTopics(topics_to_create);
        
        client.close();
    }
    
    private static List<String> discoverTopics()
    {
        // Discover what topics currently exist.
        ListTopicsResult res = client.listTopics();
        KafkaFuture<Set<String>> topics = res.names();
        Set<String> topic_names = null;
        try
        {
            topic_names = topics.get();
        } 
        catch (InterruptedException | ExecutionException e)
        {
            logger.log(Level.WARNING, "Unable to list topics. Automatic topic detection failed.", e);
            return null;
        }
        
        if (topic_names == null)
            return null;
        
        ArrayList<String> topics_to_create = new ArrayList<String>();
        if (! topic_names.contains(ACCELERATOR))
            topics_to_create.add(ACCELERATOR);
        if (! topic_names.contains(ACCELERATOR_STATE))
            topics_to_create.add(ACCELERATOR_STATE);
        if (! topic_names.contains(ACCELERATOR_CMD))
            topics_to_create.add(ACCELERATOR_CMD);
        
        return topics_to_create;
    }
    
    private static void createTopics(List<String> topics_to_create)
    {
        for (String topic : topics_to_create)
        {
            try
            {
                logger.info("Creating topic " + topic);
                createTopic(topic);
            } 
            catch (InterruptedException | ExecutionException e)
            {
                logger.log(Level.WARNING, "Attempt to create topic '" + topic + "' failed.", e);
            }
        }
    }
    
    private static void createTopic(String topic_name) throws InterruptedException, ExecutionException
    {
        ArrayList<NewTopic> topics = new ArrayList<NewTopic>();
        NewTopic new_topic = new NewTopic(topic_name, PARTITIONS, REPLICATION_FACTOR);
        HashMap<String, String> configs = new HashMap<String, String>();
        configs.put(cleanup_policy, policy);
        configs.put(segment_time, time);
        configs.put(dirty2clean, ratio);
        new_topic.configs(configs);
        topics.add(new_topic);
        CreateTopicsResult res = client.createTopics(topics);
        KafkaFuture<Void> future = res.all();
        future.get();
    }
}
