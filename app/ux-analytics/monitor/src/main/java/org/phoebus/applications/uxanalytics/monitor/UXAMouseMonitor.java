package org.phoebus.applications.uxanalytics.monitor;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

public class UXAMouseMonitor implements EventHandler<MouseEvent>{

    private ActiveTab tab;
    private final UXAMonitor monitor = UXAMonitor.getInstance();

    public UXAMouseMonitor(ActiveTab tab){
        tab.getParentTab().getContent().addEventFilter(MouseEvent.MOUSE_CLICKED, this);
        this.tab = tab;
    }

    @Override
    public void handle(MouseEvent event) {
        if(event.getEventType().equals(MouseEvent.MOUSE_CLICKED)){
            monitor.getJfxConnection().handleClick(tab, (int) event.getX(), (int) event.getY());
        }
    }
}
