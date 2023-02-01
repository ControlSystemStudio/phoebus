/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.eslog.archivedjmslog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import org.phoebus.applications.eslog.Activator;
import org.phoebus.applications.eslog.util.JMSConnectionFactory;
import org.phoebus.applications.eslog.util.JMSConnectionListener;

/**
 * A connection to a JMS server configured to receive messages for several
 * topics.
 */
public class JMSReceiver
        implements ExceptionListener, MessageListener, JMSConnectionListener
{
    final String url;
    final String user;
    final String password;
    private volatile Connection connection = null;
    private volatile Session session = null;
    // key: topic name
    private volatile Map<String, MessageConsumer> consumers = new HashMap<>();
    private final Object lock = new Object();

    /**
     * Run flag. In principle we try to close the model properly. But in case
     * the main thread was still hung in the connection, it will not get to the
     * proper shutdown, and there is no perfect way to interrupt an ongoing
     * "failover" connection problem. So we set the 'run' flag to
     * <code>false</code> to suppress notifications and stop the main thread in
     * case it wakes up after a connection problem.
     */
    volatile boolean run = true;

    private Map<String, Set<LiveModel<?>>> listeners = new HashMap<>();

    private volatile String server_name = "disconnected";

    /**
     * Initialize
     *
     * @param url
     *            JMS server URL
     * @param user
     *            JMS user name or <code>null</code>
     * @param password
     *            JMS password or <code>null</code>
     * @param topic_names
     *            JMS topics, separated by comma
     * @throws Exception
     *             on error
     */
    public JMSReceiver(final String url, final String user,
            final String password)
    {
        Activator.checkParameterString(url, "url"); //$NON-NLS-1$
        Activator.logger
                .info(String.format("Creating JMS receiver for '%s'.", url)); //$NON-NLS-1$
        this.url = url;
        this.user = user;
        this.password = password;
        if (url == null || url.length() <= 0)
        {
            throw new IllegalArgumentException("No URL given");
        }
    }

    public void addListener(String topic, LiveModel<?> listener)
    {
        Activator.checkParameterString(topic, "topic"); //$NON-NLS-1$
        Activator.checkParameter(listener, "listener"); //$NON-NLS-1$
        synchronized (this.listeners)
        {
            this.listeners
                    .computeIfAbsent(topic,
                            t -> Collections.newSetFromMap(
                                    new WeakHashMap<LiveModel<?>, Boolean>()))
                    .add(listener);
        }
    }

    /** Must be called to release resources when no longer used */
    public void close()
    {
        this.run = false;
        synchronized (this)
        {
            this.notifyAll();
        }
    }

    /**
     * Connect to JMS; run in background thread
     *
     * @throws Exception
     *             on error
     */
    void connect() throws Exception
    {
        synchronized (this.lock)
        {
            if (null != this.connection)
            {
                return;
            }

            this.connection = JMSConnectionFactory.connect(this.url, this.user,
                    this.password);
            JMSConnectionFactory.addListener(this.connection, this);

            try
            {
                this.connection.setExceptionListener(this);
            }
            catch (LinkageError ex)
            {
                System.err.println(ex);
                throw (ex);
            }
            this.connection.start();
            this.session = this.connection.createSession(/* transacted */ false,
                    Session.AUTO_ACKNOWLEDGE);
        }

        updateSubscriptions();
        fireModelChanged();
    }

    /** Disconnect JMS. Called in background thread */
    void disconnect()
    {
        synchronized (this.lock)
        {
            if (null == this.connection)
            {
                return;
            }
            try
            {
                synchronized (this.consumers)
                {
                    this.consumers.forEach((t, c) -> {
                        if (null != c)
                        {
                            try
                            {
                                c.close();
                            }
                            catch (JMSException e)
                            {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });
                    this.consumers.replaceAll((t, c) -> null);
                }
                if (null != this.session)
                {
                    this.session.close();
                    this.session = null;
                }
                if (null != this.connection)
                {
                    this.connection.close();
                    this.connection = null;
                }
            }
            catch (Exception ex)
            {
                Activator.logger.log(Level.WARNING, "JMS shutdown error", ex); //$NON-NLS-1$
            }
        }
    }

    private void fireModelChanged()
    {
        // TODO Auto-generated method stub
    }

    /** @return name of JMS server */
    public String getServerName()
    {
        return this.server_name;
    }

    /** @see JMSConnectionListener */
    // @Override
    @Override
    public void linkDown()
    {
        Activator.logger.warning("JMS disconnected."); //$NON-NLS-1$
        this.server_name = "disconnected";
        fireModelChanged();
    }

    /** @see JMSConnectionListener */
    // @Override
    @Override
    public void linkUp(final String server)
    {
        Activator.logger.info("JMS connected to " + server + "."); //$NON-NLS-1$
        this.server_name = server;
        fireModelChanged();
    }

    /** @see ExceptionListener */
    @Override
    public void onException(final JMSException ex)
    {
        Activator.logger.log(Level.SEVERE, "JMS Exception", ex); //$NON-NLS-1$
    }

    /** @see MessageListener */
    @Override
    public void onMessage(Message message)
    {
        String topic;
        try
        {
            topic = ((Topic) message.getJMSDestination()).getTopicName();
        }
        catch (JMSException ex)
        {
            Activator.logger.log(Level.SEVERE, "JMS Exception", //$NON-NLS-1$
                    ex);
            return;
        }
        synchronized (this.listeners)
        {
            Set<LiveModel<?>> recipients = this.listeners.getOrDefault(topic,
                    null);

            if (null == recipients)
            {
                System.out.println("No recipients.");
                return;
            }
            recipients.forEach((r) -> {
                r.onMessage(message);
            });
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    public void removeListener(final LiveModel<?> listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.remove(listener);
            // TODO: shutdown, if empty
        }
    }

    public void start()
    {
        final Runnable connector = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    connect();
                    while (JMSReceiver.this.run)
                    {
                        synchronized (JMSReceiver.this)
                        {
                            JMSReceiver.this.wait();
                        }
                    }
                    disconnect();
                }
                catch (Exception ex)
                {
                    Activator.logger.log(Level.SEVERE,
                            "JMSMonitorConnector thread error", ex);
                }
            }
        };
        final Thread thread = new Thread(connector, "JMSReceiver"); //$NON-NLS-1$
        thread.setDaemon(true);
        thread.start();
    }

    void subscribeToTopic(final String t)
    {
        synchronized (this.consumers)
        {
            this.consumers.putIfAbsent(t, null);
        }
        updateSubscriptions();
    }

    private void updateSubscriptions()
    {
        synchronized (this.lock)
        {
            if (null == this.session)
            {
                return;
            }
            synchronized (this.consumers)
            {
                this.consumers.forEach((topic, c) -> {
                    if (null == c)
                    {
                        Topic t;
                        try
                        {
                            Activator.logger.info(String.format(
                                    "Creating consumer for '%s'.", topic)); //$NON-NLS-1$
                            t = this.session.createTopic(topic);
                            c = this.session.createConsumer(t);
                            c.setMessageListener(this);
                        }
                        catch (JMSException e)
                        {
                            e.printStackTrace();
                            return;
                        }
                        this.consumers.put(topic, c);
                    }
                });
            }
        }
    }
}