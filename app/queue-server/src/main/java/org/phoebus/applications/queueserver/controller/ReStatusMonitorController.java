package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public final class ReStatusMonitorController implements Initializable {

    @FXML private Label envLabel;
    @FXML private Label managerLabel;
    @FXML private Label reLabel;
    @FXML private Label histLabel;
    @FXML private Label autoLabel;
    @FXML private Label stopLabel;
    @FXML private Label loopLabel;
    @FXML private Label queueLabel;

    private ChangeListener<StatusResponse> listener;

    @Override public void initialize(URL url, ResourceBundle rb) {

        render(StatusBus.latest().get());

        listener = (obs, oldVal, newVal) -> render(newVal);
        StatusBus.latest().addListener(listener);
    }

    public void shutdown() {
        StatusBus.latest().removeListener(listener);
    }

    private void render(StatusResponse s) {
        if (s == null) {          // offline / not connected
            dashAll();
            return;
        }

        set(envLabel,   "RE Environment: ",   s.workerEnvironmentExists() ? "OPEN" : "CLOSED");
        set(managerLabel,"Manager state: ",   uc(s.managerState()));
        set(reLabel,     "RE state: ",        uc(s.reState()));
        set(histLabel,   "Items in history: ",s.itemsInHistory());
        set(queueLabel,  "Items in queue: ",  s.itemsInQueue());
        set(autoLabel,   "Queue AUTOSTART: ", s.queueAutostartEnabled() ? "ON"  : "OFF");
        set(stopLabel,   "Queue STOP pending: ", s.queueStopPending()   ? "YES" : "NO");

        Boolean loop = s.planQueueMode() == null ? null : s.planQueueMode().loop();
        set(loopLabel, "Queue LOOP mode: ", loop == null ? "—" : (loop ? "ON" : "OFF"));
    }

    private void dashAll() {
        set(envLabel,   "RE Environment: ",   "-");
        set(managerLabel,"Manager state: ",   "-");
        set(reLabel,    "RE state: ",         "-");
        set(histLabel,  "Items in history: ", "-");
        set(queueLabel, "Items in queue: ",   "-");
        set(autoLabel,  "Queue AUTOSTART: ",  "-");
        set(stopLabel,  "Queue STOP pending: ","-");
        set(loopLabel,  "Queue LOOP mode: ",  "-");
    }

    private static void set(Label lbl, String prefix, Object value) {
        lbl.setText(prefix + (value == null ? "-" : String.valueOf(value)));
    }
    private static String uc(String s) { return s == null ? null : s.toUpperCase(); }
}
