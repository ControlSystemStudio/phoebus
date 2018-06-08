/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.KafkaHelper;

/**
 * Client for *Talk Topics
 * <p> Largely based on Kay Kasemir's {@link AlarmClient}. 
 * @author Evan Smith
 *
 */
public class TalkClient
{
    private final String talk_topic;
    private final CopyOnWriteArrayList<TalkClientListener> listeners = new CopyOnWriteArrayList<>();
    // TODO What type of list?
    //private final List<Message> messages = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    // TODO Do we need a producer? Are we sending messages back deleting the message once it has been annunciated? How should this work?
    //private final Producer<String, String> producer;
    private final Thread thread;

    /** @param server Kafka Server host:port
     *  @param config_name Name of alarm tree root
     */
    public TalkClient(final String server, final String config_name)
    {
        Objects.requireNonNull(server);
        Objects.requireNonNull(config_name);
        
        talk_topic = config_name + AlarmSystem.TALK_TOPIC_SUFFIX;

        final List<String> topics = List.of(talk_topic);
        consumer = KafkaHelper.connectConsumer(server, topics, topics);
        //producer = KafkaHelper.connectProducer(server);

        thread = new Thread(this::run, "TalkClient");
        thread.setDaemon(true);
    }

    /** @param listener Listener to add */
    public void addListener(final TalkClientListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final TalkClientListener listener)
    {
        if (! listeners.remove(listener))
            throw new IllegalStateException("Unknown listener");
    }
    /** Start client
     *  @see #shutdown()
     */
    public void start()
    {
        thread.start();
    }

    /** @return <code>true</code> if <code>start()</code> had been called */
    public boolean isRunning()
    {
        return thread.isAlive();
    }
    
    /** Background thread loop that checks for alarm tree updates */
    private void run()
    {
        try
        {
            while (running.get())
                checkUpdates();
        }
        catch (final Throwable ex)
        {
            if (running.get())
                logger.log(Level.SEVERE, "Alarm client model error", ex);
            // else: Intended shutdown
        }
        finally
        {
            consumer.close();
        }
    }
    
    /** Perform one check for updates */
    private void checkUpdates()
    {
        final ConsumerRecords<String, String> records = consumer.poll(100);
        for (final ConsumerRecord<String, String> record : records)
        {
            final String severity = record.key();
            final String description = record.value();
            for (TalkClientListener listener : listeners)
            {
                listener.messageRecieved(severity, description);
            }
        }
    }
    
    /** Stop client */
    public void shutdown()
    {
        running.set(false);
        consumer.wakeup();
        try
        {
            thread.join(2000);
        }
        catch (final InterruptedException ex)
        {
            logger.log(Level.WARNING, "Alarm client thread doesn't shut down", ex);
        }
        logger.info(thread.getName() + " shut down");

    }
}
