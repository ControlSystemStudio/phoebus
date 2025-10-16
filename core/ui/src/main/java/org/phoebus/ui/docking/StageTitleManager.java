package org.phoebus.ui.docking;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.stage.Stage;
import org.phoebus.ui.Preferences;

import java.util.List;

/**
 * Helper class to manage the window title.
 */
public class StageTitleManager {

    private static final DockPaneListener listener = StageTitleManager::setStageTitleToTabTitle;

    // This class is only a static utility, so it should not be instantiated.
    private StageTitleManager() {}

    public static void bindStageTitlesToActiveTabs(Stage mainStage) {
        // this will listen to all tab focus events and set the stage title
        // of the stage in which the tab is located to the formatted title.
        DockPane.addListener(listener);

        // This listener is passed down to all child panes when a split occurs,
        // so it fires when any pane in the main stage becomes empty.
        // Therefore, we have to check if all panes are empty.
        DockStage.getDockPanes(mainStage).get(0).addDockPaneEmptyListener(() -> {
            List<DockPane> panes = DockStage.getDockPanes(mainStage);
            for (DockPane pane : panes) {
                if (!pane.getDockItems().isEmpty()) {
                    // If any pane has items, we don't need to set the title to default
                    return;
                }
            }
            // If all panes are empty, set the stage title to the default
            mainStage.titleProperty().unbind();
            setStageTitleToDefault(mainStage);
        });
    }

    private static void setStageTitleToTabTitle(DockItem dockItem) {
        if (dockItem == null) {
            return;
        }
        DockPane pane = dockItem.getDockPane();
        // I don't think this is ever true,
        // but better to have it and not need it than the other way around
        if (pane == null) {
            return;
        }
        pane.deferUntilInScene(scene -> {
            if (scene.getWindow() instanceof Stage stage) {
                if (DockPane.getActiveDockPane() != pane) {
                    // If the dock pane is not active, don't do anything
                    return;
                }
                StringExpression exp = Bindings.format(Preferences.window_title_format, dockItem.labelTextProperty());
                stage.titleProperty().bind(exp);
            }
        });
    }

    private static void setStageTitleToDefault(Stage stage) {
        stage.setTitle(Preferences.default_window_title);
    }
}
