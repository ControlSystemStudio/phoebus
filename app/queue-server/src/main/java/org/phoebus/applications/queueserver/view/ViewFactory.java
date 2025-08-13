package org.phoebus.applications.queueserver.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralised FXML loader.  Add new enum constants for every view you create and
 * call {@code get()} to obtain the root node.
 *
 * FXML files themselves continue to live under {@code resources/view}.
 */
public enum ViewFactory {

    APPLICATION            ("/org/phoebus/applications/queueserver/view/Application.fxml"),
    MONITOR_QUEUE          ("/org/phoebus/applications/queueserver/view/MonitorQueue.fxml"),
    EDIT_AND_CONTROL_QUEUE ("/org/phoebus/applications/queueserver/view/EditAndControlQueue.fxml");

    private static final Logger LOG = Logger.getLogger(ViewFactory.class.getName());

    private final String path;

    ViewFactory(String path) { this.path = path; }

    public Parent get() {
        try {
            return FXMLLoader.load(ViewFactory.class.getResource(path));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "FXML load failed: " + path, ex);
            return new StackPane(new Label("⚠ Unable to load " + path));
        }
    }

}
