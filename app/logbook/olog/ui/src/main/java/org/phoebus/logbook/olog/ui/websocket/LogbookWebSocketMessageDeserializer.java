/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.logbook.olog.ui.websocket;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.phoebus.core.websocket.common.WebSocketMessage;

/**
 * Custom JSON deserializer of {@link WebSocketMessage}s particular to the logbook app.
 */
public class LogbookWebSocketMessageDeserializer extends StdDeserializer<WebSocketMessage<?>> {

    public LogbookWebSocketMessageDeserializer(Class<?> clazz) {
        super(clazz);
    }

    /**
     * Deserializes a save-and-restore {@link WebSocketMessage}.
     *
     * @param jsonParser Parsed used for reading JSON content
     * @param context    Context that can be used to access information about
     *                   this deserialization activity.
     * @return A {@link WebSocketMessage} object, or <code>null</code> if deserialization fails, e.g. due to
     * unknown/invalid {@link org.phoebus.core.websocket.common.MessageType} or <code>null</code> payload.
     */
    @Override
    public WebSocketMessage<?> deserialize(JsonParser jsonParser, DeserializationContext context) {
        try {
            JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
            LogbookMessageType logbookMessageType = LogbookMessageType.valueOf(rootNode.get("messageType").asText());
            JsonNode payload = rootNode.get("payload");
            switch (logbookMessageType) {
                case NEW_LOG_ENTRY -> {
                    return new WebSocketMessage<>(logbookMessageType, null);
                }
                case LOG_ENTRY_UPDATED -> {
                    return new WebSocketMessage<>(logbookMessageType, payload.textValue());
                }
                case SHOW_BANNER -> throw new RuntimeException("SHOW_BANNER not yet implemented");

            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
