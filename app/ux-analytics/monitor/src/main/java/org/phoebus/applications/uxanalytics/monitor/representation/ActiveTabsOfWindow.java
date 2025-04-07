package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.stage.Window;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.docking.DockItemWithInput;

import java.util.concurrent.ConcurrentHashMap;

public class ActiveTabsOfWindow {

    private final ActiveWindowsService activeWindowsService;
    private final Window parentWindow;
    private final ConcurrentHashMap<String, ActiveTab> activeTabs = new ConcurrentHashMap<>();

    public ActiveTabsOfWindow(Window window){
        this.parentWindow = window;
        activeWindowsService = ActiveWindowsService.getInstance();
    }

    public void add(ActiveTab tab) throws Exception {
        this.remove(tab);
        activeTabs.putIfAbsent(tab.toString(), tab);
    }

    public void add(DockItemWithInput tab) throws Exception {
        this.remove(tab);
        activeTabs.putIfAbsent(tab.toString(), new ActiveTab(tab));
    }

    public void remove(DockItemWithInput tab){
        if(activeTabs.containsKey(tab.toString())){
            activeTabs.get(tab.toString()).detachListeners();
            activeTabs.remove(tab.toString());
        }
    }

    public void remove(ActiveTab tab){
        if(activeTabs.containsKey(tab.toString())){
            activeTabs.get(tab.toString()).detachListeners();
            activeTabs.remove(tab.toString());
        }
    }

    public boolean contains(DockItemWithInput tab){
        return activeTabs.containsKey(tab.toString());
    }

    public synchronized void addWidget(DockItemWithInput tab, Widget widget){
        activeTabs.get(tab.toString()).add(widget);
    }

    public ConcurrentHashMap<String, ActiveTab> getActiveTabs() {
        return activeTabs;
    }
}
