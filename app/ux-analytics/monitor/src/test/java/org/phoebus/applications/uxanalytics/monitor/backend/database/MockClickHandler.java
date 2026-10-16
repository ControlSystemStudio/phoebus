package org.phoebus.applications.uxanalytics.monitor.backend.database;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class MockClickHandler implements MockHandler{

    boolean good = true;
    String received_body = null;
    FutureTask<Boolean> received = new FutureTask<>(() -> {
        return true;
    });

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) {

        try {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            received_body = new String(requestBody, StandardCharsets.UTF_8);
            received.run();
            if(good){
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("{\"id\": 424242}".getBytes());
            }
            else{
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().write("{\"error\": \"Internal Server Error\"}".getBytes());
            }
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
