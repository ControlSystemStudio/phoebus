package org.phoebus.applications.eslog.archivedjmslog;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Level;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Topic;

import org.phoebus.applications.eslog.Activator;

/**
 * A model receiving log messages via JMS.
 */
public class LiveModel<T extends LogMessage> extends Model
{
    protected String dateField;
    private Set<LiveModelListener<T>> listeners = Collections
            .newSetFromMap(new WeakHashMap<LiveModelListener<T>, Boolean>());
    private volatile boolean running = false;
    private Function<MapMessage, T> parser;

    /**
     * Initialize the model.
     *
     * @param receiver
     *            The JMSReceiver connected to the JMS server.
     * @param topic
     *            The topic to receive messages for.
     * @param dateField
     *            The name of the field in the message that represents the time
     *            stamp of the message.
     * @param parser
     *            The function to call to convert the received MapMessage to the
     *            required type of LogMessage.
     */
    public LiveModel(JMSReceiver receiver, String topic, String dateField,
            Function<MapMessage, T> parser)
    {
        Activator.checkParameter(receiver, "receiver"); //$NON-NLS-1$
        Activator.checkParameterString(topic, "topic"); //$NON-NLS-1$
        Activator.checkParameterString(dateField, "dateField"); //$NON-NLS-1$
        // this adds only a weak reference.
        // ⇒ we need not take care of removing ourselves from the list.
        receiver.addListener(topic, this);
        this.dateField = dateField;
        this.parser = parser;
    }

    public void addListener(LiveModelListener<T> listener)
    {
        Activator.checkParameter(listener, "listener"); //$NON-NLS-1$
        synchronized (this.listeners)
        {
            this.listeners.add(listener);
        }
    }

    void distributeMessage(String topic, T entry)
    {
        synchronized (this.listeners)
        {
            this.listeners.forEach((r) -> {
                try
                {
                    r.newMessage(entry);
                }
                catch (Throwable ex)
                {
                    Activator.logger
                            .warning("Notification failed: " + ex.getMessage()); //$NON-NLS-1$
                }
            });
        }
    }

    protected static boolean matchFilter(MapMessage message,
            PropertyFilter filter)
    {
        try
        {
            if (filter instanceof StringPropertyFilter)
            {
                StringPropertyFilter f = (StringPropertyFilter) filter;
                String p = ((StringPropertyFilter) filter).getProperty();
                String v = message.getString(p);
                if (null == v)
                {
                    return !filter.isInverted();
                }
                return v.contains(f.getPattern()) != filter.isInverted();
            }
            else if (filter instanceof StringPropertyMultiFilter)
            {
                StringPropertyMultiFilter f = (StringPropertyMultiFilter) filter;
                String p = ((StringPropertyMultiFilter) filter).getProperty();
                String v = message.getString(p);
                if (null == v)
                {
                    return !filter.isInverted();
                }
                for (String pat : f.getPatterns())
                {
                    if (v.contains(pat) != filter.isInverted())
                    {
                        return true;
                    }
                }
                return false;
            }
            else
            {
                throw new IllegalArgumentException(
                        "Filter type not supported."); //$NON-NLS-1$
            }
        }
        catch (JMSException ex)
        {
            return false;
        }
    }

    /**
     * Handle a received MapMessage.
     *
     * @param message
     *            The MapMessage
     * @return The parsed LogMessage.
     */
    protected T handleMapMessage(MapMessage message)
    {
        // apply message filters
        synchronized (this)
        {
            if (null != this.filters)
            {
                for (PropertyFilter f : this.filters)
                {
                    if (!matchFilter(message, f))
                    {
                        return null;
                    }
                }
            }
        }

        return this.parser.apply(message);
    }

    public void onMessage(Message message)
    {
        if (!this.running)
        {
            return;
        }
        try
        {
            T log = parseJMSMessage(message);
            if (null == log)
            {
                return;
            }
            distributeMessage(
                    ((Topic) message.getJMSDestination()).getTopicName(), log);
        }
        catch (Throwable ex)
        {
            Activator.logger.log(Level.SEVERE, "Message handling error", ex); //$NON-NLS-1$
        }
    }

    /**
     * Handle incoming JMS messages.
     *
     * The default implementation handles MapMessage objects by calling
     * {@link #handleMapMessage(MapMessage)}. Override if your message type is
     * not MapMessage.
     *
     * @param message
     *            The Message received via JMS.
     * @return The LogMessage to pass to the registered Listeners.
     * @throws Exception
     *             On any error.
     */
    protected T parseJMSMessage(Message message) throws Exception
    {
        if (message instanceof MapMessage)
        {
            return handleMapMessage((MapMessage) message);
        }
        else
        {
            Activator.logger.log(Level.WARNING, "Message type {0} not handled", //$NON-NLS-1$
                    message.getClass().getName());
            return null;
        }
    }

    public void removeListener(LiveModelListener<T> listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.remove(listener);
        }
    }

    /** Start receiving messages. */
    public void start()
    {
        this.running = true;
    }

    /** Stop receiving messages. */
    public void stop()
    {
        this.running = false;
    }
}
