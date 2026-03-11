package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.util.ConnectionManager;
import org.phoebus.applications.queueserver.util.ConnectionManager.ConnectionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

/**
 * Thin UI controller for the connection manager widget.
 * All connection logic lives in {@link ConnectionManager}.
 * Multiple instances of this controller (one per app) share the same
 * ConnectionManager singleton, so connecting in one app connects both.
 */
public final class ReManagerConnectionController {

    @FXML private ToggleButton autoConnectToggle;
    @FXML private Label connectionStatusLabel;

    private final ConnectionManager cm = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // Reflect current state immediately
        updateUI(cm.getState());
        autoConnectToggle.setSelected(cm.isAutoConnect());

        // Listen for shared state changes
        cm.stateProperty().addListener((obs, oldState, newState) -> updateUI(newState));

        // Auto-connect on startup if toggle is selected and not yet connected
        if (autoConnectToggle.isSelected()
                && cm.getState() == ConnectionState.DISCONNECTED) {
            cm.start();
        }
    }

    @FXML
    private void toggleConnection() {
        if (autoConnectToggle.isSelected()) {
            cm.start();
        } else {
            cm.stop();
        }
    }

    private void updateUI(ConnectionState state) {
        autoConnectToggle.setSelected(state != ConnectionState.DISCONNECTED);

        switch (state) {
            case DISCONNECTED -> {
                connectionStatusLabel.setText("OFFLINE");
                connectionStatusLabel.setStyle("-fx-text-fill: grey;");
                autoConnectToggle.setText("Connect");
            }
            case CONNECTING -> {
                connectionStatusLabel.setText("CONNECTING");
                connectionStatusLabel.setStyle("-fx-text-fill: grey;");
                autoConnectToggle.setText("Disconnect");
            }
            case NETWORK_ERROR -> {
                connectionStatusLabel.setText("NETWORK");
                connectionStatusLabel.setStyle("-fx-text-fill: #00008B;");
                autoConnectToggle.setText("Disconnect");
            }
            case NO_STATUS -> {
                connectionStatusLabel.setText("STATUS");
                connectionStatusLabel.setStyle("-fx-text-fill: red;");
                autoConnectToggle.setText("Disconnect");
            }
            case CONNECTED -> {
                connectionStatusLabel.setText("CONNECTED");
                connectionStatusLabel.setStyle("-fx-text-fill: green;");
                autoConnectToggle.setText("Disconnect");
            }
        }
    }
}
