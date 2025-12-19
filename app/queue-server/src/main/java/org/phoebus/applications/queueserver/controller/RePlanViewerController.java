package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.*;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.util.QueueItemSelectionEvent;
import org.phoebus.applications.queueserver.util.PythonParameterConverter;
import org.phoebus.applications.queueserver.util.StatusBus;
import org.phoebus.applications.queueserver.view.PlanEditEvent;
import org.phoebus.applications.queueserver.view.TabSwitchEvent;
import org.phoebus.applications.queueserver.view.ItemUpdateEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RePlanViewerController implements Initializable {

    @FXML private TableColumn<ParameterRow, String> paramCol;
    @FXML private TableColumn<ParameterRow, Boolean> chkCol;
    @FXML private TableColumn<ParameterRow, String> valueCol;
    @FXML private CheckBox paramChk;
    @FXML private Label planLabel;
    @FXML private TableView<ParameterRow> table;
    @FXML private Button copyBtn, editBtn;

    private final RunEngineService svc = new RunEngineService();
    private static final Logger logger = Logger.getLogger(RePlanViewerController.class.getPackageName());
    private final ObservableList<ParameterRow> parameterRows = FXCollections.observableArrayList();
    private final Map<String, Map<String, Object>> allowedPlans = new HashMap<>();
    private final Map<String, Map<String, Object>> allowedInstructions = new HashMap<>();
    // Python-based parameter converter (lazy-initialized to avoid blocking UI on startup)
    private PythonParameterConverter pythonConverter;

    private PythonParameterConverter getPythonConverter() {
        if (pythonConverter == null) {
            pythonConverter = new PythonParameterConverter();
        }
        return pythonConverter;
    }

    private QueueItem currentQueueItem;
    private String queueItemName = "-";
    private String queueItemType = null;
    private boolean detailedView = true; // Show all parameters by default
    private String currentUser = "GUI Client";
    private String currentUserGroup = "root";

    // Selection change handler - to be called from queue controller
    private Runnable onEditItemRequested;

    public static class ParameterRow {
        private final SimpleStringProperty name;
        private final SimpleBooleanProperty enabled;
        private final SimpleStringProperty value;
        private final SimpleStringProperty description;
        private final SimpleBooleanProperty isOptional;
        private final Object defaultValue;

        public ParameterRow(String name, boolean enabled, String value, String description,
                            boolean isOptional, Object defaultValue) {
            this.name = new SimpleStringProperty(name);
            this.enabled = new SimpleBooleanProperty(enabled);
            this.value = new SimpleStringProperty(value != null ? value : "");
            this.description = new SimpleStringProperty(description != null ? description : "");
            this.isOptional = new SimpleBooleanProperty(isOptional);
            this.defaultValue = defaultValue;
        }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleBooleanProperty enabledProperty() { return enabled; }
        public SimpleStringProperty valueProperty() { return value; }
        public SimpleStringProperty descriptionProperty() { return description; }
        public SimpleBooleanProperty isOptionalProperty() { return isOptional; }

        public String getName() { return name.get(); }
        public boolean isEnabled() { return enabled.get(); }
        public String getValue() { return value.get(); }
        public String getDescription() { return description.get(); }
        public boolean isOptional() { return isOptional.get(); }
        public Object getDefaultValue() { return defaultValue; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        initializeControls();

        // Only load plans if already connected
        if (StatusBus.latest().get() != null) {
            loadAllowedPlansAndInstructions();
        }

        // Listen for status changes to load plans when connected and update widget state
        StatusBus.latest().addListener((o, oldV, newV) -> {
            if (newV != null && allowedPlans.isEmpty()) {
                loadAllowedPlansAndInstructions();
            }
            // Clear the displayed item when disconnected
            if (newV == null) {
                Platform.runLater(() -> showItem(null));
            }
            Platform.runLater(this::updateWidgetState);
        });

        QueueItemSelectionEvent.getInstance().addListener(this::showItem);

        ItemUpdateEvent.getInstance().addListener(this::handleItemUpdate);
    }

    private void initializeTable() {
        paramCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        chkCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        paramCol.setCellFactory(column -> {
            TableCell<ParameterRow, String> cell = new TableCell<ParameterRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        ParameterRow row = getTableRow().getItem();
                        if (row != null && row.getDescription() != null && !row.getDescription().isEmpty()) {
                            Tooltip tooltip = new Tooltip(row.getDescription());
                            tooltip.setWrapText(true);
                            tooltip.setMaxWidth(300);
                            setTooltip(tooltip);
                        }
                    }
                }
            };
            return cell;
        });

        chkCol.setCellFactory(column -> {
            CheckBoxTableCell<ParameterRow, Boolean> cell = new CheckBoxTableCell<>();
            cell.setEditable(false);
            return cell;
        });

        valueCol.setCellFactory(column -> {
            TableCell<ParameterRow, String> cell = new TableCell<ParameterRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        ParameterRow row = getTableRow().getItem();
                        if (!row.isEnabled()) {
                            setStyle("-fx-text-fill: grey;");
                        } else {
                            setStyle("");
                        }

                        // Add tooltip with parameter description
                        if (row.getDescription() != null && !row.getDescription().isEmpty()) {
                            Tooltip tooltip = new Tooltip(row.getDescription());
                            tooltip.setWrapText(true);
                            tooltip.setMaxWidth(300);
                            setTooltip(tooltip);
                        }
                    }
                }
            };
            return cell;
        });

        table.setItems(parameterRows);
        table.setEditable(false); // Viewer is read-only
    }

    private void initializeControls() {
        paramChk.setSelected(detailedView);
        paramChk.selectedProperty().addListener((obs, oldVal, newVal) -> {
            detailedView = newVal;
            if (currentQueueItem != null) {
                showItem(currentQueueItem);
            }
        });

        copyBtn.setOnAction(e -> copyToQueue());

        editBtn.setOnAction(e -> editCurrentItem());

        updateWidgetState();
    }

    private void loadAllowedPlansAndInstructions() {
        new Thread(() -> {
            try {
                Map<String, Object> responseMap = svc.plansAllowedRaw();

                // Prepare data in background thread
                Map<String, Map<String, Object>> newPlans = new HashMap<>();
                if (responseMap != null && Boolean.TRUE.equals(responseMap.get("success"))) {
                    if (responseMap.containsKey("plans_allowed")) {
                        Map<String, Object> plansData = (Map<String, Object>) responseMap.get("plans_allowed");
                        for (Map.Entry<String, Object> entry : plansData.entrySet()) {
                            String planName = entry.getKey();
                            Map<String, Object> planInfo = (Map<String, Object>) entry.getValue();
                            newPlans.put(planName, planInfo);
                        }
                    }
                }

                Map<String, Map<String, Object>> newInstructions = new HashMap<>();
                Map<String, Object> queueStopInstr = new HashMap<>();
                queueStopInstr.put("name", "queue_stop");
                queueStopInstr.put("description", "Stop execution of the queue.");
                queueStopInstr.put("parameters", List.of());
                newInstructions.put("queue_stop", queueStopInstr);

                // Update maps on JavaFX thread to avoid threading issues
                // Don't update UI here - let it update when needed to avoid
                // simultaneous UI updates from multiple controllers during connection
                Platform.runLater(() -> {
                    allowedPlans.clear();
                    allowedPlans.putAll(newPlans);
                    allowedInstructions.clear();
                    allowedInstructions.putAll(newInstructions);
                });

            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load plans", e);
            }
        }).start();
    }

    public void showItem(QueueItem item) {
        currentQueueItem = item;

        String defaultName = "-";
        queueItemName = item != null ? (item.name() != null ? item.name() : defaultName) : defaultName;
        queueItemType = item != null ? item.itemType() : null;

        String displayedItemType = "instruction".equals(queueItemType) ? "Instruction: " : "Plan: ";
        planLabel.setText(displayedItemType + queueItemName);

        setItemDescriptionTooltip();

        updateWidgetState();
        loadParametersForItem(item);
    }

    private void setItemDescriptionTooltip() {
        if (currentQueueItem != null) {
            Map<String, Object> itemInfo;
            if ("plan".equals(queueItemType)) {
                itemInfo = allowedPlans.get(queueItemName);
            } else {
                itemInfo = allowedInstructions.get(queueItemName);
            }

            if (itemInfo != null) {
                String description = (String) itemInfo.get("description");
                if (description != null && !description.isEmpty()) {
                    Tooltip tooltip = new Tooltip(description);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(400);
                    planLabel.setTooltip(tooltip);
                } else {
                    planLabel.setTooltip(new Tooltip("Description for '" + queueItemName + "' was not found..."));
                }
            }
        } else {
            planLabel.setTooltip(null);
        }
    }

    private void loadParametersForItem(QueueItem item) {
        parameterRows.clear();

        if (item == null) {
            return;
        }

        Map<String, Object> itemInfo;
        if ("plan".equals(item.itemType())) {
            itemInfo = allowedPlans.get(item.name());
        } else {
            itemInfo = allowedInstructions.get(item.name());
        }

        if (itemInfo == null) return;

        List<Map<String, Object>> parameters = (List<Map<String, Object>>) itemInfo.get("parameters");
        if (parameters == null) parameters = new ArrayList<>();

        Map<String, Object> itemKwargs = item.kwargs() != null ? item.kwargs() : new HashMap<>();

        for (Map<String, Object> paramInfo : parameters) {
            String paramName = (String) paramInfo.get("name");
            String description = (String) paramInfo.get("description");
            if (description == null || description.isEmpty()) {
                description = "Description for parameter '" + paramName + "' was not found...";
            }
            Object defaultValue = paramInfo.get("default");
            boolean isOptional = defaultValue != null || "VAR_POSITIONAL".equals(paramInfo.get("kind")) ||
                    "VAR_KEYWORD".equals(paramInfo.get("kind"));

            String currentValue = "";
            boolean isEnabled = false;

            if (itemKwargs.containsKey(paramName)) {
                Object value = itemKwargs.get(paramName);
                currentValue = value != null ? PythonParameterConverter.toPythonRepr(value) : "";
                isEnabled = true;
            } else if (defaultValue != null) {
                // Use normalizeAndRepr for defaults - they might be strings that need parsing
                currentValue = getPythonConverter().normalizeAndRepr(defaultValue);
                isEnabled = false;
            }

            boolean shouldShow = detailedView || isEnabled;

            if (shouldShow) {
                ParameterRow row = new ParameterRow(paramName, isEnabled, currentValue, description, isOptional, defaultValue);
                parameterRows.add(row);
            }
        }

        addMetadataAndResults(item);

        autoResizeColumns();
    }

    private void addMetadataAndResults(QueueItem item) {
        if (item.result() == null) {
            return;
        }

        Map<String, Object> result = item.result();
        
        if (!result.isEmpty()) {
            ParameterRow separator = new ParameterRow("--- Metadata & Results ---", false, "", 
                "Execution metadata and results", false, null);
            parameterRows.add(separator);
        }

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String displayValue = formatResultValue(value);
            String description = "Result field: " + key;
            
            ParameterRow row = new ParameterRow(key, true, displayValue, description, false, null);
            parameterRows.add(row);
        }
    }

    private String formatResultValue(Object value) {
        if (value == null) {
            return "None";
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            // For small maps, show the full Python repr
            if (map.size() <= 3) {
                return PythonParameterConverter.toPythonRepr(value);
            }
            return "Map (" + map.size() + " entries)";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            // For small lists, show the full Python repr
            if (list.size() <= 5) {
                return PythonParameterConverter.toPythonRepr(value);
            }
            return "List (" + list.size() + " items)";
        }

        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 100) {
                return "'" + str.substring(0, 97) + "...'";
            }
            return PythonParameterConverter.toPythonRepr(value);
        }

        return PythonParameterConverter.toPythonRepr(value);
    }

    private void autoResizeColumns() {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (TableColumn<ParameterRow, ?> col : table.getColumns()) {
            Text tmp = new Text(col.getText());
            double max = tmp.getLayoutBounds().getWidth();

            for (int i = 0; i < parameterRows.size(); i++) {
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

    private void updateWidgetState() {
        StatusResponse status = StatusBus.latest().get();
        boolean isConnected = status != null;
        boolean envOpen = isConnected && status.workerEnvironmentExists();

        boolean isItemAllowed = false;
        if (queueItemType != null && queueItemName != null && !"-".equals(queueItemName)) {
            if ("plan".equals(queueItemType)) {
                isItemAllowed = allowedPlans.get(queueItemName) != null;
            } else if ("instruction".equals(queueItemType)) {
                isItemAllowed = allowedInstructions.get(queueItemName) != null;
            }
        }

        // Disable all controls when environment is closed
        table.setDisable(!envOpen);
        paramChk.setDisable(!envOpen);
        copyBtn.setDisable(!envOpen || !isItemAllowed);
        editBtn.setDisable(!envOpen || !isItemAllowed);
    }

    private void copyToQueue() {
        if (currentQueueItem == null) {
            return;
        }

        try {
            // Create a copy of the current item for adding to queue
            QueueItem itemCopy = new QueueItem(
                    currentQueueItem.itemType(),
                    currentQueueItem.name(),
                    currentQueueItem.args() != null ? currentQueueItem.args() : List.of(),
                    currentQueueItem.kwargs() != null ? currentQueueItem.kwargs() : new HashMap<>(),
                    null, // New item, no UID
                    currentUser,
                    currentUserGroup,
                    null // No result for new item
            );

            QueueItemAdd request = new QueueItemAdd(
                    new QueueItemAdd.Item(itemCopy.itemType(), itemCopy.name(), itemCopy.args(), itemCopy.kwargs()),
                    currentUser,
                    currentUserGroup
            );

            new Thread(() -> {
                try {
                    var response = svc.queueItemAdd(request);
                    Platform.runLater(() -> {
                        if (!response.success()) {
                            logger.log(Level.WARNING, "Copy to queue failed", response.msg());
                        }
                    });
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Copy to queue error", e);
                }
            }).start();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Copy to queue error", e);
        }
    }

    private void editCurrentItem() {
        if (currentQueueItem != null) {
            PlanEditEvent.getInstance().notifyEditRequested(currentQueueItem);
            TabSwitchEvent.getInstance().switchToTab("Plan Editor");
        }
    }

    public void setOnEditItemRequested(Runnable callback) {
        this.onEditItemRequested = callback;
    }

    public QueueItem getCurrentItem() {
        return currentQueueItem;
    }

    private void handleItemUpdate(QueueItem updatedItem) {
        // If this is the item currently being viewed, refresh the display
        if (currentQueueItem != null &&
                updatedItem != null &&
                updatedItem.itemUid() != null &&
                updatedItem.itemUid().equals(currentQueueItem.itemUid())) {

            // Update the current item and refresh the display
            Platform.runLater(() -> showItem(updatedItem));
        }
    }

}