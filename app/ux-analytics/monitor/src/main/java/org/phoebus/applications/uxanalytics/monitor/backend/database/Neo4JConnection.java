package org.phoebus.applications.uxanalytics.monitor.backend.database;

import javafx.application.Platform;
import org.csstudio.display.actions.OpenDisplayAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfoBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.actions.OpenDisplayAction;
import org.csstudio.display.actions.WritePVAction;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.neo4j.driver.*;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.applications.uxanalytics.monitor.util.ResourceOpenSources;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.security.tokens.AuthenticationScope;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.csstudio.display.actions.OpenDisplayAction.OPEN_DISPLAY;
import static org.csstudio.display.actions.WritePVAction.WRITE_PV;

public class Neo4JConnection implements BackendConnection {

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
    public static final String ACTION_NAVIGATED = "navigation_button";

    public static final String PROTOCOL = "neo4j://";

    public static Neo4JConnection instance;
    public static Neo4JConnection getInstance(){
        if(instance == null){
            instance = new Neo4JConnection();
        }
        return instance;
    }

    private Session session;

    private Neo4JConnection(){
        tryAutoConnect(AuthenticationScope.NEO4J);
    }

    @Override
    public Boolean connect(String host, Integer port, String username, String password) {
        try {
            if(host == null)
                host = getHost();
            if(port == null)
                port = Integer.parseInt(getPort());
            driver = GraphDatabase.driver(PROTOCOL + host + ":" + port.toString(), AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            logger.log(Level.INFO, "Connected to " + host + " on port " + port + " as " + username);
            session = driver.session(
                    SessionConfig.builder()
                            .withDatabase(
                                    PhoebusPreferenceService.userNodeForClass(FileUtils.class)
                                            .get("database", "neo4j"))
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
    public String getHost(){
        return PhoebusPreferenceService.userNodeForClass(this.getClass()).get("host", "localhost");
    }

    @Override
    public String getPort() {
        return PhoebusPreferenceService.userNodeForClass(this.getClass()).get("port", "7687");
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

    @Override
    public void handlePVWrite(ActiveTab who, Widget widget, String PVName, String value) {
        String display = FileUtils.analyticsPathForTab(who);
        String widgetID = widget.getName();
        if(session!=null && session.isOpen()){
            Platform.runLater(()->{
                String query = String.format("MERGE(src:%s {name:$srcName}) " +
                                "MERGE(dst:%s {name:$dstName}) " +
                                "MERGE(src)-[connection:%s]->(dst) " +
                                "ON CREATE SET connection.timestamps = [$timestamp]," +
                                "connection.via = [$widgetId]"+
                                "ON MATCH SET connection.timestamps=connection.timestamps+$timestamp,"+
                                "connection.via = connection.via+[$widgetId]",
                        TYPE_DISPLAY,TYPE_PV,ACTION_WROTE);
                session.executeWriteWithoutResult(tx->tx.run(query, Map.of("srcName",display,"dstName",PVName,
                        "widgetId", widgetID, "timestamp", Instant.now().getEpochSecond())));
            });
        }
    }

    @Override
    public void handleDisplayOpen(DisplayInfo target, DisplayInfo src, ResourceOpenSources how) {
        String sourcePath="UNKNOWN";
        String targetPath = FileUtils.getAnalyticsPathFor(target.getPath());
        String sourceType = null;
        String action = ACTION_OPENED;
        switch(how) {
            case RELOAD:
            case NAVIGATION_BUTTON:
                if(src!=null) {
                    sourcePath = FileUtils.getAnalyticsPathFor(src.getPath());
                    sourceType = TYPE_DISPLAY;
                    action = ACTION_NAVIGATED;
                }
                break;
            case FILE_BROWSER:
                sourcePath=SRC_FILE_BROWSER;
                sourceType=TYPE_ORIGIN;
                break;
            case TOP_RESOURCES:
                sourcePath=SRC_TOP_RESOURCES;
                sourceType=TYPE_ORIGIN;
                break;
            case RESTORED:
                sourcePath=SRC_RESTORATION;
                sourceType=TYPE_ORIGIN;
                break;
            default:
                sourcePath=SRC_UNKNOWN;
                sourceType=TYPE_ORIGIN;
        }
        fileOpenConnection(targetPath,sourcePath,sourceType, action);
    }

    //this operation involves calculating a hash and posting an update to the Neo4J database
    //so it should not be done on the JavaFX thread
    //This is separate from the rest because it requires additional validation to ensure the target is real.
    public void handleDisplayOpenViaActionButton(ActiveTab who, Widget widget, OpenDisplayAction openDisplayAction) {
        Platform.runLater(() -> {
            DisplayInfo currentDisplayInfo = who.getDisplayInfo();
            String sourcePath = FileUtils.getAnalyticsPathFor(currentDisplayInfo.getPath());
            String targetPath = FileUtils.getAnalyticsPathFor(
                    ModelResourceUtil.resolveResource(
                            currentDisplayInfo.getPath(),
                            openDisplayAction.getFile())
            );
            try {
                fileOpenConnection(targetPath,sourcePath,TYPE_DISPLAY,ACTION_OPENED);
            }
            catch(Exception e){
                logger.warning("Problem opening " + targetPath + " from " + sourcePath + ", not logging in analytics.");
            }
        });
    }

    @Override
    public void handleAction(ActiveTab who, Widget widget, ActionInfo info) {
        String actionType = info.getType();
        switch (actionType) {
            case WRITE_PV:
                handlePVWrite(who, widget, ((WritePVAction) info).getPV(), ((WritePVAction) info).getValue());
                break;
            case OPEN_DISPLAY:
                handleDisplayOpenViaActionButton(who, widget, (OpenDisplayAction)info );
                break;
            default:
                //keep it simple for now, just PVs and file opens
                break;
        }
    }

    private void fileOpenConnection( String dstName, String srcName, String srcType, String action){
        Platform.runLater(()->{
            String query = String.format("MERGE(src:%s {name:$srcName}) " +
                            "MERGE(dst:%s {name:$dstName}) " +
                            "MERGE(src)-[connection:%s]->(dst) " +
                            "ON CREATE SET connection.timestamps = [$timestamp]" +
                            "ON MATCH SET connection.timestamps=connection.timestamps+$timestamp",
                    srcType, TYPE_DISPLAY, action);
            if(dstName != null && srcName != null && srcType != null && session!=null && session.isOpen()) {
                session.executeWriteWithoutResult(tx -> tx.run(query,
                        Map.of("srcName", srcName,
                                "dstName", dstName,
                                "timestamp", Instant.now().getEpochSecond())
                ));
            }
        });
    }

}