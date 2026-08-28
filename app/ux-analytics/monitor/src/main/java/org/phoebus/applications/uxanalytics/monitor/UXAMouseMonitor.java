package org.phoebus.applications.uxanalytics.monitor;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;

public class UXAMouseMonitor implements EventHandler<MouseEvent>{

    private final UXAMonitor monitor = UXAMonitor.getInstance();
    private final ActiveTab tab;

    public UXAMouseMonitor(ActiveTab tab){
        this.tab = tab;
    }

    @Override
    public void handle(MouseEvent event) {
        if(event.getEventType().equals(MouseEvent.MOUSE_CLICKED)){
            monitor.getJfxConnection().handleClick(tab, (int) event.getX(), (int) event.getY());
        }
    }
}
