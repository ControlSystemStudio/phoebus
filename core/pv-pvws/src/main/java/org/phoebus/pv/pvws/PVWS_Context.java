package org.phoebus.pv.pvws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.pv.pvws.client.PVWS_Client;
import org.phoebus.pv.pvws.models.temp.SubscribeMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PVWS_Context {

    // Singleton instance of PVWS_Context
    private static volatile PVWS_Context instance;

    private final PVWS_Client client;


    private PVWS_Context() throws URISyntaxException, InterruptedException, JsonProcessingException {
        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        this.client = initializeClient(serverUri);
    }

    // Thread-safe singleton getter with lazy initialization
    public static PVWS_Context getInstance() throws URISyntaxException, InterruptedException, JsonProcessingException {
        if (instance == null) {
            synchronized (PVWS_Context.class) {
                if (instance == null) {
                    instance = new PVWS_Context();
                }
            }
        }
        return instance;
    }

    // Public access to the client
    public PVWS_Client getClient() {
        return client;
    }

    // Initialization logic
    private PVWS_Client initializeClient(URI serverUri) throws URISyntaxException, InterruptedException, JsonProcessingException {
        CountDownLatch latch = new CountDownLatch(1);
        ObjectMapper mapper = new ObjectMapper();
        PVWS_Client client = new PVWS_Client(serverUri, latch, mapper);

        client.connect();
        latch.await();

        return client;
    }

    // Shutdown method (optional)
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    public void clientSubscribe(String base_name) throws JsonProcessingException {

        SubscribeMessage message = new SubscribeMessage();
        message.setType("subscribe");

        List<String> pv = new ArrayList<>(List.of(base_name));
        message.setPvs(pv);
        String json = getClient().mapper.writeValueAsString(message);
        getClient().send(json);
    }
}
