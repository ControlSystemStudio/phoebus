package org.phoebus.applications.uxanalytics.monitor;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

public class UXAMouseMonitor implements EventHandler<MouseEvent>{

    private DockItemWithInput tab;
    private BackendConnection backendConnection;

    public UXAMouseMonitor(DockItemWithInput tab){
        tab.getContent().addEventFilter(MouseEvent.MOUSE_CLICKED, this);
    }

    @Override
    public void handle(MouseEvent event) {
        if(event.getEventType().equals(MouseEvent.MOUSE_CLICKED)){
            backendConnection.handleClick(tab, (int)event.getSceneX(), (int)event.getSceneY());
            event.consume();
        }
    }
}
