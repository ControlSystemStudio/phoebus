package org.phoebus.applications.uxanalytics.monitor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDBConnection implements BackendConnection{

    Logger logger = Logger.getLogger(MongoDBConnection.class.getName());

    public static final String PROTOCOL = "mongodb://";

    private MongoClient mongoClient = null;
    private ImageClient imageClient = null;

    @Override
    public Boolean connect(String hostname, Integer port, String username, String password) {
        String uri = PROTOCOL + username + ":" + password + "@" + hostname + ":" + port.toString();
        try {
            mongoClient = MongoClients.create(uri);
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to connect to " + hostname, ex);
            return false;
        }
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public String getDefaultPort() {
        return "27017";
    }

    public Integer connect(String host, String port, String user, String password, ImageClient imageClient) {
        this.imageClient = imageClient;
        if(connect(host, Integer.parseInt(port), user, password)){
            return 0;
        }
        return -1;
    }

    public void setImageClient(ImageClient imageClient) {
        this.imageClient = imageClient;
    }

    public boolean hasImageConnection() {
        return imageClient != null;
    }

    @Override
    public Integer tearDown() {
        mongoClient.close();
        return 0;
    }

    @Override
    public void handleClick(ActiveTab who, Integer x, Integer y) {
        logger.log(Level.INFO, "MongoDB Connection would've handled click at " + x + ", " + y + " from " + who);

    }

}
