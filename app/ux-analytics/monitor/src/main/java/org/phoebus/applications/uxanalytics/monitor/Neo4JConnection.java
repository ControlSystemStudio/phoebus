package org.phoebus.applications.uxanalytics.monitor;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.neo4j.driver.*;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Neo4JConnection implements BackendConnection{

    Logger logger = Logger.getLogger(Neo4JConnection.class.getName());
    Driver driver;
    //string names for "origin" sources (i.e., not another display)
    public static final String SRC_FILE_BROWSER = "file_browser";
    public static final String SRC_TOP_RESOURCES = "top_resources_list";
    public static final String SRC_RESTORATION = "memento_restored";
    public static final String SRC_UNKNOWN = "other_source";

    //names for node types (an 'origin' source, another display, or a PV)
    public static final String TYPE_ORIGIN = "origin_source";
    public static final String TYPE_DISPLAY = "display";
    public static final String TYPE_PV = "pv";

    //names for action types (Opened a display, wrote to a PV)
    public static final String ACTION_WROTE = "wrote_to";
    public static final String ACTION_OPENED = "opened";

    public static final String PROTOCOL = "neo4j://";

    private Session session;

    @Override
    public Boolean connect(String host, Integer port, String username, String password) {
        try {
            driver = GraphDatabase.driver(PROTOCOL + host + ":" + port.toString(), AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            logger.log(Level.INFO, "Connected to " + host + " on port " + port + " as " + username);
            session = driver.session(
                    SessionConfig.builder()
                            .withDatabase("phoebus-analytics")
                            .build());
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

    @Override
    public void handleClick(ActiveTab who, Widget widget, Integer x, Integer y) {
        //In Neo4J, we don't have any actions to perform on a click
        return;
    }

    //this operation involves calculating a hash and posting an update to the Neo4J database
    //so it should not be done on the JavaFX thread
    public void handleDisplayOpenViaActionButton(ActiveTab who, Widget widget, ActionInfo actionInfo) {
        Platform.runLater(() -> {
            DisplayInfo currentDisplayInfo = who.getDisplayInfo();
            String sourcePath = ModelResourceUtil.normalize(currentDisplayInfo.getPath());
            String targetPath = ModelResourceUtil.normalize(
                    ModelResourceUtil.resolveResource(sourcePath,
                            ((OpenDisplayActionInfo) actionInfo).getFile()));
            try {
                logger.info("Neo4J would have handled file open action: " + FileUtils.getAnalyticsPathFor(sourcePath) + "->" + FileUtils.getAnalyticsPathFor(targetPath));
            }
            catch(Exception e){
                logger.warning("Problem opening " + targetPath + " from " + sourcePath + ", not logging in analytics.");
            }
        });
    }

    @Override
    public void handleAction(ActiveTab who, Widget widget, ActionInfo info) {
        ActionInfo.ActionType actionType = info.getType();
        switch (actionType) {
            case WRITE_PV:
                handlePVWrite(who, widget, ((WritePVActionInfo) info).getPV(), ((WritePVActionInfo) info).getValue());
                break;
            case OPEN_DISPLAY:
                handleDisplayOpenViaActionButton(who, widget, info);
                break;
            default:
                //keep it simple for now, just PVs and file opens
                break;
        }
    }

    @Override
    public void handlePVWrite(ActiveTab who, Widget widget, String PVName, Object value) {
        logger.log(Level.INFO, "Neo4J Connection would have handled PV Write of " + value + " to " + PVName+  " from " + who + "on" + widget);
    }

    private void createNodeIfNotExists(String name, String type){
        Platform.runLater(()->session.executeWriteWithoutResult(tx->tx.run("MERGE ($name:$type {name: $name})", Map.of("name",name,"type",type))));
    }

    private void fileOpenConnection(String srcName, String srcType, String dstName){
        Platform.runLater(()->{
            session.executeWriteWithoutResult(tx -> tx.run(
                        "MERGE(src:$srcType {name:'$srcName'}"+
                                "MERGE(dst:$dstType {name:'$dstName'}"+
                                "MERGE(src)-[connection:$connectionType]->(dst)"+
                                "ON CREATE SET connection.timestamps = [$timestamp]"+
                                "ON MATCH SET connection.timestamps=connection.timestamps+$timestamp",
                                Map.of("srcName","srcType",
                                        "dstName",TYPE_DISPLAY,
                                        "connectionType",ACTION_OPENED,
                                        "timestamp", Instant.now().getEpochSecond())
                        ));
        });
    }

    @Override
    public void handleDisplayOpen(DisplayInfo target, DisplayInfo src, ResourceOpenSources how) {

        switch(how) {
            case RELOAD:
            case NAVIGATION_BUTTON:
                //create a connection with source and destination
            case FILE_BROWSER:
                //create a connection from SRC_FILE_BROWSER to destination
            case TOP_RESOURCES:
                //create a connection from SRC_TOP_RESOURCES to destination
            case RESTORED:
                //create a connection from SRC_RESTORATION to destination
            case UNKNOWN:
                //create a connection from SRC_UNKNOWN to destination
        }
    }
}