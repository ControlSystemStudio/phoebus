package org.phoebus.applications.eslog.model;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.phoebus.applications.eslog.archivedjmslog.LogMessage;
import org.phoebus.util.time.SecondsParser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.TextNode;

@JsonDeserialize(using = CustomEventDeserializer.class)
public class EventLogMessage extends LogMessage
{
    /** Property for message ID in RDB */
    public static final String ID = "ID"; //$NON-NLS-1$

    /** Property for Time when message was added to log */
    public static final String DATE = "CREATETIME"; //$NON-NLS-1$

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; //$NON-NLS-1$

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

    @SuppressWarnings("nls")
    static final String[] PROPERTY_NAMES = { "CREATETIME", "TEXT", "NAME",
            "CLASS", "USER", "HOST", "APPLICATION-ID", "SEVERITY" };

    @SuppressWarnings("nls")
    public static EventLogMessage fromElasticsearch(JsonParser parser)
            throws IOException
    {
        final var node = parser.getCodec().readTree(parser);
        EventLogMessage msg = new EventLogMessage();
        for (String name : EventLogMessage.PROPERTY_NAMES)
        {
            final var val = (TextNode) node.get(name);
            if (null != val) msg.properties.put(name, val.textValue());
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
        int r = this.getPropertyValue(EventLogMessage.DATE)
                .compareTo(o.getPropertyValue(EventLogMessage.DATE));
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
        return this.properties.keySet().iterator();
    }

    @Override
    public String getPropertyValue(final String id)
    {
        if (EventLogMessage.DELTA.equals(id))
        {
            return this.delta;
        }
        return this.properties.get(id);
    }

    @Override
    public Instant getTime()
    {
        try
        {
            final var formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
                    .withZone(ZoneId.systemDefault());
            return Instant.from(
                    formatter.parse(getPropertyValue(EventLogMessage.DATE)));
        }
        catch (NumberFormatException e)
        {
            System.err.println(getPropertyValue(EventLogMessage.DATE));
            e.printStackTrace();
            return Instant.ofEpochMilli(0);
        }
    }

    @Override
    public int hashCode()
    {
        return this.properties.hashCode();
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
        buf.append("Message ").append(this.getPropertyValue(EventLogMessage.ID))
                .append(":");
        Iterator<String> props = getProperties();
        while (props.hasNext())
        {
            String prop = props.next();
            if (EventLogMessage.ID.equals(prop))
            {
                continue;
            }
            buf.append("\n  ").append(prop).append(": ")
                    .append(getPropertyValue(prop));
        }
        return buf.toString();
    }

    /** Verify that all required fields are there. */
    public void verify() throws IllegalArgumentException
    {
        // this is the absolute minimum a message should have.
        if (!this.properties.containsKey(EventLogMessage.DATE)
                || !this.properties.containsKey(EventLogMessage.SEVERITY)
                || !this.properties.containsKey(EventLogMessage.TEXT))
        {
            throw new IllegalArgumentException("Invalid log message."); //$NON-NLS-1$
        }
    }
}
