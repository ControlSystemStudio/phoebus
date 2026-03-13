package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.view.ViewFactory;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ApplicationController implements Initializable {

    @FXML private Tab monitorQueueTab;
    @FXML private Tab editAndControlQueueTab;
    @FXML private TabPane tabPane;

    private static final Logger logger = Logger.getLogger(ApplicationController.class.getPackageName());

    @Override public void initialize(URL url, ResourceBundle rb) {
        logger.log(Level.FINE, "Initializing ApplicationController");
        monitorQueueTab.setContent(ViewFactory.MONITOR_QUEUE.get());
        editAndControlQueueTab.setContent(ViewFactory.EDIT_AND_CONTROL_QUEUE.get());

        // Disable focus traversal on all components
        disableFocusTraversal(monitorQueueTab.getContent());
        disableFocusTraversal(editAndControlQueueTab.getContent());

        logger.log(Level.FINE, "ApplicationController initialization complete");
    }

    /**
     * Recursively disables focus traversal on all nodes in the scene graph,
     * except for TableView which remains focus traversable for arrow key navigation.
     */
    private void disableFocusTraversal(Node node) {
        if (node == null) return;

        // Allow TableView to remain focus traversable for arrow key navigation
        if (!(node instanceof TableView)) {
            node.setFocusTraversable(false);
        }

        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                disableFocusTraversal(child);
            }
        }
    }

}
