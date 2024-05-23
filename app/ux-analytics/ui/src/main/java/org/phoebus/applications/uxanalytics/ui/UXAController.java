package org.phoebus.applications.uxanalytics.ui;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;

import java.util.logging.Level;

import static org.phoebus.applications.uxanalytics.ui.UXAnalyticsUI.logger;

public class UXAController {

    UXAMonitor observer;

    public void setObserver(UXAMonitor observer) {
        this.observer = observer;
    }

    @FXML
    TextField txtHost;

    @FXML
    TextField txtPort;

    @FXML
    TextField txtUser;

    @FXML
    PasswordField passPassword;

    @FXML
    Button btnConnect;

    @FXML
    Label lblSuccessFailure;

    @FXML
    public int tryConnect(Event event) {
        lblSuccessFailure.setVisible(false);
        try {
            String host = txtHost.getText();
            if (host.isEmpty()) {
                lblSuccessFailure.setText("Set a host name.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }
            host = "neo4j://" + host;
            String port = txtPort.getText();
            if (port.isEmpty() || !port.matches("\\d+")) {
                lblSuccessFailure.setText("Set a valid port number.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }
            host = host + ":" + port;

            String user = txtUser.getText();
            if (user.isEmpty()) {
                lblSuccessFailure.setText("Set a user name.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }
            String pass = passPassword.getText();
            try (var driver = GraphDatabase.driver(host, AuthTokens.basic(user, pass))) {
                driver.verifyConnectivity();
                lblSuccessFailure.setText("Connected to server.");
                lblSuccessFailure.setVisible(true);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to connect to server", e);
            lblSuccessFailure.setText("Failed to connect to server.");
            lblSuccessFailure.setVisible(true);
            return 1;
        }
        return 0;
    }
}
