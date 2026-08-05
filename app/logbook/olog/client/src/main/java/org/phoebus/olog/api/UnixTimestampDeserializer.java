package org.phoebus.olog.api;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

class UnixTimestampDeserializer extends StdDeserializer<Instant> {
    Logger logger = Logger.getLogger(UnixTimestampDeserializer.class.getName());

    public UnixTimestampDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
        String timestamp = jp.getText().trim();
        try {
            return Instant.ofEpochMilli(Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Unable to deserialize timestamp: " + timestamp, e);
            return null;
        }
    }
}