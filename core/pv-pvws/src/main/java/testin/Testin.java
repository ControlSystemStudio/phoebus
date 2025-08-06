package testin;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;
import org.phoebus.pv.pvws.client.PVWS_Client;
import org.phoebus.pv.pvws.models.temp.ApplicationClientEchoMessage;
import org.phoebus.pv.pvws.models.temp.Message;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.*;

public class Testin {



    @Test
    public void testWebSocketBadUrl() throws Exception {
        URI uri = new URI("ws://localhost:8080");
        AtomicBoolean clientClosed = new AtomicBoolean(false);

        CountDownLatch latch = new CountDownLatch(1);
        ObjectMapper mapper = new ObjectMapper();

        WebSocketClient client = new PVWS_Client(uri, latch, mapper) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected!");
                latch.countDown();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ Disconnected. Reason: " + reason);
                closeClient();
                clientClosed.set(true);
                latch.countDown();
            }
        };

        client.connect();
        latch.await(5,  TimeUnit.SECONDS);
        // Wait for the connection or timeout after 5 seconds
        //latch.countDown();
        boolean closed = clientClosed.get();

        //assertTrue("WebSocket did not connect successfully", connected);
        //assertTrue("WebSocket connection is not open", client.isOpen());
        assertTrue("WebSocket did not close gracefully", closed);
    }

    @Test
    public void testWebSocketConnectionSuccess() throws Exception {
        URI uri = new URI("ws://localhost:8080/pvws/pv");

        CountDownLatch latch = new CountDownLatch(1);
        ObjectMapper mapper = new ObjectMapper();

        WebSocketClient client = new PVWS_Client(uri, latch, mapper) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected!");
                latch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };

        client.connect();

        // Wait for the connection or timeout after 5 seconds
        boolean connected = latch.await(5, TimeUnit.SECONDS);

        assertTrue("WebSocket did not connect successfully", connected);
        assertTrue("WebSocket connection is not open", client.isOpen());
    }


    @Test
    public void testWebSocketEcho() throws Exception {
        //add url
        URI uri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(2); // one countdown when onopen and the other for on message
        List<String> received = new CopyOnWriteArrayList<>();

        //make client
        ObjectMapper mapper = new ObjectMapper();
        //create new client and use message
        WebSocketClient client = new PVWS_Client(uri, latch, mapper ) {
            @Override
            public void onMessage(String message) {
                received.add(message);
                System.out.println(message);
                latch.countDown();

            }
        };
        client.connectBlocking();
        ApplicationClientEchoMessage msg = new ApplicationClientEchoMessage();
        String json = mapper.writeValueAsString(msg);
        client.send(json);
        //Thread.sleep(500);

        assertTrue("Did not receive any message", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Message list is empty", !received.isEmpty());
        //assertTrue(latch.await(5, TimeUnit.SECONDS));


        ApplicationClientEchoMessage actualMessage = mapper.readValue(received.get(0), ApplicationClientEchoMessage.class);

        //assertEquals("echo", received.get(0));
        assertEquals("echo", actualMessage.getType());
        assertEquals(null, actualMessage.getBody());
        assertEquals(null, actualMessage.getFoo());
    }

}