package org.phoebus.applications.uxanalytics.monitor;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Neo4JConnection implements BackendConnection{

    Logger logger = Logger.getLogger(Neo4JConnection.class.getName());
    Driver driver;

    public static final String PROTOCOL = "neo4j://";

    @Override
    public Boolean connect(String host, Integer port, String username, String password) {
        try {
            driver = GraphDatabase.driver(PROTOCOL+host, AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to connect to " + host, ex);
            return false;
        }
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public String getDefaultPort() {
        return "7687";
    }

    @Override
    public String getDefaultUsername() {
        return "neo4j";
    }

    @Override
    public Integer tearDown() {
        driver.close();
        return 0;
    }
}
