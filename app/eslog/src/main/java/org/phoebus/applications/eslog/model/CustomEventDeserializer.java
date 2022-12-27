package org.phoebus.applications.eslog.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomEventDeserializer extends StdDeserializer<EventLogMessage>
{
    private static final long serialVersionUID = 1L;

    public CustomEventDeserializer()
    {
        this(null);
    }

    public CustomEventDeserializer(Class<?> vc)
    {
        super(vc);
    }

    @Override
    public EventLogMessage deserialize(JsonParser parser,
            DeserializationContext deserializer)
    {
        try
        {
            return EventLogMessage.fromElasticsearch(parser);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
