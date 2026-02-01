/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.core.websocket.WebSocketMessage;

/**
 * Custom JSON deserializer of {@link WebSocketMessage}s particular to save-and-restore.
 */
public class SaveAndRestoreWebSocketMessageDeserializer extends StdDeserializer<WebSocketMessage<?>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SaveAndRestoreWebSocketMessageDeserializer(Class<?> clazz) {
        super(clazz);
    }

    /**
     * Deserializes a save-and-restore {@link WebSocketMessage}.
     *
     * @param jsonParser Parsed used for reading JSON content
     * @param context    Context that can be used to access information about
     *                   this deserialization activity.
     * @return A {@link WebSocketMessage} object, or <code>null</code> if deserialization fails, e.g. due to
     * unknown/invalid {@link org.phoebus.core.websocket.MessageType} or <code>null</code> payload.
     */
    @Override
    public WebSocketMessage<?> deserialize(JsonParser jsonParser, DeserializationContext context) {
        try {
            JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
            SaveAndRestoreMessageType saveAndRestoreMessageType = SaveAndRestoreMessageType.valueOf(rootNode.get("messageType").asText());
            JsonNode payload = rootNode.get("payload");
            switch (saveAndRestoreMessageType) {
                case NODE_ADDED, NODE_REMOVED, FILTER_REMOVED-> {
                    return new WebSocketMessage<>(saveAndRestoreMessageType, payload.textValue());
                }
                case NODE_UPDATED -> {
                    Node node = objectMapper.readValue(payload.toString(), Node.class);
                    return new WebSocketMessage<>(saveAndRestoreMessageType, node);
                }
                case FILTER_ADDED_OR_UPDATED -> {
                    Filter filter = objectMapper.readValue(payload.toString(), Filter.class);
                    return new WebSocketMessage<>(saveAndRestoreMessageType, filter);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}

