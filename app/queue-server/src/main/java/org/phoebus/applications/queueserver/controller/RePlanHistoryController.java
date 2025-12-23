package org.phoebus.applications.queueserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.phoebus.applications.queueserver.api.HistoryGetPayload;
import org.phoebus.applications.queueserver.api.QueueItem;
import org.phoebus.applications.queueserver.api.QueueItemAddBatch;
import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.StatusBus;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RePlanHistoryController implements Initializable {

    @FXML private TableView<Row>  table;
    @FXML private TableColumn<Row,Number> idxCol;
    @FXML private TableColumn<Row,String> typeCol,nameCol,statusCol,paramCol,userCol,grpCol;
    @FXML private Button copyBtn, deselectBtn, clearBtn;
    @FXML private SplitMenuButton exportBtn;
    @FXML private MenuItem exportTxtItem, exportJsonItem, exportYamlItem;


    private final RunEngineService   svc  = new RunEngineService();
    private final ObservableList<Row>rows = FXCollections.observableArrayList();
    private final Map<String,QueueItem> uid2item = new HashMap<>();

    private List<Integer> stickySel = List.of();
    private boolean       ignoreSel = false;
    private static final Logger logger =
            Logger.getLogger(RePlanHistoryController.class.getPackageName());

    private final boolean viewOnly;

    // Drag-to-select state
    private int dragStartRow = -1;

    public RePlanHistoryController() {
        this(false); // default to editable
    }

    public RePlanHistoryController(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    @Override public void initialize(URL u, ResourceBundle rb) {

        table.setItems(rows);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        idxCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(rows.indexOf(c.getValue())+1));
        idxCol.setSortable(false);

        typeCol  .setCellValueFactory(c -> new ReadOnlyStringWrapper(firstLetter(c.getValue().itemType())));
        typeCol.setStyle("-fx-alignment:CENTER;");
        nameCol  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        statusCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().status()));
        paramCol .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().params()));
        userCol  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().user()));
        grpCol   .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().group()));

        table.getSelectionModel().getSelectedIndices()
                .addListener((ListChangeListener<? super Integer>) c -> updateButtonStates());

        deselectBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            stickySel = List.of();
        });

        if (viewOnly) {
            copyBtn.setDisable(true);
            clearBtn.setDisable(true);
        } else {
            hookButtons();
            updateButtonStates();
        }
        
        hookExportButtons();

        table.getSelectionModel().getSelectedIndices()
                .addListener((ListChangeListener<? super Integer>) c -> {
                    if (!ignoreSel) stickySel =
                            List.copyOf(table.getSelectionModel().getSelectedIndices());
                });

        ChangeListener<StatusResponse> l =
                (o,oldV,nv) -> {
                    // Run refresh in background thread to avoid blocking UI
                    new Thread(() -> refresh(nv)).start();
                };
        StatusBus.latest().addListener(l);

        // Add drag-to-select functionality
        setupDragSelection();

        // Run initial refresh in background thread
        new Thread(() -> refresh(StatusBus.latest().get())).start();
    }

    /**
     * Sets up drag-to-select functionality for the table.
     * Users can click and drag to select multiple rows.
     */
    private void setupDragSelection() {
        table.setOnMousePressed(event -> {
            int index = getRowIndexAt(event.getY());
            if (index >= 0 && index < rows.size()) {
                dragStartRow = index;
                // Don't consume - let normal click work
            } else {
                dragStartRow = -1;
            }
        });

        table.setOnMouseDragged(event -> {
            if (dragStartRow >= 0) {
                int currentIndex = getRowIndexAt(event.getY());
                if (currentIndex >= 0 && currentIndex < rows.size()) {
                    selectRange(dragStartRow, currentIndex);
                }
                event.consume(); // Prevent default drag behavior
            }
        });

        table.setOnMouseReleased(event -> {
            dragStartRow = -1;
        });
    }

    /**
     * Gets the row index at the specified Y coordinate relative to the table.
     */
    private int getRowIndexAt(double y) {
        // Get the fixed cell size or estimate
        if (rows.isEmpty()) return -1;

        // Look through visible rows
        for (javafx.scene.Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow) {
                @SuppressWarnings("unchecked")
                TableRow<Row> row = (TableRow<Row>) node;

                // Convert to table's coordinate space
                javafx.geometry.Bounds boundsInTable = table.sceneToLocal(
                    row.localToScene(row.getBoundsInLocal())
                );

                if (y >= boundsInTable.getMinY() && y <= boundsInTable.getMaxY()) {
                    return row.getIndex();
                }
            }
        }
        return -1;
    }

    /**
     * Selects all rows between start and end indices (inclusive).
     */
    private void selectRange(int start, int end) {
        table.getSelectionModel().clearSelection();
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        for (int i = from; i <= to; i++) {
            table.getSelectionModel().select(i);
        }
    }

    private void refresh(StatusResponse st) {

        if (st == null) {
            // Don't clear history - keep last data visible for users
            // Just update button states (will be disabled via StatusBus)
            Platform.runLater(this::updateButtonStates);
            return;
        }

        try {
            // Blocking HTTP call - runs on background thread
            HistoryGetPayload hp = svc.historyGetTyped();   // typed DTO

            // UI updates must happen on FX thread
            Platform.runLater(() -> {
                ignoreSel = true;
                rebuildRows(hp.items());
                ignoreSel = false;
                restoreSelection(stickySel);
            });
        } catch (Exception ex) {
            logger.log(Level.FINE, "History refresh failed: " + ex.getMessage());
        }
    }

    private void rebuildRows(List<QueueItem> items) {
        rows.clear(); uid2item.clear();
        if (items == null) { updateButtonStates(); return; }

        for (QueueItem qi : items) {
            rows.add(new Row(
                    qi.itemUid(),
                    qi.itemType(),
                    qi.name(),
                    exitStatus(qi),
                    fmtParams(qi),
                    qi.user(),
                    qi.userGroup()));
            uid2item.put(qi.itemUid(), qi);
        }
        autoResizeColumns();
        updateButtonStates();
    }

    private void hookButtons() {
        copyBtn    .setOnAction(e -> copySelectedToQueue());
        deselectBtn.setOnAction(e -> table.getSelectionModel().clearSelection());
        clearBtn   .setOnAction(e -> clearHistory());

        table.getSelectionModel().getSelectedIndices()
                .addListener((ListChangeListener<? super Integer>) c -> updateButtonStates());
    }
    
    private void hookExportButtons() {
        exportTxtItem.setOnAction(e -> exportHistory(PlanHistorySaver.Format.TXT, "txt"));
        exportJsonItem.setOnAction(e -> exportHistory(PlanHistorySaver.Format.JSON, "json"));
        exportYamlItem.setOnAction(e -> exportHistory(PlanHistorySaver.Format.YAML, "yaml"));
        
        exportBtn.setOnAction(e -> exportHistory(PlanHistorySaver.Format.TXT, "txt"));
    }

    private void copySelectedToQueue() {
        var sel = table.getSelectionModel().getSelectedIndices();
        if (sel.isEmpty()) return;

        List<QueueItem> clones = sel.stream()
                .map(rows::get)
                .map(r -> uid2item.get(r.uid))
                .filter(Objects::nonNull)
                .map(q -> new QueueItem(
                        q.itemType(),
                        q.name(),
                        q.args(),
                        q.kwargs(),
                        null,
                        q.user(),
                        q.userGroup(),
                        q.result()
                ))
                .toList();

        try {
            QueueItemAddBatch req =
                    new QueueItemAddBatch(clones, "GUI Client", "primary");
            svc.queueItemAddBatch(req);                   // service takes DTO, not Map
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Copy-to-Queue failed", ex);
        }
    }

    private void clearHistory() {
        try { svc.historyClear(); }
        catch (Exception ex) { logger.log(Level.WARNING, "Clear-history failed", ex); }
    }
    
    private void exportHistory(PlanHistorySaver.Format fmt, String ext) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase(), "."+ext));
        fc.setInitialFileName("plan_history." + ext);
        File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;

        Platform.runLater(() -> {
            try {
                HistoryGetPayload hp = svc.historyGetTyped();
                List<QueueItem> items = hp.items();
                PlanHistorySaver.save(items, f, fmt);
                logger.log(Level.FINE, () -> "Exported plan history → " + f);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Export history failed", e);
            }
        });
    }

    private void updateButtonStates() {
        StatusResponse status = StatusBus.latest().get();
        boolean connected = status != null;
        boolean envOpen = connected && status.workerEnvironmentExists();
        boolean hasSel = !table.getSelectionModel().getSelectedIndices().isEmpty();

        if (viewOnly) {
            copyBtn.setDisable(true);
            clearBtn.setDisable(true);
            deselectBtn.setDisable(!hasSel);
        } else {
            // Only allow modifications when connected AND environment is open
            copyBtn.setDisable(!(envOpen && hasSel));
            clearBtn.setDisable(!(envOpen && !rows.isEmpty()));
            deselectBtn.setDisable(!hasSel);
        }
    }


    private void autoResizeColumns() {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (TableColumn<Row,?> col : table.getColumns()) {
            Text probe = new Text(col.getText());
            double max = probe.getLayoutBounds().getWidth();
            for (int i=0;i<rows.size();i++) {
                Object v = col.getCellData(i);
                if (v!=null) {
                    probe = new Text(v.toString());
                    max = Math.max(max, probe.getLayoutBounds().getWidth());
                }
            }
            col.setPrefWidth(max + 14);
        }
    }

    private void restoreSelection(Collection<Integer> idx) {
        if (idx.isEmpty()) return;
        var sm = table.getSelectionModel();
        var fm = table.getFocusModel();
        sm.clearSelection();
        int first = -1;
        for (Integer i : idx) {
            if (i>=0 && i<rows.size()) {
                sm.select(i); fm.focus(i);
                if (first==-1) first = i;
            }
        }
        if (first!=-1) table.requestFocus();
    }

    private static String firstLetter(String s){
        return (s==null||s.isBlank())?"":s.substring(0,1).toUpperCase();
    }
    private static String fmtParams(QueueItem q){
        String a = Optional.ofNullable(q.args()).orElse(List.of())
                .stream().map(Object::toString).collect(Collectors.joining(", "));
        String k = Optional.ofNullable(q.kwargs()).orElse(Map.of())
                .entrySet().stream()
                .map(e -> e.getKey()+": "+e.getValue())
                .collect(Collectors.joining(", "));
        return Stream.of(a,k).filter(s->!s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static String exitStatus(QueueItem q){
        Map<String,Object> res = q.result();

        return res == null ? ""
                : String.valueOf(res.getOrDefault("exit_status",""));
    }

    private record Row(
            String uid,
            String itemType,
            String name,
            String status,
            String params,
            String user,
            String group) {}
            
    private static final class PlanHistorySaver {

        private static final ObjectMapper JSON =
                (ObjectMapper) new ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .withDefaultPrettyPrinter()
                        .getFactory()
                        .getCodec();

        private static final DateTimeFormatter TS =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                        .withZone(ZoneId.systemDefault());

        private PlanHistorySaver() {}
        enum Format {TXT, JSON, YAML}

        static void save(List<QueueItem> history, File file, Format fmt) throws Exception {
            switch (fmt) {
                case TXT  -> writeTxt(history, file);
                case JSON -> JSON.writerWithDefaultPrettyPrinter().writeValue(file, history);
                case YAML -> new ObjectMapper(new YAMLFactory())
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(file, history);
            }
        }

        private static void writeTxt(List<QueueItem> h, File f) throws Exception {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath())) {

                int idx = 0;
                for (QueueItem qi : h) {

                    Map<String, Object> res = qi.result();
                    double t0 = res == null ? 0 : ((Number)res.getOrDefault("time_start",0)).doubleValue();
                    double t1 = res == null ? 0 : ((Number)res.getOrDefault("time_stop" ,0)).doubleValue();

                    w.write("=".repeat(80)); w.newLine();

                    String hdr = "PLAN " + (++idx);
                    if (t0>0) {
                        hdr += ": " + TS.format(Instant.ofEpochMilli((long)(t0*1_000)));
                        if (t1>0)
                            hdr += " - " + TS.format(Instant.ofEpochMilli((long)(t1*1_000)));
                    }
                    w.write(center(hdr,80)); w.newLine();
                    w.write("=".repeat(80)); w.newLine();

                    String pretty = JSON.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(JSON.convertValue(qi, Map.class));
                    w.write(pretty); w.newLine(); w.newLine();
                }
            }
        }

        private static String center(String text, int width) {
            int pad = Math.max(0, (width - text.length()) / 2);
            return " ".repeat(pad) + text;
        }
    }
}
