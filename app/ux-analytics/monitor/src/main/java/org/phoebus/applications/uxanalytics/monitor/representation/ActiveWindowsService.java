package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.collections.*;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/*
    * This class is a singleton that keeps track of all active windows and their tabs.
    * The state remains tracked, but if 'stop()' is called, the listeners are detached from the actual representation.
    * This way, if the user consents to tracking, the listeners are reattached and the state is updated without restarting.
 */
public class ActiveWindowsService {

    private boolean active = false;
    private static ActiveWindowsService instance = null;
    private final ConcurrentHashMap<String, ActiveTabsOfWindow> activeWindowsAndTabs = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    ConcurrentHashMap<String, ActiveTabsOfWindow> getActiveWindowsAndTabs() {
        return activeWindowsAndTabs;
    }

    ListChangeListener<Tab> UXATabChangeListener = new ListChangeListener<>() {
        @Override
        public void onChanged(Change<? extends Tab> change) {
            while(change.next()){
                if(change.wasAdded()){
                    for(Tab tab: change.getAddedSubList()){
                        Window window = change.getList().get(0).getTabPane().getScene().getWindow();
                        if(tab.getProperties().get("application") instanceof DisplayRuntimeInstance && tab instanceof DockItemWithInput){

                            try {
                                //Creating the wrapper object first (in the application thread) attaches a listener ASAP
                                //we want to catch what caused the display to open
                                ActiveTab tabWrapper = new ActiveTab((DockItemWithInput) tab);
                                DisplayRuntimeInstance instance = (DisplayRuntimeInstance) tabWrapper.getParentTab().getProperties().get("application");
                                //When @DockItem s are initialized, their models aren't ready yet.
                                //On startup, the DockItemWithInput will show up but its DisplayModel will be null.
                                //block until the model is ready, in an individual background thread.
                                new Thread(() -> {
                                    try {
                                        //block until the model is ready
                                        instance.getRepresentation_init().get();
                                        lock.lock();
                                        String windowID = (String) window.getProperties().get(DockStage.KEY_ID);
                                        activeWindowsAndTabs.get(windowID).add(tabWrapper);
                                        lock.unlock();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                else if(change.wasRemoved()){
                    for(Tab tab: change.getRemoved()){
                        if(tab.getProperties().get("application") instanceof DisplayRuntimeInstance && tab instanceof DockItemWithInput){
                            lock.lock();
                            String windowID = (String) tab.getTabPane().getScene().getWindow().getProperties().get(DockStage.KEY_ID);
                            activeWindowsAndTabs.get(windowID).remove((DockItemWithInput) tab);
                            lock.unlock();
                        }
                    }
                }
            }
        }
    };


    ListChangeListener<Window> UXAWindowChangeListener = new ListChangeListener<>() {
        @Override
        public void onChanged(Change<? extends Window> change) {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (javafx.stage.Window window : change.getAddedSubList()) {
                        if(window.getProperties().containsKey(DockStage.KEY_ID)){
                            String windowID = (String) window.getProperties().get(DockStage.KEY_ID);
                            lock.lock();
                            activeWindowsAndTabs.putIfAbsent(windowID, new ActiveTabsOfWindow(window));
                            for(DockPane item: DockStage.getDockPanes((Stage)window)){
                                item.getTabs().removeListener(UXATabChangeListener);
                                item.getTabs().addListener(UXATabChangeListener);
                            }
                            lock.unlock();
                        }
                    }
                }
                else if(change.wasRemoved()){
                    for(Window window: change.getRemoved()){
                        if(window.getProperties().containsKey(DockStage.KEY_ID)){
                            String windowID = (String) window.getProperties().get(DockStage.KEY_ID);
                            lock.lock();
                            activeWindowsAndTabs.remove(windowID);
                            lock.unlock();
                        }
                    }
                }
            }
        }
    };

    private ActiveWindowsService() {
    }

    //this singleton will be the exclusive communicator with the window list
    public static ActiveWindowsService getInstance() {
        if(instance == null){
            instance = new ActiveWindowsService();
            instance.addWindowChangeListener();
        }
        return instance;
    }

    //If user has not consented to tracking, we still keep track of the state, but don't ping listeners about it.
    //Allows tracking to start/stop mid-session without restarting the application.
    public boolean isActive() {
        return active;
    }

    public void addWindowChangeListener(){
        javafx.stage.Window.getWindows().addListener(UXAWindowChangeListener);
    }

    public void start() {
        if(!active) {
            for(ActiveTabsOfWindow window: activeWindowsAndTabs.values()) {
                for (ActiveTab tab : window.getActiveTabs().values()) {
                    tab.addListeners();
                }
            }
        }
        active = true;
    }

    public void stop() {
        if(active){
            for(ActiveTabsOfWindow window: activeWindowsAndTabs.values()) {
                for (ActiveTab tab : window.getActiveTabs().values()) {
                    tab.detachListeners();
                }
            }
        }
        active = false;
    }

    public ActiveTabsOfWindow getTabsForWindow(Window window){
        return activeWindowsAndTabs.get((String) window.getProperties().get(DockStage.KEY_ID));
    }

    public ActiveTabsOfWindow getTabsForWindow(String windowID){
        return activeWindowsAndTabs.get(windowID);
    }

    public static ActiveTab getUXAWrapperFor(DockItemWithInput tab){
        Window window =  tab.getTabPane().getScene().getWindow();
        return ActiveWindowsService.getInstance().getTabsForWindow(window).getActiveTabs().get(tab.toString());
    }

}