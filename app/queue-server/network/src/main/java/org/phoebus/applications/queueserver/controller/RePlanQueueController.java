package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.*;
import org.phoebus.applications.queueserver.api.QueueGetPayload;
import org.phoebus.applications.queueserver.api.QueueItem;
import org.phoebus.applications.queueserver.api.StatusResponse;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.PlansCache;
import org.phoebus.applications.queueserver.util.StatusBus;
import org.phoebus.applications.queueserver.util.QueueItemSelectionEvent;
import org.phoebus.applications.queueserver.util.PythonParameterConverter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RePlanQueueController implements Initializable {

    @FXML private TableView<Row>              table;
    @FXML private TableColumn<Row,Number>     idxCol;
    @FXML private TableColumn<Row,String>     typeCol, nameCol, paramCol, userCol, grpCol;
    @FXML private Button                      upBtn, downBtn, topBtn, bottomBtn,
            deleteBtn, duplicateBtn, clearBtn, deselectBtn;
    @FXML private ToggleButton                loopBtn;


    private final RunEngineService svc  = new RunEngineService();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final Map<String, QueueItem> uid2item = new HashMap<>();
    private final Map<String, Map<String, Object>> allowedPlans = new HashMap<>();
    private final Map<String, Map<String, Object>> allowedInstructions = new HashMap<>();
    private List<String> stickySel   = List.of();   // last user selection
    private boolean      ignoreSticky= false;       // guard while we rebuild
    private final AtomicLong refreshSeq = new AtomicLong(); // discard stale bg refreshes

    private static final Logger logger =
            Logger.getLogger(RePlanQueueController.class.getPackageName());

    private final boolean viewOnly;

    public RePlanQueueController() {
        this(false); // default to editable
    }

    public RePlanQueueController(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    @Override public void initialize(URL url, ResourceBundle rb) {

        table.setItems(rows);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        idxCol .setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(rows.indexOf(c.getValue()) + 1));
        idxCol.setSortable(false);

        typeCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(firstLetter(c.getValue().itemType())));
        typeCol.setStyle("-fx-alignment:CENTER;");

        nameCol .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        paramCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().params()));
        userCol .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().user()));
        grpCol  .setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().group()));

        table.getSelectionModel().getSelectedIndices()
                .addListener((ListChangeListener<? super Integer>) c -> updateButtonStates());

        deselectBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            stickySel = List.of();
        });

        if (viewOnly) {
            upBtn.setDisable(true);
            downBtn.setDisable(true);
            topBtn.setDisable(true);
            bottomBtn.setDisable(true);
            deleteBtn.setDisable(true);
            duplicateBtn.setDisable(true);
            clearBtn.setDisable(true);
            loopBtn.setDisable(true);
        } else {
            enableRowDragAndDrop();
            hookButtons();
            updateButtonStates();
        }

        table.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Row>) c -> {
                    if (!ignoreSticky) {
                        stickySel = selectedUids();
                        // Notify plan viewer of selection change
                        notifySelectionChange();
                    }
                });

        initializeAllowedInstructions();

        // Use shared plans cache - copy to local map when loaded
        if (PlansCache.isLoaded()) {
            allowedPlans.clear();
            allowedPlans.putAll(PlansCache.get());
        }

        // Listen for plans cache updates
        PlansCache.addListener((o, oldV, newV) -> {
            Platform.runLater(() -> {
                allowedPlans.clear();
                if (newV != null) {
                    allowedPlans.putAll(newV);
                }
            });
        });

        // Listen for status changes to refresh queue
        ChangeListener<StatusResponse> poll =
                (o,oldV,newV) -> {
                    long seq = refreshSeq.incrementAndGet();
                    new Thread(() -> bgRefresh(newV, seq)).start();
                };
        StatusBus.addListener(poll);

        // Initial refresh on background thread
        long seq = refreshSeq.incrementAndGet();
        new Thread(() -> bgRefresh(StatusBus.latest().get(), seq)).start();
    }

    private void initializeAllowedInstructions() {
        Map<String, Object> queueStopInstr = new HashMap<>();
        queueStopInstr.put("name", "queue_stop");
        queueStopInstr.put("description", "Stop execution of the queue.");
        queueStopInstr.put("parameters", List.of());
        allowedInstructions.put("queue_stop", queueStopInstr);
    }

    /**
     * Background refresh: fetches queue data on a worker thread, then applies
     * it on the FX thread.  Uses a generation counter ({@code seq}) so that
     * out-of-order responses are silently discarded – only the latest data is
     * ever applied to the UI.
     */
    private void bgRefresh(StatusResponse st, long seq) {
        if (st == null) {
            Platform.runLater(() -> {
                if (seq < refreshSeq.get()) return;
                updateButtonStates();
            });
            return;
        }
        try {
            QueueGetPayload qp = svc.queueGetTyped();
            Platform.runLater(() -> {
                if (seq < refreshSeq.get()) return; // stale – discard
                applyQueueData(qp, List.of(), st);
            });
        } catch (Exception ex) {
            logger.log(Level.FINE, "Queue refresh failed: " + ex.getMessage());
        }
    }

    /** Applies fetched queue data to the UI.  Must be called on the FX thread. */
    private void applyQueueData(QueueGetPayload qp, Collection<String> explicitFocus,
                                StatusResponse st) {
        QueueItemSelectionEvent selEvent = QueueItemSelectionEvent.getInstance();

        ignoreSticky = true;
        rebuildRows(qp.queue());
        ignoreSticky = false;

        List<String> focus;
        List<String> pendingUids = selEvent.getPendingSelectUids();
        if (pendingUids != null) {
            // We have exact UIDs to select (from a successful add/copy response)
            List<String> found = pendingUids.stream()
                    .filter(uid2item::containsKey)
                    .toList();
            if (!found.isEmpty()) {
                focus = found;
                selEvent.clearPendingSelect();
            } else {
                // UIDs not yet in this refresh's data – keep for next cycle
                focus = explicitFocus.isEmpty() ? stickySel
                        : List.copyOf(explicitFocus);
            }
        } else if (!explicitFocus.isEmpty()) {
            focus = List.copyOf(explicitFocus);
        } else {
            focus = stickySel;
        }

        applyFocus(focus);

        // applyFocus suppresses intermediate notifications, so update
        // stickySel and notify once here.
        if (table.getSelectionModel().getSelectedItems().isEmpty()) {
            stickySel = List.of();
        } else {
            stickySel = selectedUids();
        }
        notifySelectionChange();

        loopBtn.setSelected(Optional.ofNullable(st.planQueueMode())
                .map(StatusResponse.PlanQueueMode::loop)
                .orElse(false));
    }

    private void rebuildRows(List<QueueItem> items) {
        rows.clear(); uid2item.clear();
        if (items == null) { updateButtonStates(); return; }

        for (QueueItem q : items) {
            rows.add(new Row(q.itemUid(), q.itemType(), q.name(),
                    fmtParams(q, q.itemType(), q.name()), q.user(), q.userGroup()));
            uid2item.put(q.itemUid(), q);
        }
        updateButtonStates();
        autoResizeColumns();
    }


    private void autoResizeColumns() {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (TableColumn<Row,?> col : table.getColumns()) {

            Text tmp = new Text(col.getText());
            double max = tmp.getLayoutBounds().getWidth();

            for (int i = 0; i < rows.size(); i++) {
                Object cell = col.getCellData(i);
                if (cell != null) {
                    tmp = new Text(cell.toString());
                    double w = tmp.getLayoutBounds().getWidth();
                    if (w > max) max = w;
                }
            }
            col.setPrefWidth(max + 14);
        }
    }

    private void applyFocus(Collection<String> uids) {
        if (uids.isEmpty()) return;

        var sm = table.getSelectionModel();
        var fm = table.getFocusModel();

        // Suppress intermediate listener notifications during clear+select
        ignoreSticky = true;
        sm.clearSelection();

        int first = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (uids.contains(rows.get(i).uid())) {
                sm.select(i); fm.focus(i);
                if (first == -1) first = i;
            }
        }
        ignoreSticky = false;

        if (first != -1) {
            final int scrollIdx = first;
            table.scrollTo(scrollIdx);
            // Defer focus to next pulse for reliable visual highlight
            Platform.runLater(() -> table.requestFocus());
        }
    }
    private void refreshLater(Collection<String> focus) {
        // Invalidate any pending background Platform.runLater blocks
        refreshSeq.incrementAndGet();
        StatusResponse st = StatusBus.latest().get();
        if (st == null) { updateButtonStates(); return; }
        try {
            QueueGetPayload qp = svc.queueGetTyped();
            applyQueueData(qp, focus, st);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Queue refresh (explicit) failed: " + ex.getMessage());
        }
    }


    private void hookButtons() {
        upBtn      .setOnAction(e -> moveRelative(-1));
        downBtn    .setOnAction(e -> moveRelative(+1));
        topBtn     .setOnAction(e -> moveAbsolute(0));
        bottomBtn  .setOnAction(e -> moveAbsolute(rows.size()-1));
        deleteBtn  .setOnAction(e -> deleteSelected());
        duplicateBtn.setOnAction(e -> duplicateSelected());
        clearBtn   .setOnAction(e -> clearQueue());
        deselectBtn.setOnAction(e -> table.getSelectionModel().clearSelection());

        loopBtn.selectedProperty().addListener((o,ov,nv) -> setLoopMode(nv));

        table.getSelectionModel().getSelectedIndices()
                .addListener((ListChangeListener<? super Integer>) c -> updateButtonStates());
    }

    private void sendMove(List<String> sel, String refUid, boolean before) throws Exception {
        if (sel.size() == 1)  svc.moveSingle(sel.get(0), refUid, before);
        else                  svc.moveBatch (sel        , refUid, before);
    }
    private void moveRelative(int delta) {
        var selRows = table.getSelectionModel().getSelectedIndices();
        if (selRows.isEmpty()) return;

        int first = selRows.get(0), last = selRows.get(selRows.size()-1);
        int ref   = (delta<0)? first+delta : last+delta;
        if (ref<0 || ref>=rows.size()) return;

        List<String> focus = selectedUids();
        try { sendMove(focus, rows.get(ref).uid(), delta<0); refreshLater(focus); }
        catch (Exception ex) { logger.log(Level.WARNING, "Move failed: "+ex.getMessage()); }
    }
    private void moveAbsolute(int targetRow) {
        if (rows.isEmpty()) return;
        targetRow = Math.max(0, Math.min(targetRow, rows.size()-1));

        var selRows = table.getSelectionModel().getSelectedIndices();
        if (selRows.isEmpty()) return;

        boolean before = targetRow < selRows.get(0);
        List<String> focus = selectedUids();
        try { sendMove(focus, rows.get(targetRow).uid(), before); refreshLater(focus); }
        catch (Exception ex) { logger.log(Level.WARNING, "Move-abs failed: "+ex.getMessage()); }
    }

    private void deleteSelected() {
        var selRows = table.getSelectionModel().getSelectedIndices();
        if (selRows.isEmpty()) return;

        int first = selRows.get(0), last = selRows.get(selRows.size()-1);
        String nextFocus = (last+1 < rows.size()) ? rows.get(last+1).uid()
                : (first>0 ? rows.get(first-1).uid() : null);
        try {
            svc.queueItemRemoveBatch(Map.of("uids", selectedUids()));
            refreshLater(nextFocus==null? List.of() : List.of(nextFocus));
        } catch (Exception ex) { logger.log(Level.WARNING, "Delete failed: "+ex.getMessage()); }
    }

    private void duplicateSelected() {
        var selIdx = table.getSelectionModel().getSelectedIndices();
        if (selIdx.isEmpty()) return;

        // keep copy of the queue before duplication
        Set<String> before = new HashSet<>(uid2item.keySet());

        // duplicate – top→bottom so indices don’t shift
        selIdx.stream().sorted().forEach(idx -> {
            QueueItem orig = uid2item.get(rows.get(idx).uid());
            if (orig == null) return;
            try { svc.addAfter(orig, orig.itemUid()); }      // server returns nothing we need
            catch (Exception ex) {                           // log but continue
                logger.log(Level.WARNING, "Duplicate RPC failed: "+ex.getMessage());
            }
        });

        try {
            // Invalidate pending background refreshes so they don't overwrite our selection
            refreshSeq.incrementAndGet();

            QueueGetPayload qp = svc.queueGetTyped();
            List<String> afterList = qp.queue().stream()
                    .map(QueueItem::itemUid).toList();
            List<String> added = afterList.stream()
                    .filter(uid -> !before.contains(uid))
                    .toList();

            ignoreSticky = true;
            rebuildRows(qp.queue());
            ignoreSticky = false;

            if (!added.isEmpty()) {
                applyFocus(added);
                stickySel = added;
                notifySelectionChange();
            }

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Refresh after duplicate failed: "+ex.getMessage());
        }
    }

    private void clearQueue() { try { svc.queueClear(); }
    catch (Exception ex) { logger.log(Level.WARNING, "Clear failed: "+ex.getMessage()); } }
    private void setLoopMode(boolean loop){ try { svc.queueModeSet(Map.of("loop",loop)); }
    catch (Exception ex){ logger.log(Level.WARNING, "Loop-set failed: "+ex.getMessage()); } }

    private void updateButtonStates() {
        StatusResponse status = StatusBus.latest().get();
        boolean connected = status != null;

        var sel = table.getSelectionModel().getSelectedIndices();
        boolean hasSel = !table.getSelectionModel().getSelectedIndices().isEmpty();
        boolean atTop = hasSel && table.getSelectionModel().getSelectedIndices().get(0) == 0;
        boolean atBot = hasSel && table.getSelectionModel().getSelectedIndices().size() - 1== rows.size() - 1;

        if (viewOnly) {
            upBtn.setDisable(true);
            downBtn.setDisable(true);
            topBtn.setDisable(true);
            bottomBtn.setDisable(true);
            deleteBtn.setDisable(true);
            duplicateBtn.setDisable(true);
            clearBtn.setDisable(true);
            loopBtn.setDisable(true);
            deselectBtn.setDisable(!hasSel);
        } else {
            // Only require server connection (not environment open)
            upBtn.setDisable(!(connected && hasSel && !atTop));
            downBtn.setDisable(!(connected && hasSel && !atBot));
            topBtn.setDisable(upBtn.isDisable());
            bottomBtn.setDisable(downBtn.isDisable());
            deleteBtn.setDisable(!(connected && hasSel));
            duplicateBtn.setDisable(deleteBtn.isDisable());
            clearBtn.setDisable(!(connected && !rows.isEmpty()));
            loopBtn.setDisable(!connected);
            // Deselect always enabled when there's a selection
            deselectBtn.setDisable(!hasSel);
        }
    }

    private void enableRowDragAndDrop() {
        table.setRowFactory(tv -> {
            TableRow<Row> row = new TableRow<>();
            row.setOnDragDetected(e -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setContent(Map.of(DataFormat.PLAIN_TEXT,""));
                    table.getSelectionModel().select(row.getIndex());
                    e.consume();
                }
            });
            row.setOnDragOver(e -> {
                if (e.getGestureSource()!=row &&
                        e.getDragboard().hasContent(DataFormat.PLAIN_TEXT)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });
            row.setOnDragDropped(e -> {
                moveAbsolute(row.getIndex());
                e.setDropCompleted(true); e.consume();
            });
            return row;
        });
    }

    private List<String> selectedUids() {
        return table.getSelectionModel().getSelectedItems()
                .stream().map(Row::uid).toList();
    }
    private static String firstLetter(String s) {
        return (s == null || s.isBlank()) ? "" : s.substring(0, 1).toUpperCase();
    }
    private String fmtParams(QueueItem q, String itemType, String itemName) {
        // Format args first
        String a = Optional.ofNullable(q.args()).orElse(List.of())
                .stream().map(PythonParameterConverter::toPythonRepr)
                .collect(Collectors.joining(", "));

        // Get the plan/instruction definition to determine parameter order
        Map<String, Object> itemInfo = null;
        if ("plan".equals(itemType)) {
            itemInfo = allowedPlans.get(itemName);
        } else if ("instruction".equals(itemType)) {
            itemInfo = allowedInstructions.get(itemName);
        }

        // Format kwargs in schema order if available
        String k;
        if (itemInfo != null) {
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) itemInfo.get("parameters");
            if (parameters != null) {
                Map<String, Object> kwargs = Optional.ofNullable(q.kwargs()).orElse(Map.of());
                // Format kwargs in the order they appear in the schema
                k = parameters.stream()
                        .map(paramInfo -> (String) paramInfo.get("name"))
                        .filter(kwargs::containsKey)
                        .map(paramName -> paramName + ": " + PythonParameterConverter.toPythonRepr(kwargs.get(paramName)))
                        .collect(Collectors.joining(", "));
            } else {
                // No parameters defined, use default formatting
                k = formatKwargsDefault(q.kwargs());
            }
        } else {
            // Plan/instruction not found in schema, use default formatting
            k = formatKwargsDefault(q.kwargs());
        }

        return Stream.of(a, k).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String formatKwargsDefault(Map<String, Object> kwargs) {
        return Optional.ofNullable(kwargs).orElse(Map.of())
                .entrySet().stream()
                .map(e -> e.getKey() + ": " + PythonParameterConverter.toPythonRepr(e.getValue()))
                .collect(Collectors.joining(", "));
    }
    private record Row(String uid, String itemType, String name,
                       String params, String user, String group) {}


    private void notifySelectionChange() {
        var selectedItems = table.getSelectionModel().getSelectedItems();
        QueueItem selectedItem = null;

        if (selectedItems.size() == 1) {
            Row selectedRow = selectedItems.get(0);
            selectedItem = uid2item.get(selectedRow.uid());
        }

        // Track the last selected UID for insert-after operations
        // For multi-select, use the last item in the batch
        String lastUid = null;
        if (!selectedItems.isEmpty()) {
            Row lastRow = selectedItems.get(selectedItems.size() - 1);
            lastUid = lastRow.uid();
        }

        QueueItemSelectionEvent event = QueueItemSelectionEvent.getInstance();
        event.setLastSelectedUid(lastUid);
        event.notifySelectionChanged(selectedItem);
    }
}