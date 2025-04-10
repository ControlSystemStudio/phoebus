package org.phoebus.applications.uxanalytics.monitor.backend.database;

import java.time.Instant;
import java.util.HashMap;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import javafx.application.Platform;
import org.csstudio.display.actions.OpenDisplayAction;
import org.csstudio.display.actions.WritePVAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.phoebus.applications.uxanalytics.monitor.backend.image.ImageClient;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.applications.uxanalytics.monitor.util.ResourceOpenSources;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.security.tokens.AuthenticationScope;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.util.Map;

import static org.csstudio.display.actions.OpenDisplayAction.OPEN_DISPLAY;
import static org.csstudio.display.actions.WritePVAction.WRITE_PV;

public class ServiceLayerConnection implements BackendConnection{

    //string names for "origin" sources (i.e., not another display)
    public static final String SRC_FILE_BROWSER = "file_browser";
    public static final String SRC_TOP_RESOURCES = "top_resources_list";
    public static final String SRC_RESTORATION = "memento_restored";
    public static final String SRC_UNKNOWN = "other_source";

    public static final String TYPE_ORIGIN = "origin_source";
    public static final String TYPE_DISPLAY = "display";
    public static final String TYPE_PV = "pv";

    public static final String ACTION_WROTE = "wrote_to";
    public static final String ACTION_OPENED = "opened";
    public static final String ACTION_NAVIGATED = "navigation_button";
    public static final String ACTION_RELOADED = "reloaded";

    //track if last attempt failed, to prevent spamming the log
    private boolean exceptionRaised = false;

    Logger logger = Logger.getLogger(ServiceLayerConnection.class.getName());

    Client client = new Client();
    private String endpoint;

    public static ServiceLayerConnection instance;
    public static ServiceLayerConnection getInstance(){
        if(instance == null){
            instance = new ServiceLayerConnection();
        }
        return instance;
    }

    private ServiceLayerConnection(){
        tryAutoConnect(null);
    }

    private boolean checkConnection(){
        try{
            String response = client.resource(endpoint+"/checkConnection").get(String.class);
            JsonNode node = new ObjectMapper().readTree(response);
            boolean appStatus =  node.get("applicationStatus").asText().equals("OK");
            boolean dbStatus = node.get("mariaDatabaseStatus").asText().equals("OK");
            boolean graphStatus = node.get("graphDatabaseStatus").asText().equals("OK");
            if (!(dbStatus && graphStatus)) {
                logger.warning("UX Analytics service layer connection failed. Application status: " + appStatus + ", MariaDB status: " + dbStatus + ", GraphDB status: " + graphStatus);
            }
            return appStatus && dbStatus && graphStatus;
        }
        catch(JsonProcessingException e){
            logger.warning("Exception connecting to UX Analytics service layer: " + e.getMessage());
            exceptionRaised = true;
            return false;
        }
    }

    @Override
    public Boolean connect(String hostOrRegion, Integer port, String usernameOrAccessKey, String passwordOrSecretKey) {
        //user/password not used
        if(hostOrRegion==null)
            hostOrRegion = getHost();
        if (port==null)
            port = Integer.parseInt(getPort());
        endpoint = getProtocol() + hostOrRegion + ":" + port + "/analytics";
        return checkConnection();
    }

    @Override
    public String getProtocol() {
        return "http://";
    }

    @Override
    public String getHost(){
        return PhoebusPreferenceService.userNodeForClass(this.getClass()).get("host", "localhost");
    }

    @Override
    public String getPort() {
        return PhoebusPreferenceService.userNodeForClass(this.getClass()).get("port", "8080");
    }

    @Override
    public boolean tryAutoConnect(AuthenticationScope scope) {
        return BackendConnection.super.tryAutoConnect(scope);
    }

    @Override
    public String getDefaultUsername() {
        return ""; //not used
    }

    @Override
    public Integer tearDown() {
        client.destroy();
        return 0;
    }

    @Override
    public void setImageClient(ImageClient imageClient) {
        return; //don't do anything with images yet
    }

    @Override
    public void handleClick(ActiveTab who, Integer x, Integer y) {
        //todo: check if image exists, store that response in a map so we don't have to check again
        //if image exists, send in a separate API call

        //for now just record a click with a POST request
        HashMap<String, String> click = new HashMap<String,String>();
        click.put("x", x.toString());
        click.put("y", y.toString());
        click.put("filename", FileUtils.analyticsPathForTab(who));
        click.put("timestamp", Instant.now().toString());
        ObjectMapper mapper = new JsonMapper();
        try {
            String json = mapper.writeValueAsString(click);
            ClientResponse response = client.resource(endpoint + "/recordClick")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, json);
            exceptionRaised = false;
        }
        catch(Exception e){
            if (!exceptionRaised) {
                logger.warning("Exception connecting to UX Analytics service layer: " + e.getMessage());
                exceptionRaised = true;
            }
        }
    }

    @Override
    public void handleAction(ActiveTab who, Widget widget, ActionInfo info) {
        String actionType = info.getType();
        switch (actionType){
            case WRITE_PV:
                handlePVWrite(who, widget, ((WritePVAction) info).getPV(), ((WritePVAction) info).getValue());
                break;
            case OPEN_DISPLAY:
                handleDisplayOpenViaActionButton(who, widget, (OpenDisplayAction)info );
                break;
            default:
                break;
        }
    }

    private void recordConnection(String srcType, String dstType, String srcName, String dstName, String action){
        try {
            Map<String, String> connection = Map.of("srcName", srcName,
                    "srcType", srcType,
                    "dstName", dstName,
                    "dstType", dstType,
                    "action", action);
            ClientResponse response = client.resource(endpoint + "/recordNavigation")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, connection);
        }
        catch(Exception e){
            if (!exceptionRaised) {
                logger.warning("Exception connecting to UX Analytics service layer: " + e.getMessage());
                exceptionRaised = true;
            }
        }
    }

    private void recordConnection(String srcType, String dstType, String srcName, String dstName, String action, String via){
        if(via==null) {
            recordConnection(srcType, dstType, srcName, dstName, action);
            return;
        }
        try {
            Map<String, Object> connection = Map.of("srcName", srcName,
                    "srcType", srcType,
                    "dstName", dstName,
                    "dstType", dstType,
                    "action", action,
                    "via", via);
            ClientResponse response = client.resource(endpoint + "/recordNavigation")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, connection);
        }
        catch(Exception e){
            if (!exceptionRaised) {
                logger.warning("Exception connecting to UX Analytics service layer: " + e.getMessage());
                exceptionRaised = true;
            }
        }
    }

    @Override
    public void handlePVWrite(ActiveTab who, Widget widget, String PVName, String value) {
        Platform.runLater(()->{
            String display = FileUtils.analyticsPathForTab(who);
            String widgetID = widget.getName();
            recordConnection(TYPE_DISPLAY, TYPE_PV, display, PVName, ACTION_WROTE, widgetID);
        });
    }

    public void handleDisplayOpenViaActionButton(ActiveTab who, Widget widget, OpenDisplayAction openDisplayAction) {
        Platform.runLater(() -> {
            DisplayInfo currentDisplayInfo = who.getDisplayInfo();
            String widgetID = widget.getName();
            String sourcePath = FileUtils.getAnalyticsPathFor(currentDisplayInfo.getPath());
            String targetPath = FileUtils.getAnalyticsPathFor(
                    ModelResourceUtil.resolveResource(
                            currentDisplayInfo.getPath(),
                            openDisplayAction.getFile())
            );
            recordConnection(TYPE_DISPLAY,TYPE_DISPLAY,sourcePath,targetPath,ACTION_OPENED,widgetID);
        });
    }

    @Override
    public void handleDisplayOpen(DisplayInfo target, DisplayInfo src, ResourceOpenSources how) {
        Platform.runLater(()->{
                String sourcePath = "UNKNOWN";
            String targetPath = FileUtils.getAnalyticsPathFor(target.getPath());
            String sourceType = null;
            String action = ACTION_OPENED;
            switch (how) {
                case RELOAD:
                case NAVIGATION_BUTTON:
                    if (src != null) {
                        sourcePath = FileUtils.getAnalyticsPathFor(src.getPath());
                        sourceType = TYPE_DISPLAY;
                        assert sourcePath != null;
                        if(sourcePath.equals(targetPath)){
                            action=ACTION_RELOADED;
                        }
                        else{
                            action = ACTION_NAVIGATED;
                        }
                    }
                    break;
                case FILE_BROWSER:
                    sourcePath = SRC_FILE_BROWSER;
                    sourceType = TYPE_ORIGIN;
                    break;
                case TOP_RESOURCES:
                    sourcePath = SRC_TOP_RESOURCES;
                    sourceType = TYPE_ORIGIN;
                    break;
                case RESTORED:
                    sourcePath = SRC_RESTORATION;
                    sourceType = TYPE_ORIGIN;
                    break;
                default:
                    sourcePath = SRC_UNKNOWN;
                    sourceType = TYPE_ORIGIN;
            }
            recordConnection(sourceType, TYPE_DISPLAY, sourcePath, targetPath, action, null);
        });
    }
}
