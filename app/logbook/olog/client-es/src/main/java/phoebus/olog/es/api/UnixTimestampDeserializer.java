package org.phoebus.olog.es.api;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class UnixTimestampDeserializer extends JsonDeserializer<Instant> {
    Logger logger = Logger.getLogger(UnixTimestampDeserializer.class.getName());

    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String timestamp = jp.getText().trim();
        try {
            return Instant.ofEpochMilli(Long.valueOf(timestamp));
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Unable to deserialize timestamp: " + timestamp, e);
            return null;
        }
    }
}