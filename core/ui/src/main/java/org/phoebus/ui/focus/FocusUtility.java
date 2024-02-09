package org.phoebus.ui.focus;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockStage;

import java.util.logging.Level;

/**
 * A utility class which provides support for handling Focus
 */
public class FocusUtility {

    /**
     * Create a Runnable which when called sets the focus on the DockPane hosting the provided Node
     * @param node A node
     * @return A Runnable to set the Focus on the Pane which holds the Node
     */
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
