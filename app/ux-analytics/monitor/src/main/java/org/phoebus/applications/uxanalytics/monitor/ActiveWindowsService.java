package org.phoebus.applications.uxanalytics.monitor;

import com.sun.javafx.collections.UnmodifiableObservableMap;

import javafx.collections.*;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class ActiveWindowsService {

    private boolean started = false;
    private static ActiveWindowsService instance = null;
    private final ConcurrentHashMap<String, ActiveTabsService> activeWindowsAndTabs = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();


    ConcurrentHashMap<String, ActiveTabsService> getActiveWindowsAndTabs() {
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

                            //When @DockItem s are initialized, their models aren't ready yet.
                            //On startup, the DockItemWithInput will show up but its DisplayModel will be null.
                            //block until the model is ready, in an individual background thread.
                            try {
                                new Thread(() -> {
                                    try {
                                        DisplayRuntimeInstance instance = ((DisplayRuntimeInstance) tab.getProperties().get("application"));
                                        //block until the model is ready
                                        instance.getRepresentation_init().get();
                                        lock.lock();
                                        String windowID = (String) window.getProperties().get(DockStage.KEY_ID);

                                        DockItemWithInput diwi = (DockItemWithInput)tab;
                                        activeWindowsAndTabs.get(windowID).add(diwi);
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
                            activeWindowsAndTabs.putIfAbsent(windowID, new ActiveTabsService(window));
                            for(DockPane item: DockStage.getDockPanes((Stage)window)){
                                for (Tab tab: item.getTabs()){
                                    if(tab instanceof DockItemWithInput){
                                        try {
                                            activeWindowsAndTabs.get(windowID).add((DockItemWithInput)tab);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                                item.getTabs().addListener(UXATabChangeListener);
                            }
                        }
                    }
                }
                else if(change.wasRemoved()){
                    for(Window window: change.getRemoved()){
                        if(window.getProperties().containsKey(DockStage.KEY_ID)){
                            String windowID = (String) window.getProperties().get(DockStage.KEY_ID);
                            activeWindowsAndTabs.remove(windowID);
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
            instance.start();
            instance.started = true;
        }
        return instance;
    }

    public boolean isStarted() {
        return started;
    }

    private void start() {
        instance = this;
        javafx.stage.Window.getWindows().addListener(UXAWindowChangeListener);
    }

    public ActiveTabsService getTabsForWindow(Window window){
        return activeWindowsAndTabs.get((String) window.getProperties().get(DockStage.KEY_ID));
    }

    public ActiveTabsService getTabsForWindow(String windowID){
        return activeWindowsAndTabs.get(windowID);
    }

    public static ActiveWidgetsService getUXAWrapperFor(DockItemWithInput tab){
        Window window =  tab.getTabPane().getScene().getWindow();
        return ActiveWindowsService.getInstance().getTabsForWindow(window).getActiveTabs().get(tab.toString());
    }

}