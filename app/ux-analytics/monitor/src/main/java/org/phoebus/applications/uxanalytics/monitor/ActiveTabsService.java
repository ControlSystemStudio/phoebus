package org.phoebus.applications.uxanalytics.monitor;

import javafx.collections.*;
import javafx.stage.Window;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.csstudio.display.builder.runtime.app.DockItemRepresentation;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ActiveTabsService {

    private final ActiveWindowsService activeWindowsService;
    private final Window parentWindow;
    private final ConcurrentHashMap<String, ActiveWidgetsService> activeTabs = new ConcurrentHashMap<>();

    public ActiveTabsService(Window window){
        this.parentWindow = window;
        activeWindowsService = ActiveWindowsService.getInstance();
    }

    public synchronized void add(DockItemWithInput tab) throws Exception {
        this.remove(tab);
        activeTabs.putIfAbsent(tab.toString(), new ActiveWidgetsService(tab));
        addAllWidgetsIn(tab);
    }

    public synchronized void remove(DockItemWithInput tab){
        if(activeTabs.containsKey(tab.toString())){
            activeTabs.get(tab.toString()).close();
            activeTabs.remove(tab.toString());
        }
    }

    public boolean contains(DockItemWithInput tab){
        return activeTabs.containsKey(tab.toString());
    }

    public synchronized void addWidget(DockItemWithInput tab, Widget widget){
        activeTabs.get(tab.toString()).add(widget);
    }

    public synchronized void addAllWidgetsIn(DockItemWithInput tab) throws Exception {
        DisplayRuntimeInstance instance = (DisplayRuntimeInstance) tab.getProperties().get("application");
        for(Widget widget: instance.getActiveModel().getChildren()){
            if(widget instanceof EmbeddedDisplayWidget){
                addAllWidgetsIn((EmbeddedDisplayWidget) widget, tab);
            }
            else{
                addWidget(tab, widget);
            }
        }
    }

    public synchronized void addAllWidgetsIn(EmbeddedDisplayWidget widget, DockItemWithInput parentTab) throws Exception {
        DisplayModel model = (DisplayModel) widget.getProperty("embedded_model").getValue();
        for(Widget embeddedWidget: model.getChildren()){
            if(embeddedWidget instanceof EmbeddedDisplayWidget){
                //recursively add widgets (We don't care that it's embedded, only that it's seen in this tab)
                addAllWidgetsIn((EmbeddedDisplayWidget) embeddedWidget, parentTab);
            }
            else{
                addWidget(parentTab, embeddedWidget);
            }
        }

    }

    public ConcurrentHashMap<String, ActiveWidgetsService> getActiveTabs() {
        return activeTabs;
    }
}
