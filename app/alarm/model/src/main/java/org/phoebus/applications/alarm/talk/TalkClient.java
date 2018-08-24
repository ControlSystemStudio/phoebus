/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Collections;
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
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.model.json.JsonTags;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Alarm Client Model for a *Talk topic.
 * <p>Given an alarm configuration name like "Accelerator", subscribes to the "AcceleratorTalk" topic for
 * alarm messages that can be displayed or annunciated.
 * <p> Largely based off of Kay Kasemir's {@link AlarmClient}
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class TalkClient
{
    private final CopyOnWriteArrayList<TalkClientListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    private final Thread thread;

    /** @param server - Kafka Server host:port
     *  @param config_name - Name of kafka config topic that the talk topic accompanies.
     */
    public TalkClient(final String server, final String config_name)
    {
        Objects.requireNonNull(server);
        Objects.requireNonNull(config_name);

        final List<String> topics = List.of(config_name + AlarmSystem.TALK_TOPIC_SUFFIX);
        consumer = KafkaHelper.connectConsumer(server, topics, Collections.emptyList());

        thread = new Thread(this::run, "TalkClient");
        thread.setDaemon(true);
    }

    /** @param listener - Listener to add */
    public void addListener(final TalkClientListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener - Listener to remove */
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

    /** Background thread loop that checks for alarm messages */
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
            String path = record.key();
            String jsonString = record.value();

            final JsonNode jn;
            try
            {
                jn = (JsonNode) JsonModelReader.parseJsonText(jsonString);
            } catch (Exception ex)
            {
                logger.log(Level.WARNING, "Parsing of talk message for " + path + " failed.", ex);
                continue;
            }

            // Extract the message info from the JSON and notify the listeners.

            final String   severity = jn.get(JsonTags.SEVERITY).textValue();
            final boolean standout = jn.get(JsonTags.STANDOUT).asBoolean();
            final String   message  = jn.get(JsonTags.TALK).textValue();

            try
            {
                for (final TalkClientListener listener : listeners)
                    listener.messageReceived(SeverityLevel.valueOf(severity), standout, message);
            }
            catch (final Exception ex)
            {
                logger.log(Level.WARNING,
                           "Talk error for " + severity +
                           ", " + message, ex);
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
            logger.log(Level.WARNING, "Talk client thread doesn't shut down", ex);
        }
        logger.info(thread.getName() + " shut down");
    }
}
