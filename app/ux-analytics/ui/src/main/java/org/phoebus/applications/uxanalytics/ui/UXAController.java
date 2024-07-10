package org.phoebus.applications.uxanalytics.ui;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.phoebus.applications.uxanalytics.monitor.backend.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;

import java.util.logging.Level;

import static org.phoebus.applications.uxanalytics.ui.UXAnalyticsMain.logger;

public class UXAController {

    UXAMonitor observer;

    public void setObserver(UXAMonitor observer) {
        this.observer = observer;
    }

    BackendConnection connectionLogic;

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
    Label lblProtocol;

    String host;
    String protocol;


    public UXAController(BackendConnection connectionLogic) {
        this.protocol = connectionLogic.getProtocol();
        this.connectionLogic = connectionLogic;
    }

    @FXML
    public void initialize() {
        lblProtocol.setText(protocol);
        txtHost.setText(connectionLogic.getDefaultHost());
        txtPort.setText(connectionLogic.getDefaultPort());
    }

    @FXML
    public int tryConnect(Event event) {
        lblSuccessFailure.setVisible(false);
        try {
            host = txtHost.getText();
            if (host.isEmpty()) {
                lblSuccessFailure.setText("Set a host name.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }
            String port = txtPort.getText();
            if (port.isEmpty() || !port.matches("\\d+")) {
                lblSuccessFailure.setText("Set a valid port number.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }

            String user = txtUser.getText();
            String pass = passPassword.getText();
            try {
                if (!connectionLogic.connect(host, Integer.parseInt(port), user, pass)) {
                    lblSuccessFailure.setText("Failed to connect to host " + host + " as " + user + ".");
                    lblSuccessFailure.setVisible(true);
                    return 1;
                } else {
                    lblSuccessFailure.setText("Connected to server.");
                    lblSuccessFailure.setVisible(true);
                    observer.notifyConnectionChange(connectionLogic);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to connect to server", e);
                lblSuccessFailure.setText("Failed to connect to server.");
                lblSuccessFailure.setVisible(true);
                return 1;
            }
            return 0;
        } finally {

        }
    }
}
