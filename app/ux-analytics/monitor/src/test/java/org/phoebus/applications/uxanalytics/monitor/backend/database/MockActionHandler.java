package org.phoebus.applications.uxanalytics.monitor.backend.database;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.FutureTask;

public class MockActionHandler implements MockHandler{

    private final ObjectMapper objectMapper = new ObjectMapper();
    public boolean good = true;
    public String received_body = null;
    public FutureTask<Boolean> received = new FutureTask<>(() -> {
        return true;
    });

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            received_body = new String(requestBody);
            received.run();
            if (good) {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("{\"nodesCreated\":1,\"nodesDeleted\":0,\"relationshipsCreated\":1,\"relationshipsDeleted\":0,\"propertiesSet\":3,\"labelsAdded\":1,\"labelsRemoved\":0,\"indexesAdded\":0,\"indexesRemoved\":0,\"constraintsAdded\":0,\"constraintsRemoved\":0,\"systemUpdates\":0}".getBytes());
            }
            else {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().write("{\"error\": \"Internal Server Error\"}".getBytes());
            }
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
