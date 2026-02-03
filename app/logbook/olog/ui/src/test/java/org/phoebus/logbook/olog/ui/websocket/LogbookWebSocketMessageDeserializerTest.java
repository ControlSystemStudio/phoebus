/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.logbook.olog.ui.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.phoebus.core.websocket.common.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.*;

public class LogbookWebSocketMessageDeserializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    public LogbookWebSocketMessageDeserializerTest() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(WebSocketMessage.class, new LogbookWebSocketMessageDeserializer(WebSocketMessage.class));
        objectMapper.registerModule(module);
    }

    @Test
    public void testDeserialize_1() throws Exception{
        WebSocketMessage webSocketMessage = objectMapper.readValue(getClass().getResourceAsStream("/websocketexample1.json"), new TypeReference<>() {
        });
        assertEquals(LogbookMessageType.NEW_LOG_ENTRY, webSocketMessage.messageType());
    }

    @Test
    public void testDeserialize_2() throws Exception{
        WebSocketMessage webSocketMessage = objectMapper.readValue(getClass().getResourceAsStream("/websocketexample2.json"), new TypeReference<>() {
        });
        assertEquals(LogbookMessageType.LOG_ENTRY_UPDATED, webSocketMessage.messageType());
        assertEquals("logEntryId", webSocketMessage.payload());
    }

    @Test
    public void testDeserialize_3() throws Exception{
        WebSocketMessage webSocketMessage = objectMapper.readValue(getClass().getResourceAsStream("/websocketexample3.json"), new TypeReference<>() {
        });
        assertNull(webSocketMessage);
    }
}
