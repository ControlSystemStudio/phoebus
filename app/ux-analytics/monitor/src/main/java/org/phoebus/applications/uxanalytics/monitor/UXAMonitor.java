package org.phoebus.applications.uxanalytics.monitor;


import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import javafx.stage.Stage;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.phoebus.applications.uxanalytics.monitor.backend.database.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.backend.database.MongoDBConnection;
import org.phoebus.applications.uxanalytics.monitor.backend.database.Neo4JConnection;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveWindowsService;

/**
 * Singleton Class to capture UI events (clicks, PV Writes, Display open/close)
 * and dispatch them to backend connections
 */
public class UXAMonitor{
    private static final UXAMonitor instance = new UXAMonitor();
    private ArrayList<Stage> activeStages;
    private static ActiveWindowsService activeWindowsService = ActiveWindowsService.getInstance();
    private static final ExecutorService executor = RuntimeUtil.getExecutor();

    //This dispatcher has exactly one phoebus related connection and one JFX related connection
    //If you want to broadcast to multiple back-ends, subclass BackendConnection to notify them.
    private BackendConnection phoebusConnection;
    private BackendConnection jfxConnection;

    private UXAMonitor() {
    }

    public BackendConnection getJfxConnection() {return jfxConnection;}

    public BackendConnection getPhoebusConnection() { return phoebusConnection;}

    public static synchronized UXAMonitor getInstance() {
        return instance;
    }

    public void notifyConnectionChange(BackendConnection connection){
        if(connection instanceof MongoDBConnection){
            jfxConnection = connection;
        } else if(connection instanceof Neo4JConnection){
            phoebusConnection = connection;
        }
    }

    public void setPhoebusConnection(BackendConnection phoebusConnection) {
        this.phoebusConnection = phoebusConnection;
    }

    public void setJfxConnection(BackendConnection jfxConnection) {
        this.jfxConnection = jfxConnection;
    }
}
