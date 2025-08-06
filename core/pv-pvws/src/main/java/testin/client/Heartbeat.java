package testin.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;
import org.phoebus.pv.pvws.client.PVWS_Client;
import org.phoebus.pv.pvws.models.temp.ApplicationClientEchoMessage;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class Heartbeat {



    @Test
    public void ping() throws Exception {
        //add url
        AtomicBoolean receivedPong = new AtomicBoolean(false);
        URI uri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(2); // one countdown when onopen and the other for on message

        //make client
        ObjectMapper mapper = new ObjectMapper();
        //create new client and use message
        WebSocketClient client = new PVWS_Client(uri, latch, mapper ) {
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            try {
                System.out.println("Connected to server");
                this.sendPing();
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Exception in onOpen: " + e.getMessage());
                e.printStackTrace();
            }
        }
            @Override
            public void onWebsocketPong(WebSocket conn, Framedata f) {
                System.out.println("Received Pong frame"); // you could also comment this out to test the heartbeat timeout just for visual clarity
                super.onWebsocketPong(conn, f);
                receivedPong.set(true);
                closeClient();
                latch.countDown();
            }
        };
        client.connect();
        Thread.sleep(1000);
        assertTrue(receivedPong.get());
    }



    /*
    @Test
    public void missedPong() throws Exception {

        AtomicInteger pingCount = new AtomicInteger(0);
        AtomicBoolean reconnected = new AtomicBoolean(false);


        //add url
        AtomicBoolean receivedPong = new AtomicBoolean(false);
        URI uri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(2); // one countdown when onopen and the other for on message

        //make client
        ObjectMapper mapper = new ObjectMapper();
        //create new client and use message
        WebSocketClient client = new PVWS_Client(uri, latch, mapper ) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                try {
                    System.out.println("Connected to server");
                    this.sendPing();
                    latch.countDown();
                } catch (Exception e) {
                    System.err.println("Exception in onOpen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            @Override
            public void onWebsocketPong(WebSocket conn, Framedata f) {
                System.out.println("Received Pong frame"); // you could also comment this out to test the heartbeat timeout just for visual clarity
                super.onWebsocketPong(conn, f);
                receivedPong.set(true);
                closeClient();
                latch.countDown();
            }
            @Override
            public void sendPing() {
                pingCount.incrementAndGet();
    ///..
            }


            public void attemptReconnect() {
                reconnected.set(true);
            }
        };
        client.connect();
        Thread.sleep(1000);
        assertTrue(receivedPong.get());
    }
       */
}
