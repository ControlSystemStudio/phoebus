package org.phoebus.applications.eslog.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.phoebus.applications.eslog.archivedjmslog.LogMessage;
import org.phoebus.util.time.SecondsParser;

import co.elastic.clients.elasticsearch.core.search.Hit;

public class EventLogMessage extends LogMessage
{
    /** Property for Time when message was added to log */
    public static final String DATE = "CREATETIME"; //$NON-NLS-1$

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; //$NON-NLS-1$
    public static final DateTimeFormatter date_formatter = DateTimeFormatter
            .ofPattern(DATE_FORMAT).withZone(ZoneId.systemDefault());

    public static final String HOST = "HOST"; //$NON-NLS-1$

    /** Property for message severity */
    public static final String SEVERITY = "SEVERITY"; //$NON-NLS-1$

    /** Property for message text */
    public static final String TEXT = "TEXT"; //$NON-NLS-1$

    /** Property for Time in seconds from previous message to this message */
    public static final String DELTA = "DELTA"; //$NON-NLS-1$

    /** Map of property names and values */
    private final Map<String, String> properties = new HashMap<>();
    private String delta = null;
    private Instant date;

    @SuppressWarnings("nls")
    static final public String[] PROPERTY_NAMES = { "TEXT", "NAME", "CLASS",
            "USER", "HOST", "APPLICATION-ID", "SEVERITY" };

    @SuppressWarnings("nls")
    public static EventLogMessage fromElasticsearch(Hit<EventLogMessage> hit)
    {
        final var msg = new EventLogMessage();
        msg.date = Instant.ofEpochMilli(
                Long.valueOf(hit.fields().get(EventLogMessage.DATE).toJson()
                        .asJsonArray().getString(0)));
        for (String name : EventLogMessage.PROPERTY_NAMES)
        {
            final var val = hit.fields().get(name).toJson().asJsonArray()
                    .getString(0);
            if (null != val) msg.properties.put(name, val);
        }
        msg.verify();
        return msg;
    }

    public static EventLogMessage fromJMS(MapMessage message)
    {
        try
        {
            EventLogMessage msg = new EventLogMessage();
            for (String name : EventLogMessage.PROPERTY_NAMES)
            {
                msg.properties.put(name, message.getString(name));
            }
            msg.properties.put("ID", message.getJMSMessageID()); //$NON-NLS-1$
            final var DATE = message.getString(EventLogMessage.DATE);
            try
            {
                msg.date = Instant.parse(DATE);
            }
            catch (DateTimeParseException e)
            {
                msg.date = Instant.from(date_formatter.parse(DATE));
            }
            msg.verify();
            return msg;
        }
        catch (JMSException ex)
        {
            return null;
        }
    }

    @Override
    public int compareTo(final LogMessage other)
    {
        if (!(other instanceof EventLogMessage))
        {
            return 0;
        }
        EventLogMessage o = (EventLogMessage) other;

        // first compare by date
        int r = this.date.compareTo(o.date);
        if (0 != r)
        {
            return r;
        }

        // then by anything else that might be different
        for (String s : EventLogMessage.PROPERTY_NAMES)
        {
            r = this.getPropertyValue(s).compareTo(o.getPropertyValue(s));
            if (0 != r)
            {
                return r;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof EventLogMessage))
        {
            return false;
        }
        EventLogMessage msg = (EventLogMessage) other;
        if (this.date != msg.date)
        {
            return false;
        }
        for (String s : EventLogMessage.PROPERTY_NAMES)
        {
            if (!this.getPropertyValue(s).equals(msg.getPropertyValue(s)))
            {
                return false;
            }
        }
        return true;
    }

    /** @return Iterator over all properties in this message */
    public Iterator<String> getProperties()
    {
        Set<String> keys = new HashSet<>();
        keys.addAll(this.properties.keySet());
        keys.add(EventLogMessage.DATE);
        return keys.iterator();
    }

    @Override
    public String getPropertyValue(final String id)
    {
        if (EventLogMessage.DELTA.equals(id))
        {
            return this.delta;
        }
        if (EventLogMessage.DATE.equals(id))
        {
            return EventLogMessage.date_formatter.format(date);
        }

        return this.properties.get(id);
    }

    @Override
    public Instant getTime()
    {
        return this.date;
    }

    @Override
    public int hashCode()
    {
        return this.properties.hashCode() ^ this.date.hashCode();
    }

    /**
     * Set 'delta'. Public, but really only meant to be called by code that
     * constructs the message to overcome the problem that we can only configure
     * the 'delta' after constructing the _next_ message.
     */
    public void setDelta(long delta_millis)
    {
        this.delta = SecondsParser.formatSeconds(delta_millis / 1000.0);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Message:");
        Iterator<String> props = getProperties();
        while (props.hasNext())
        {
            String prop = props.next();
            buf.append("\n  ").append(prop).append(": ")
                    .append(getPropertyValue(prop));
        }
        return buf.toString();
    }

    /** Verify that all required fields are there. */
    public void verify() throws IllegalArgumentException
    {
        // this is the absolute minimum a message should have.
        if (!this.properties.containsKey(EventLogMessage.SEVERITY)
                || !this.properties.containsKey(EventLogMessage.TEXT))
        {
            throw new IllegalArgumentException("Invalid log message."); //$NON-NLS-1$
        }
    }
}