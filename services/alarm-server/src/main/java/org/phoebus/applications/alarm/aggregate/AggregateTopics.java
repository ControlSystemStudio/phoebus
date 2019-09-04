/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.aggregate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.streams.KafkaStreams;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.server.CreateTopics;

/** Tool that aggregates topics
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AggregateTopics
{
    private Logger logger = Logger.getLogger(this.getClass().getPackageName());
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

        logger.info("server:\"" + kafka_servers + "\", config: \"" + config + "\"");
        logger.info("topics: " + topics.toString());

        aggregateStream = createStream();
        logger.info("Starting stream aggregation.");
        aggregateStream.start();

        try
        (
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(System.in));
        )
        {
            System.out.println("Type \"exit\" to stop.");
            String input;
            do
            {
                System.out.print("> ");
                input = bufReader.readLine();
                System.out.print("\n");
            }
            while (! input.equalsIgnoreCase("exit"));

        } catch (IOException ex)
        {
            logger.log(Level.WARNING, "Reading input from stdin failed.", ex);
        }

        // Exit the program. The shutdown hook will clean up the stream.
        System.exit(0);
    }

    private void help()
    {
        System.out.println("AggregateTopics usage. \n\nThis program serves to aggregate Config, State, Command, and Talk compacted topics into a non compacted long term topic.\n");
        System.out.println("\t-help : Prints this message.");
        System.out.println("\t-server server_name: Allows specification of server address.\n\t\tDefault is \"localhost:9092\".");
        System.out.println("\t-confg config_name: Allows specification of config name.\n\t\tDefault is \"Accelerator\".");
        System.out.println("\t-create : Discovers if the config + \"LongTerm\" topic already exists. If it does not, it creates it.");
        System.exit(0);
    }

    private void parseArgs(String[] args)
    {
        List<String> argList = Arrays.asList(args);
        Iterator<String> token = argList.iterator();
        try
        {
            while (token.hasNext())
            {
                String arg = token.next();
                if (arg.startsWith("-h"))
                {
                    help();
                }
                else if (arg.equals("-server"))
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
                else
                {
                    throw new Exception("Unknown argument '" + arg + "'.");
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Argument Error", ex);
            help();
        }
    }

    private List<String> createTopicList()
    {
        List<String> list = List.of(config,
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
            logger.log(Level.WARNING, "Kafka Streams Error.", throwable);
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
