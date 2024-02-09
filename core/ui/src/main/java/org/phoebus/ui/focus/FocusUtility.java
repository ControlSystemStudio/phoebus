package org.phoebus.ui.focus;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockStage;

import java.util.logging.Level;

public class FocusUtility {

    public static Runnable setFocusOn(final Node node){
        {
            Window window = node.getScene().getWindow().getScene().getWindow();
            if (window instanceof Stage)
            {
                final Stage stage = (Stage) window;
                return () -> DockStage.setActiveDockStage(stage);
            } else
            {
                PhoebusApplication.logger.log(Level.WARNING, "Expected 'Stage' for context menu, got " + window);
                return () -> {
                };
            }
        }
    }
}
