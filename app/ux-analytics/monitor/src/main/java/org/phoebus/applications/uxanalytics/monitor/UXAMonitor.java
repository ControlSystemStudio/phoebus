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
 * This single monitor dispatches events to all registered observers
 */
public class UXAMonitor{
    private static UXAMonitor instance = null;
    private ArrayList<Stage> activeStages;
    private static ActiveWindowsService activeWindowsService;
    private static final ExecutorService executor = RuntimeUtil.getExecutor();

    private UXAMonitor() {
        activeWindowsService = ActiveWindowsService.getInstance();
    }

    public static UXAMonitor getInstance() {
        if (instance == null) {
            instance = new UXAMonitor();
        }
        return instance;
    }


}
