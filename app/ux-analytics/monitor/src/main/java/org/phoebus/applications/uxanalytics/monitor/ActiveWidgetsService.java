package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.ArrayList;

public class ActiveWidgetsService {

    private final ArrayList<Widget> widgets;
    private final DockItemWithInput parentTab;
    private final ToolkitListener listener;

    public ActiveWidgetsService(DockItemWithInput tab){
        widgets = new ArrayList<>();
        parentTab = tab;
        listener = new GraphDatabaseToolkitListener();
        ((DisplayRuntimeInstance)tab.getProperties().get("application")).addListener(listener);
    }

    public ToolkitListener getListener(){
        return listener;
    }

    public void add(Widget widget){
        widgets.add(widget);
    }

    public void remove(Widget widget){
        widgets.remove(widget);
    }

}
