package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.stage.Window;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

import java.util.concurrent.ConcurrentHashMap;

public class ActiveTabsOfWindow {

    private final ActiveWindowsService activeWindowsService;
    private final Window parentWindow;
    private final ConcurrentHashMap<String, ActiveTab> activeTabs = new ConcurrentHashMap<>();

    public static String tabIDOf(DockItemWithInput tab){
        return (String)tab.getProperties().get(DockStage.KEY_ID);
    }

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
        activeTabs.putIfAbsent(tabIDOf(tab), new ActiveTab(tab, parentWindow.getProperties().get(DockStage.KEY_ID).toString()));
    }

    public void remove(DockItemWithInput tab){
        if(activeTabs.containsKey(tabIDOf(tab))){
            activeTabs.get(tabIDOf(tab)).detachListeners();
            activeTabs.remove(tabIDOf(tab));
        }
    }

    public void remove(ActiveTab tab){
        if(activeTabs.containsKey(tab.toString())){
            activeTabs.get(tab.toString()).detachListeners();
            activeTabs.remove(tab.toString());
        }
    }

    public boolean contains(DockItemWithInput tab){
        return activeTabs.containsKey(tabIDOf(tab));
    }

    public synchronized void addWidget(DockItemWithInput tab, Widget widget){
        activeTabs.get(tabIDOf(tab)).add(widget);
    }

    public ConcurrentHashMap<String, ActiveTab> getActiveTabs() {
        return activeTabs;
    }
}
