package testin;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.junit.Test;
import org.phoebus.pv.pvws.client.PVWS_Client;
import org.phoebus.pv.pvws.models.temp.ApplicationClientEchoMessage;
import org.phoebus.pv.pvws.models.temp.Message;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class Testin {
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