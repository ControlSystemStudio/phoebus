package org.phoebus.applications.uxanalytics.monitor.backend.database;

public class MockCheckConnectionHandler implements MockHandler {

    boolean appStatus;
    boolean sqlStatus;
    boolean graphDatabaseStatus;

    public MockCheckConnectionHandler(boolean appStatus, boolean sqlStatus, boolean graphDatabaseStatus) {
        this.appStatus = appStatus;
        this.sqlStatus = sqlStatus;
        this.graphDatabaseStatus = graphDatabaseStatus;
    }

    String generateConnectionStatus(boolean app, boolean sql, boolean graph){
        return "{\"applicationStatus\":\""+(appStatus?"OK":"NOK")+"\",\"mariaDatabaseStatus\":\""+(sqlStatus?"OK":"NOK")+"\",\"graphDatabaseStatus\":\""+(graphDatabaseStatus?"OK":"NOK")+"\"}";
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(generateConnectionStatus(appStatus, sqlStatus, graphDatabaseStatus).getBytes());
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
