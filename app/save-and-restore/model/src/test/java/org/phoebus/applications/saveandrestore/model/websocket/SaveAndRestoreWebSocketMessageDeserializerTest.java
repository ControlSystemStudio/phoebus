/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SaveAndRestoreWebSocketMessageDeserializerTest {

    private ObjectMapper mapper = new ObjectMapper();

    public SaveAndRestoreWebSocketMessageDeserializerTest(){
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SaveAndRestoreWebSocketMessage.class,
                new WebMessageDeserializer(SaveAndRestoreWebSocketMessage.class));
        mapper.registerModule(module);
    }

    @Test
    public void test1() {
        try {
            SaveAndRestoreWebSocketMessage<Node> webSocketMessage =
                    mapper.readValue(getClass().getResourceAsStream("/websocketexample2.json"), new TypeReference<>() {
                    });
            assertEquals(MessageType.NODE_UPDATED, webSocketMessage.messageType());
            assertEquals("a", webSocketMessage.payload().getUniqueId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void test2() {
        try {
            SaveAndRestoreWebSocketMessage<String> webSocketMessage =
                    mapper.readValue(getClass().getResourceAsStream("/websocketexample1.json"), new TypeReference<>() {
                    });
            assertEquals(MessageType.NODE_ADDED, webSocketMessage.messageType());
            assertEquals("parentNodeId", webSocketMessage.payload());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test3() {
        try {
            SaveAndRestoreWebSocketMessage<Filter> webSocketMessage =
                    mapper.readValue(getClass().getResourceAsStream("/websocketexample3.json"), new TypeReference<>() {
                    });
            assertEquals(MessageType.FILTER_ADDED_OR_UPDATED, webSocketMessage.messageType());
            assertEquals("myFilter", webSocketMessage.payload().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test4() {
        try {
            SaveAndRestoreWebSocketMessage<Filter> webSocketMessage =
                    mapper.readValue(getClass().getResourceAsStream("/websocketexample4.json"), new TypeReference<>() {
                    });
            assertNull(webSocketMessage);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test5() {
        try {
            SaveAndRestoreWebSocketMessage<Node> webSocketMessage =
                    mapper.readValue(getClass().getResourceAsStream("/websocketexample5.json"), new TypeReference<>() {
                    });
            assertNull(webSocketMessage.payload());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
