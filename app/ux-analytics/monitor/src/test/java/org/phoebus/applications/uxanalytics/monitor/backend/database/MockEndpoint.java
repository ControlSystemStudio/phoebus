package org.phoebus.applications.uxanalytics.monitor.backend.database;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import java.net.InetSocketAddress;

public class MockEndpoint {

private HttpServer server;
    private MockCheckConnectionHandler checkConnectionHandler = new MockCheckConnectionHandler(true, true, true);
    private MockClickHandler clickHandler = new MockClickHandler();
    private MockActionHandler actionHandler = new MockActionHandler();


    public MockEndpoint() {

    }

    public void setCheckConnectionHandler(MockCheckConnectionHandler checkConnectionHandler) {
        this.checkConnectionHandler = checkConnectionHandler;
    }

    public void setClickHandler(MockClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    public void setActionHandler(MockActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void start() {
        try {
            InetSocketAddress address = new InetSocketAddress(11111);
            server = HttpServer.create();
            server.createContext("/analytics/checkConnection", checkConnectionHandler);
            server.createContext("/analytics/recordClick", clickHandler);
            server.createContext("/analytics/recordNavigation",actionHandler);
            server.bind(address, 0);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        server.stop(0);
    }
}
