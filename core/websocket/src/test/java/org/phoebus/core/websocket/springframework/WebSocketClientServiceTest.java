/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket.springframework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebSocketClientServiceTest {

    @Test
    public void testGetWebsocketConnectionParameters_1() throws Exception {
        String restUrl = "http://localhost:8080";
        assertEquals("ws://localhost:8080/web-socket", WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }

    @Test
    public void testGetWebsocketConnectionParameters_2() throws Exception {
        String restUrl = "https://localhost:8080";
        assertEquals("wss://localhost:8080/web-socket", WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }

    @Test
    public void testGetWebsocketConnectionParameters_3() throws Exception {
        String restUrl = "https://localhost:8080/path-element";
        assertEquals("wss://localhost:8080/path-element/web-socket", WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }

    @Test
    public void testGetWebsocketConnectionParameters_4() throws Exception {
        String restUrl = "https://localhost/path-element";
        assertEquals("wss://localhost/path-element/web-socket", WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }

    @Test
    public void testGetWebsocketConnectionParameters_5(){
        String restUrl = "https://localhost:8080/path element";
        assertThrows(Exception.class, () -> WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }

    @Test
    public void testGetWebsocketConnectionParameters_7(){
        String restUrl = "invalid";
        assertThrows(Exception.class, () -> WebSocketClientService.getWebsocketConnectUrl(restUrl));
    }
}
