package org.phoebus.applications.uxanalytics.monitor.representation;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeApplication;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.csstudio.display.builder.runtime.internal.DisplayRuntime;
import org.epics.vtype.Display;
import org.junit.jupiter.api.*;
import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;
import org.phoebus.applications.uxanalytics.monitor.backend.database.ServiceLayerConnection;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.mockito.Mockito.mock;

public class ActiveWindowsServiceTest {

    static ActiveWindowsService activeWindowsService;
    static Stage stage;
    static DockPane pane;
    static DisplayRuntimeApplication app;
    static DisplayRuntimeInstance displayRuntimeInstance;
    static FutureTask<Boolean> loadedTask = new FutureTask<>(() -> true);


    @BeforeAll
    static void setUp() {
        Platform.startup(() -> {});
        Platform.runLater(()->{
            app = new DisplayRuntimeApplication();
            stage = new Stage();
            Scene scene = new Scene(new BorderPane(), 800, 600);
            stage.setScene(scene);
            pane = DockStage.configureStage(stage);
            DockStage.setActiveDockStage(stage);
            try {
                URI testResource = ActiveWindowsService.class.getResource("/test.bob").toURI();
                displayRuntimeInstance = app.create(testResource);
                activeWindowsService = ActiveWindowsService.getInstance();
                loadedTask.run();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

        });
    }

    @AfterEach
    void reset() {
        if(activeWindowsService != null)
            activeWindowsService.clear();
    }

    @AfterAll
    static void tearDown() {
        Platform.runLater(() ->{
            if(stage!=null)
                stage.close();
        });
        Platform.exit();
    }

    @Test
    synchronized void testWindowIsPresentInActiveWindowsServiceWhenActive(){
        try {
            loadedTask.get();
            DockStage.deferUntilAllPanesOfStageHaveScenes(
                    stage,
                    () -> {
                        activeWindowsService.stop();
                        activeWindowsService.start();
                        String windowID = (String) stage.getProperties().get(DockStage.KEY_ID);
                        ConcurrentHashMap<String, ActiveTabsOfWindow> activeWindowsAndTabs = activeWindowsService.getActiveWindowsAndTabs();
                        DockItemWithInput dockItem = (DockItemWithInput)displayRuntimeInstance.getDockItem();
                        Assertions.assertTrue(activeWindowsAndTabs.containsKey(windowID));
                        Assertions.assertEquals(1, activeWindowsAndTabs.size());
                        Assertions.assertTrue(activeWindowsService.getTabsForWindow(windowID).contains(dockItem));
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    synchronized void testWindowIsNotPresentInActiveWindowsServiceWhenInactive(){
        try {
            loadedTask.get();
            DockStage.deferUntilAllPanesOfStageHaveScenes(
                    stage,
                    () -> {
                        activeWindowsService.stop();
                        activeWindowsService.start();
                        activeWindowsService.stop();
                        Assertions.assertFalse(activeWindowsService.isActive());
                        String windowID = (String) stage.getProperties().get(DockStage.KEY_ID);
                        ConcurrentHashMap<String, ActiveTabsOfWindow> activeWindowsAndTabs = activeWindowsService.getActiveWindowsAndTabs();
                        Assertions.assertFalse(activeWindowsAndTabs.containsKey(windowID));
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    synchronized void testStartIsIdempotent(){
        try {
            loadedTask.get();
            DockStage.deferUntilAllPanesOfStageHaveScenes(
                    stage,
                    () -> {
                        activeWindowsService.stop();
                        activeWindowsService.start();
                        activeWindowsService.start();
                        activeWindowsService.start();
                        String windowID = (String) stage.getProperties().get(DockStage.KEY_ID);
                        ConcurrentHashMap<String, ActiveTabsOfWindow> activeWindowsAndTabs = activeWindowsService.getActiveWindowsAndTabs();
                        Assertions.assertTrue(activeWindowsAndTabs.containsKey(windowID));
                        Assertions.assertEquals(1, activeWindowsAndTabs.size());
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    synchronized void testAddingATabToAnActiveWindow() {
        try {
            loadedTask.get();
            DockStage.deferUntilAllPanesOfStageHaveScenes(
                    stage,
                    () -> {
                        activeWindowsService.stop();
                        activeWindowsService.start();
                        String windowID = (String) stage.getProperties().get(DockStage.KEY_ID);
                        ConcurrentHashMap<String, ActiveTabsOfWindow> activeWindowsAndTabs = activeWindowsService.getActiveWindowsAndTabs();
                        assert(activeWindowsAndTabs.containsKey(windowID));
                        try {
                            URI uri = ActiveWindowsService.class.getResource("/thirdThing.bob").toURI();
                            DisplayInfo info = DisplayInfo.forURI(uri);
                            CompletableFuture<DisplayRuntimeInstance> future = new CompletableFuture<>();
                            Platform.runLater(() -> {
                                DisplayRuntimeInstance otherInstance  = app.create(uri);
                                future.complete(otherInstance);
                            });
                            DisplayRuntimeInstance otherInstance = future.get();
                            Assertions.assertEquals(2, activeWindowsAndTabs.get(windowID).getActiveTabs().size());
                            CompletableFuture<Void> closeFuture = new CompletableFuture<>();
                            Thread.sleep(2000);
                            otherInstance.getDockItem().prepareToClose();
                            Platform.runLater(()-> {
                                try {
                                     otherInstance.getDockItem().close();
                                     closeFuture.complete(null);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            closeFuture.get();
                            Assertions.assertEquals(1, activeWindowsAndTabs.get(windowID).getActiveTabs().size());

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
