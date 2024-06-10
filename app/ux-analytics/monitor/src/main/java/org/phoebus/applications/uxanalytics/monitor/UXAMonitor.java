package org.phoebus.applications.uxanalytics.monitor;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.application.Application;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.spi.WidgetRuntimesService;

/**
 * Singleton Class to capture UI events (clicks, setting changes, Display open/close, Driver address update)
 * This maintains all the parts of the UXAMonitor.
 */
public class UXAMonitor{
    private static UXAMonitor instance = null;
    private ArrayList<Stage> activeStages;
    private static ActiveWindowsService activeWindowsService= ActiveWindowsService.getInstance();
    private static final ExecutorService executor = RuntimeUtil.getExecutor();
    private BackendConnection phoebusConnection;
    private BackendConnection jfxConnection;

    private UXAMonitor() {
    }

    public void setPhoebusConnection(BackendConnection phoebusConnection) {
        this.phoebusConnection = phoebusConnection;
    }

    public void setJfxConnection(BackendConnection jfxConnection) {
        this.jfxConnection = jfxConnection;
    }

    public static UXAMonitor getInstance() {
        if (instance == null) {
            instance = new UXAMonitor();
        }
        return instance;
    }

    public void notifyConnectionChange(BackendConnection connection){
        if(connection instanceof MongoDBConnection){
            jfxConnection = connection;
        }else if(connection instanceof Neo4JConnection){
            phoebusConnection = connection;
        }
    }

}
