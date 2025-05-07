package org.phoebus.applications.uxanalytics.ui;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.phoebus.applications.uxanalytics.monitor.backend.database.BackendConnection;
import org.phoebus.applications.uxanalytics.monitor.UXAMonitor;
import org.phoebus.framework.preferences.PhoebusPreferenceService;

import java.util.logging.Level;

import static org.phoebus.applications.uxanalytics.ui.UXAnalyticsMain.logger;

public class UXAController {

    UXAMonitor observer;

    public void setObserver(UXAMonitor observer) {
        this.observer = observer;
    }

    BackendConnection connectionLogic;

    @FXML
    Button btnAgree;
    @FXML
    Button buttonDisagree;
    @FXML
    CheckBox chkRemember;

    String host;
    String protocol;


    public UXAController(BackendConnection connectionLogic) {
        this.connectionLogic = connectionLogic;
    }

    @FXML
    public void initialize() {
        chkRemember.setSelected(ConsentPersistence.getConsent());
    }

    @FXML
    public void onAgree(ActionEvent event) {
        observer.enableTracking();
        if (chkRemember.isSelected()) {
            ConsentPersistence.storeConsent();
        }
        else{
            ConsentPersistence.deleteConsent();
        }
        ((Button) event.getSource()).getScene().getWindow().hide();
    }

    @FXML
    public void onDisagree(ActionEvent event) {
        observer.disableTracking();
        if (chkRemember.isSelected()){
            ConsentPersistence.revokeConsent();
        }
        else{
            ConsentPersistence.deleteConsent();
        }
        ((Button) event.getSource()).getScene().getWindow().hide();
    }

}
