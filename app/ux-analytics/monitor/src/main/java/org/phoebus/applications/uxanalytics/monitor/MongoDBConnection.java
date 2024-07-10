package org.phoebus.applications.uxanalytics.monitor;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.Buffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;

public class MongoDBConnection implements BackendConnection{

    Logger logger = Logger.getLogger(MongoDBConnection.class.getName());

    public static final String PROTOCOL = "mongodb://";

    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private ImageClient imageClient = null;

    @Override
    public Boolean connect(String hostname, Integer port, String username, String password) {
        String uri;
        if(username == null || password == null || username.isEmpty() || password.isEmpty())
            uri = PROTOCOL + hostname + ":" + port.toString();
        else
            uri = PROTOCOL + username + ":" + password + "@" + hostname + ":" + port.toString();
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("phoebus-analytics");
            database.createCollection("clicks");
            UXAMonitor.getInstance().notifyConnectionChange(this);
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

    static BufferedImage getSnapshot(ActiveTab who) {
        Node jfxNode = who.getParentTab().getContent();
        SnapshotParameters params = new SnapshotParameters();
        WritableImage snapshot = jfxNode.snapshot(params, null);
        return SwingFXUtils.fromFXImage(snapshot, null);
    }

    @Override
    public void handleClick(ActiveTab who, Integer x, Integer y) {
        if(database != null && imageClient == null){
            imageClient = MongoDBImageClient.getInstance();
            ((MongoDBImageClient) imageClient).connect(mongoClient);
        }
        Platform.runLater(() -> {
            String path = FileUtils.analyticsPathForTab(who);
            logger.log(Level.FINE, "MongoDB Connection handled click at " + x + ", " + y + " from " + path);
            double zoom = who.getZoom();
            Integer clickX = (int) (x / zoom);
            Integer clickY = (int) (y / zoom);
            if (imageClient!=null && !imageClient.imageExists(URI.create(path))) {
                logger.log(Level.INFO, "Uploading image for " + who + " to " + path);
                try {
                    ((DisplayRuntimeInstance) who.getParentTab().getApplication()).getRepresentation_init().get(1, TimeUnit.SECONDS);
                    BufferedImage snapshot = getSnapshot(who);
                    imageClient.uploadImage(URI.create(path), snapshot);
                }
                catch (Exception ex) {
                    logger.log(Level.WARNING, "Failed to upload image for " + who + " to " + path, ex);
                }
            }
            //write a click event to mongodb
            Document clickEvent = new Document()
                    .append("id", UUID.randomUUID())
                    .append("x", clickX)
                    .append("y", clickY)
                    .append("path", path)
                    .append("time", Instant.now().getEpochSecond());
            try {
                database.getCollection("clicks").insertOne(clickEvent);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to write click event to MongoDB", ex);
            }
        });
    }

}
