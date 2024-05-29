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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;


/*
    * Get active @Window instances
    * For each window, add it to a map with a set of tabs as the value,
    * Attach our custom ToolkitListener to each writable Widget.
    * @author Evan Daykin
 */
public class ActiveWindowsService {

    private boolean started = false;
    private static ActiveWindowsService instance = null;
    private final ConcurrentHashMap<String, ActiveTabsService> activeWindowsAndTabs = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();


    //don't want anyone faffing about with the map, but they should be able to see it
    public ConcurrentHashMap<String, ActiveTabsService> getActiveWindowsAndTabs() {
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
                                        instance.getRepresentation_init().get();
                                        lock.lock();
                                        String windowID = (String) window.getProperties().get(DockStage.KEY_ID);

                                        //FIXME: this shouldn't happen, I think
                                        if(!activeWindowsAndTabs.containsKey(windowID)){
                                            System.out.println("Window "+windowID+ "("+window.getClass()+ ") not found in activeWindowsAndTabs, adding it now.");
                                            activeWindowsAndTabs.putIfAbsent(windowID, new ActiveTabsService(window));
                                        }

                                        DockItemWithInput diwi = (DockItemWithInput)tab;
                                        activeWindowsAndTabs.get(windowID).add(diwi);
                                        lock.unlock();
                                        System.out.println("There are now "+activeWindowsAndTabs.get(windowID).getActiveTabs().size()+" tabs in window "+windowID);
                                        System.out.println("There are now "+activeWindowsAndTabs.size()+" windows tracked.");

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else if (change.wasRemoved()) {
                    Window window = change.getList().get(0).getTabPane().getScene().getWindow();
                    String windowID = (String) window.getProperties().get(DockStage.KEY_ID);
                    for(Tab tab: change.getRemoved()){
                        if(tab instanceof DockItemWithInput && activeWindowsAndTabs.get(windowID).contains((DockItemWithInput) tab)){
                            activeWindowsAndTabs.get(windowID).remove((DockItemWithInput) tab);
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
                            if(!activeWindowsAndTabs.containsKey(windowID)){
                                System.out.println("Window "+windowID+ "("+window.getClass()+ ") not found in activeWindowsAndTabs, adding it now.");
                            }
                            activeWindowsAndTabs.putIfAbsent(windowID, new ActiveTabsService(window));
                            for(DockPane item: DockStage.getDockPanes((Stage)window)){
                                //initialize
                                for (Tab tab: item.getTabs()){
                                    if(tab instanceof DockItemWithInput){

                                        try {
                                            activeWindowsAndTabs.get(windowID).add((DockItemWithInput)tab);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                                //tack on a listener for changes
                                item.getTabs().addListener(UXATabChangeListener);
                            }
                        }
                    }
                }
                else if(change.wasRemoved()){
                    for(Window window: change.getRemoved()){
                        if(window.getProperties().containsKey(DockStage.KEY_ID)){
                            System.out.println("close " + window.getProperties().get(DockStage.KEY_ID));
                            activeWindowsAndTabs.remove(window);
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
        return activeWindowsAndTabs.get(window);
    }

}