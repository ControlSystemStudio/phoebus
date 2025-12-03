package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.QueueGetPayload;
import org.phoebus.applications.queueserver.api.QueueItem;
import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReRunningPlanController implements Initializable {

    @FXML private Button   copyBtn;
    @FXML private Button   updateBtn;
    @FXML private TextArea planTextArea;

    private final RunEngineService svc = new RunEngineService();
    private static final Logger logger = Logger.getLogger(ReRunningPlanController.class.getPackageName());

    private String lastRunningUid = "";
    private QueueItem cachedRunningItem = null;

    private final boolean viewOnly;

    public ReRunningPlanController() {
        this(false); // default to editable
    }

    public ReRunningPlanController(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    @Override public void initialize(URL u, ResourceBundle b) {
        planTextArea.setEditable(false);
        planTextArea.setStyle("-fx-focus-color: transparent;");
        planTextArea.setStyle("-fx-faint-focus-color: transparent;");

        if (viewOnly) {
            copyBtn.setDisable(true);
            updateBtn.setDisable(true);
        }

        render(StatusBus.latest().get());
        ChangeListener<StatusResponse> statusL = (obs, o, n) -> render(n);
        StatusBus.latest().addListener(statusL);
    }

    private void render(StatusResponse st) {
        if (st == null) {
            planTextArea.clear();
            lastRunningUid = "";
            cachedRunningItem = null;
            copyBtn.setDisable(true);
            updateBtn.setDisable(true);
            return;
        }

        boolean envExists   = st.workerEnvironmentExists();
        String  mgrState    = nz(st.managerState());
        String  wkrState    = nz(st.workerEnvironmentState());
        String  ipkState    = nz(st.ipKernelState());
        boolean runningNow  = st.runningItemUid() != null;

        copyBtn .setDisable(!runningNow);   // RE monitor-mode does not exist in FX yet

        boolean canUpd =
                envExists &&
                        "idle".equals(mgrState) &&
                        "idle".equals(wkrState) &&
                        !"busy".equals(ipkState);
        updateBtn.setDisable(!canUpd);

        String uid = st.runningItemUid();
        if (uid == null) {                         // nothing running
            planTextArea.clear();
            lastRunningUid = "";
            cachedRunningItem = null;
            return;
        }

        // Fetch running item only if it's a new plan
        if (!uid.equals(lastRunningUid)) {
            cachedRunningItem = fetchRunningItem();
            lastRunningUid = uid;
        }

        // Always fetch the latest run list
        List<Map<String,Object>> runList = fetchRunList();

        // Use cached running item to keep displaying plan details
        planTextArea.setText(format(cachedRunningItem, runList));
        planTextArea.positionCaret(0);
    }

    private QueueItem fetchRunningItem() {
        try {
            QueueGetPayload p = svc.queueGetTyped();
            return p.runningItem();                // may be null
        } catch (Exception ex) { 
            logger.log(Level.FINE, "Failed to fetch running item: " + ex.getMessage());
            return null; 
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> fetchRunList() {
        try {
            var env = svc.reRuns(null);
            Object p = env.payload();
            if (p instanceof Map<?,?> m && m.containsKey("run_list"))
                return (List<Map<String,Object>>) m.get("run_list");
        } catch (Exception ex) {
            logger.log(Level.FINE, "Failed to fetch run list: " + ex.getMessage());
        }
        return List.of();
    }

    private static String format(QueueItem item, List<Map<String,Object>> runs) {
        StringBuilder sb = new StringBuilder();

        if (item != null) {
            sb.append("Plan Name: ").append(item.name()).append('\n');

            if (!item.args().isEmpty())
                sb.append("Arguments: ")
                        .append(String.join(", ", item.args().stream()
                                .map(Object::toString).toList()))
                        .append('\n');

            if (!item.kwargs().isEmpty()) {
                sb.append("Parameters:\n");
                item.kwargs().forEach((k,v) ->
                        sb.append("    ").append(k).append(": ").append(v).append('\n'));
            }

            sb.append('\n');
        }

        if (!runs.isEmpty()) {
            sb.append("Runs:\n");
            for (Map<String,Object> r : runs) {
                String uid  = String.valueOf(r.get("uid"));
                boolean open= Boolean.TRUE.equals(r.get("is_open"));
                sb.append("    ").append(uid).append("  ");
                sb.append(open ? "In progress ..." :
                        "Exit status: " + r.get("exit_status"));
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @FXML
    private void copyToQueue() {
        if (cachedRunningItem == null) return;

        try {
            svc.queueItemAdd(cachedRunningItem);
            logger.log(Level.FINE, "Copied running plan to queue: " + cachedRunningItem.name());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to copy running plan to queue", ex);
        }
    }


    @FXML
    private void updateEnvironment() {
        try { 
            svc.environmentUpdate(Map.of()); 
            logger.log(Level.FINE, "Environment update requested");
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to update environment", ex);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
