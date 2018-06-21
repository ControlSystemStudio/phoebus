package org.phoebus.applications.alarm.aggregate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.kafka.streams.KafkaStreams;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.server.CreateTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregateTopics
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private String kafka_servers = "localhost:9092";
    private String config = "Accelerator";
    private final String longTerm;
    private boolean createTopic = false;
    private final List<String> topics;
    private final KafkaStreams aggregateStream;
    
    private AggregateTopics(String[] args)
    {
        parseArgs(args);
        topics = createTopicList();
        longTerm = config + AlarmSystem.LONG_TERM_TOPIC_SUFFIX;
        if (createTopic)
        {
            logger.info("Discovering and creating topics in " + topics.toString());
            CreateTopics.discoverAndCreateTopics(kafka_servers, false, List.of(longTerm));
        }
        
        aggregateStream = createStream();
        logger.info("Starting stream aggregation.");
        aggregateStream.start();
        
        
        
    }
    
    private void parseArgs(String[] args)
    {
        ArrayList<String> argList = (ArrayList<String>) Arrays.asList(args);
        Iterator<String> token = argList.iterator();
        try
        {
            while (token.hasNext())
            {
                String arg = token.next();
                if (arg.equals("-server"))
                {
                    String next;
                    if (token.hasNext() && ! (next = token.next()).startsWith("-"))
                    {
                        kafka_servers = next;
                    }
                    else
                    {
                        throw new Exception("'-server' must be followed by a server name.");
                    }
                }
                else if (arg.equals("-create"))
                {
                    // create and discover long term topic.
                    createTopic = true;
                }
                else if (arg.equals("-config"))
                {
                    String next;
                    if (token.hasNext() && ! (next = token.next()).startsWith("-"))
                    {
                        config = next;
                    }
                    else
                    {
                        throw new Exception("'-config' must be followed by a config name.");
                    }
                }
            }
        }
        catch (Exception ex)
        {
            
        }
    }
    
    private List<String> createTopicList()
    {
        List<String> list = List.of(config, 
                                    config + AlarmSystem.STATE_TOPIC_SUFFIX,
                                    config + AlarmSystem.COMMAND_TOPIC_SUFFIX,
                                    config + AlarmSystem.TALK_TOPIC_SUFFIX);
        return list;
    }

    private KafkaStreams createStream()
    {
        KafkaStreams stream = KafkaHelper.aggregateTopics(kafka_servers, 
                topics, 
                longTerm);
        
        // Log any uncaught exceptions.
        stream.setUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
            logger.error("Kafka Streams Error.", throwable);
          });
         
        // Catch control-c and shutdown stream beforehand.
        Runtime.getRuntime().addShutdownHook(new Thread(stream::close));
        return stream;
    }

    public static void main(String[] args)
    {
        new AggregateTopics(args);
    }
}
