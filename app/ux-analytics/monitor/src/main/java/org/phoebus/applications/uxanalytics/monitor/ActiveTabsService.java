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

public class ActiveTabsService {

    private final ActiveWindowsService activeWindowsService;
    private final Window window;
    private final ObservableMap<DockItemWithInput, ActiveWidgetsService> activeTabs = FXCollections.observableHashMap();

    public ActiveTabsService(Window window){
        this.window = window;
        activeWindowsService = ActiveWindowsService.getInstance();
    }

    public void add(DockItemWithInput tab) throws Exception {
        if (!activeTabs.containsKey(tab)){
            activeTabs.put(tab, new ActiveWidgetsService(tab));
            addAllWidgetsIn(tab);
        }
    }

    public void remove(DockItemWithInput tab){
        activeTabs.remove(tab);
    }

    public boolean contains(DockItemWithInput tab){
        return activeTabs.containsKey(tab);
    }

    public void addWidget(DockItemWithInput tab, Widget widget){
        if(activeTabs.containsKey(tab)){
            activeTabs.get(tab).add(widget);
        }
    }

    public void addAllWidgetsIn(DockItemWithInput tab) throws Exception {
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

    public void addAllWidgetsIn(EmbeddedDisplayWidget widget, DockItemWithInput parentTab) throws Exception {
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
}
