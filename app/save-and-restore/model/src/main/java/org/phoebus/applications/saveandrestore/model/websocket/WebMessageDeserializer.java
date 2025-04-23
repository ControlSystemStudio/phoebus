/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;

public class WebMessageDeserializer extends StdDeserializer<SaveAndRestoreWebSocketMessage> {

    private ObjectMapper objectMapper = new ObjectMapper();

    public WebMessageDeserializer(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public SaveAndRestoreWebSocketMessage<? extends Object> deserialize(JsonParser jsonParser,
                                                                        DeserializationContext context) {
        try {
            JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
            String messageType = rootNode.get("messageType").asText();
            switch (MessageType.valueOf(messageType)) {
                case NODE_ADDED, NODE_REMOVED, FILTER_REMOVED-> {
                    SaveAndRestoreWebSocketMessage<String> saveAndRestoreWebSocketMessage =
                            objectMapper.readValue(rootNode.toString(), SaveAndRestoreWebSocketMessage.class);
                    return saveAndRestoreWebSocketMessage;
                }
                case NODE_UPDATED -> {
                    SaveAndRestoreWebSocketMessage<Node> saveAndRestoreWebSocketMessage = objectMapper.readValue(rootNode.toString(), new TypeReference<>() {
                    });
                    return saveAndRestoreWebSocketMessage;
                }
                case FILTER_ADDED_OR_UPDATED -> {
                    SaveAndRestoreWebSocketMessage<Filter> saveAndRestoreWebSocketMessage = objectMapper.readValue(rootNode.toString(), new TypeReference<>() {
                    });
                    return saveAndRestoreWebSocketMessage;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}

