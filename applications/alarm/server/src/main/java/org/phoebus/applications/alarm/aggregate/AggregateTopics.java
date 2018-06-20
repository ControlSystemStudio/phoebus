package org.phoebus.applications.alarm.aggregate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class AggregateTopics
{
    
    private AggregateTopics(String[] args)
    {
        ArrayList<String> argList = (ArrayList<String>) Arrays.asList(args);
        Iterator<String> token = argList.iterator();
        
        while (token.hasNext())
        {
            String arg = token.next();
            if (arg.equals("-server"))
            {
                if (token.hasNext())
                {
                    // Set server
                }
                else
                {
                    // Throw exception and die.
                }
            }
        }
        /*
        KafkaStreams streamToLongTerm = KafkaHelper.aggregateTopics(kafka_servers, 
                List.of(config_topic, command_topic, state_topic, talk_topic), 
                longterm_topic);

        streamToLongTerm.start();
        // close the stream aggregate and wait 1 second at most for threads to join.
        streamToLongTerm.close(1000, TimeUnit.MILLISECONDS);
        */
    }
    
    public static void main(String[] args)
    {
        new AggregateTopics(args);
    }
}
