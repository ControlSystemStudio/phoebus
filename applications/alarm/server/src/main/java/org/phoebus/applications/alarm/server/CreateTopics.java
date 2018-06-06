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
                            segment_time   = "segment.ms",
                            time           = "10000",
                            dirty2clean    = "min.cleanable.dirty.ratio",
                            ratio          = "0.01";
                            
    private static AdminClient client = null;
    
    /**
     * <p> Discover the currently active Kafka topics, and creates any default topics that
     * do not yet exist.
     * <p> The default topics are currently "Accelerator", "AcceleratorState", and "AcceleratorCommand".
     * The topic "AcceleratorTalk" is not yet implemented, but will be added upon completion.
     * @param kafka_servers The network address for the kafka_servers. Example: 'localhost:9092'.
     */
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
    
    /**
     * <p> Discover any currently active Kafka topics. Return a list of strings filled with any default topics that need to be created.
     * @return topics_to_create <code>List</code> of <code>Strings</code> with all the topic names that need to be created. 
     *                           Returns <code>null</code> if none need to be created.
     */
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
    
    /**
     * <p> Create a topic for each of the topics in the passed list.
     * @param topics_to_create <code>List</code> of <code>Strings</code> filled with the names of topics to create.
     */
    private static void createTopics(List<String> topics_to_create)
    {
        ArrayList<NewTopic> new_topics = new ArrayList<NewTopic>();
        // Create the new topics locally.
        for (String topic : topics_to_create)
        {
                logger.info("Creating topic " + topic);
                new_topics.add(createTopic(topic));
        }
        // Create the new topics in the Kafka server.
        try
        {
            CreateTopicsResult res = client.createTopics(new_topics);
            KafkaFuture<Void> future = res.all();
            future.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            logger.log(Level.WARNING, "Attempt to create topics failed.", e);
        }
    }
    
    /**
     * <p> Create a Kafka topic with the passed name.
     * @param   topic_name Name of the topic to be created.
     * @return  new_topic The newly created topic.
     */
    private static NewTopic createTopic(String topic_name)
    {
        NewTopic new_topic = new NewTopic(topic_name, PARTITIONS, REPLICATION_FACTOR);
        HashMap<String, String> configs = new HashMap<String, String>();
        configs.put(cleanup_policy, policy);
        configs.put(segment_time, time);
        configs.put(dirty2clean, ratio);
        return new_topic.configs(configs);
    }
}
