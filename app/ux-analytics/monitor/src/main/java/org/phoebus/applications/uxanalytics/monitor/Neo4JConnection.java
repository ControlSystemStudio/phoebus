package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenFileActionInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.neo4j.driver.*;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Neo4JConnection implements BackendConnection{

    Logger logger = Logger.getLogger(Neo4JConnection.class.getName());
    Driver driver;

    public static final String PROTOCOL = "neo4j://";

    @Override
    public Boolean connect(String host, Integer port, String username, String password) {
        try {
            driver = GraphDatabase.driver(PROTOCOL + host + ":" + port.toString(), AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            logger.log(Level.INFO, "Connected to " + host + " on port " + port + " as " + username);
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

    public void handleFileOpen(ActiveTab who, Widget widget, String destinationFileName) {
        logger.log(Level.INFO, "Neo4J Connection would've handled File Open" + destinationFileName + "from" + who + "on" + widget);
    }

    @Override
    public void handleAction(ActiveTab who, Widget widget, ActionInfo info) {
        ActionInfo.ActionType actionType = info.getType();
        switch (actionType) {
            case WRITE_PV:
                handlePVWrite(who, widget, ((WritePVActionInfo) info).getPV(), ((WritePVActionInfo) info).getValue());
                break;
            case OPEN_FILE:
                handleFileOpen(who, widget, ((OpenFileActionInfo) info).getFile());
                break;
            default:
                //keep it simple for now, just PVs and file opens
                break;
        }
    }

    public static String createNodeForFileOpen(TransactionContext tx, ActiveTab tab){
        return "";
    }

    @Override
    public void handlePVWrite(ActiveTab who, Widget widget, String PVName, Object value) {
        logger.log(Level.INFO, "Neo4J Connection would've handled PV Write of " + value + " to " + PVName+  " from " + who + "on" + widget);
    }

}
