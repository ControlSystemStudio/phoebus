package org.phoebus.applications.queueserver.controller;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.phoebus.applications.queueserver.api.*;
import org.phoebus.applications.queueserver.client.RunEngineService;
import org.phoebus.applications.queueserver.view.PlanEditEvent;
import org.phoebus.applications.queueserver.view.TabSwitchEvent;
import org.phoebus.applications.queueserver.view.ItemUpdateEvent;
import org.phoebus.applications.queueserver.util.PythonParameterConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RePlanEditorController implements Initializable {

    @FXML private Button addBtn;
    @FXML private Button batchBtn;
    @FXML private Button cancelBtn;
    @FXML private TableColumn<ParameterRow, Boolean> chkCol;
    @FXML private ChoiceBox<String> choiceBox;
    @FXML private RadioButton instrRadBtn;
    @FXML private TableColumn<ParameterRow, String> paramCol;
    @FXML private RadioButton planRadBtn;
    @FXML private Button resetBtn;
    @FXML private Button saveBtn;
    @FXML private TableView<ParameterRow> table;
    @FXML private TableColumn<ParameterRow, String> valueCol;

    private final RunEngineService svc = new RunEngineService();
    private static final Logger LOG = Logger.getLogger(RePlanEditorController.class.getName());
    private final ObservableList<ParameterRow> parameterRows = FXCollections.observableArrayList();
    private final Map<String, Map<String, Object>> allowedPlans = new HashMap<>();
    private final Map<String, Map<String, Object>> allowedInstructions = new HashMap<>();
    private QueueItem currentItem;
    private boolean isEditMode = false;
    private String currentUser = "GUI Client";
    private String currentUserGroup = "root";
    private String currentItemSource = "";
    private boolean editorStateValid = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Store original parameter values for reset functionality
    private final Map<String, Object> originalParameterValues = new HashMap<>();
    // Python-based parameter converter
    private final PythonParameterConverter pythonConverter = new PythonParameterConverter();

    private class EditableTableCell extends TableCell<ParameterRow, String> {
        private TextField textField;

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                ParameterRow row = getTableRow().getItem();

                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);

                    // Style based on enabled state and validation
                    if (!row.isEnabled()) {
                        setStyle("-fx-text-fill: grey;");
                    } else if (!row.validate(pythonConverter)) {
                        setStyle("-fx-text-fill: red;");
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
        }

        @Override
        public void startEdit() {
            ParameterRow row = getTableRow().getItem();
            if (row != null && row.isEnabled()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getString());
            setGraphic(null);
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            ParameterRow row = getTableRow().getItem();
            if (row != null) {
                row.setValue(newValue);

                // Update cell color based on Python validation
                updateValidationColor(row);

                switchToEditingMode();
                updateButtonStates();
            }
        }

        private void updateValidationColor(ParameterRow row) {
            if (!row.isEnabled()) {
                setStyle("-fx-text-fill: grey;");
            } else if (row.validate(pythonConverter)) {
                setStyle("");
            } else {
                setStyle("-fx-text-fill: red;");
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    commitEdit(textField.getText());
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }

        // Make cell clickable to start editing
        {
            setOnMouseClicked(e -> {
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    ParameterRow row = getTableRow().getItem();
                    if (row.isEnabled() && !isEditing()) {
                        startEdit();
                    }
                }
            });
        }
    }

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
        public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
        public String getValue() { return value.get(); }
        public void setValue(String value) { this.value.set(value); }
        public String getDescription() { return description.get(); }
        public boolean isOptional() { return isOptional.get(); }
        public Object getDefaultValue() { return defaultValue; }

        public boolean validate(PythonParameterConverter converter) {
            if (!enabled.get()) {
                return true;  // Disabled parameters are always valid
            }

            String valueStr = value.get();
            if (valueStr == null || valueStr.trim().isEmpty()) {
                return isOptional.get();
            }

            // Validate using Python converter
            try {
                List<PythonParameterConverter.ParameterInfo> testParams = List.of(
                    new PythonParameterConverter.ParameterInfo(
                        getName(),
                        valueStr,
                        true,
                        isOptional.get(),
                        getDefaultValue()
                    )
                );
                converter.convertParameters(testParams);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public boolean isEditable() {
            // Parameter is editable if it's enabled or if it's required (not optional)
            return enabled.get() || !isOptional.get();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        initializeControls();
        loadAllowedPlansAndInstructions();

        // Listen for edit requests from plan viewer
        PlanEditEvent.getInstance().addListener(this::editItem);
    }

    private void initializeTable() {
        paramCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        chkCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Add tooltips to parameter names
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

        chkCol.setCellFactory(CheckBoxTableCell.forTableColumn(chkCol));
        chkCol.setEditable(true);

        valueCol.setCellFactory(column -> new EditableTableCell());

        valueCol.setOnEditCommit(event -> {
            ParameterRow row = event.getRowValue();
            row.setValue(event.getNewValue());
            switchToEditingMode();
            updateButtonStates();
        });

        // Add listener for checkbox changes to enable/disable editing and trigger edit mode
        chkCol.setOnEditCommit(event -> {
            ParameterRow row = event.getRowValue();
            boolean isChecked = event.getNewValue();
            row.setEnabled(isChecked);

            // If unchecked, reset to default value; if checked and no value, set default
            if (!isChecked) {
                Object defaultValue = row.getDefaultValue();
                row.setValue(defaultValue != null ? String.valueOf(defaultValue) : "");
            } else if (row.getValue().isEmpty() && row.getDefaultValue() != null) {
                row.setValue(String.valueOf(row.getDefaultValue()));
            }

            // Trigger edit mode when checkbox is changed
            switchToEditingMode();

            updateButtonStates();
            autoResizeColumns();
        });

        table.setItems(parameterRows);
        table.setEditable(true);
    }

    private void initializeControls() {
        ToggleGroup typeGroup = new ToggleGroup();
        planRadBtn.setToggleGroup(typeGroup);
        instrRadBtn.setToggleGroup(typeGroup);
        planRadBtn.setSelected(true);

        // Setup ChoiceBox to auto-size to fit content exactly
        setupChoiceBoxAutoSizing();

        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadParametersForSelection(newVal);
                // Set tooltip for choice box with item description
                setChoiceBoxTooltip(newVal);
            }
            updateButtonStates();
            // Resize ChoiceBox to fit new selection
            resizeChoiceBoxToFitContent();
        });

        planRadBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !isEditMode) {
                populateChoiceBox(true);
                updateButtonStates();
            }
        });

        instrRadBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !isEditMode) {
                populateChoiceBox(false);
                updateButtonStates();
            }
        });

        addBtn.setOnAction(e -> addItemToQueue());
        batchBtn.setOnAction(e -> openBatchUpload());
        saveBtn.setOnAction(e -> saveItem());
        resetBtn.setOnAction(e -> resetParametersToDefaults());
        cancelBtn.setOnAction(e -> cancelEdit());

        updateButtonStates();
    }

    private void loadAllowedPlansAndInstructions() {
        new Thread(() -> {
            try {
                // Load plans from API - use raw Map instead of Envelope since response structure doesn't match
                Map<String, Object> responseMap = svc.plansAllowedRaw();

                if (responseMap != null && Boolean.TRUE.equals(responseMap.get("success"))) {
                    allowedPlans.clear();

                    if (responseMap.containsKey("plans_allowed")) {
                        Map<String, Object> plansData = (Map<String, Object>) responseMap.get("plans_allowed");

                        // Convert each plan entry to Map<String, Object>
                        for (Map.Entry<String, Object> entry : plansData.entrySet()) {
                            String planName = entry.getKey();
                            Map<String, Object> planInfo = (Map<String, Object>) entry.getValue();
                            allowedPlans.put(planName, planInfo);
                        }
                    } else {
                        LOG.log(Level.WARNING, "No 'plans_allowed' key in response. Keys: " + responseMap.keySet());
                    }
                } else {
                    LOG.log(Level.WARNING, "Plans response failed. Response: " + responseMap);
                }

                allowedInstructions.clear();
                Map<String, Object> queueStopInstr = new HashMap<>();
                queueStopInstr.put("name", "queue_stop");
                queueStopInstr.put("description", "Stop execution of the queue.");
                queueStopInstr.put("parameters", List.of());
                allowedInstructions.put("queue_stop", queueStopInstr);

                Platform.runLater(() -> {
                    populateChoiceBox(planRadBtn.isSelected());
                });

            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load plans", e);
            }
        }).start();
    }

    private void populateChoiceBox(boolean isPlans) {
        choiceBox.getItems().clear();
        parameterRows.clear();

        if (isPlans) {
            choiceBox.getItems().addAll(allowedPlans.keySet());
        } else {
            choiceBox.getItems().addAll(allowedInstructions.keySet());
        }

        Collections.sort(choiceBox.getItems());
        updateButtonStates();
        // Resize ChoiceBox after populating
        resizeChoiceBoxToFitContent();
    }

    private void setupChoiceBoxAutoSizing() {
        // Set minimum width
        choiceBox.setMinWidth(Region.USE_PREF_SIZE);
        choiceBox.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private void resizeChoiceBoxToFitContent() {
        Platform.runLater(() -> {
            String selectedText = choiceBox.getSelectionModel().getSelectedItem();
            if (selectedText != null && !selectedText.isEmpty()) {
                // Calculate the width needed for the selected text
                Text text = new Text(selectedText);
                // Use default font since ChoiceBox doesn't have getFont()
                double textWidth = text.getLayoutBounds().getWidth();

                // Add padding for dropdown arrow and margins (about 30px)
                double newWidth = textWidth + 30;

                // Set the exact width needed
                choiceBox.setPrefWidth(newWidth);
                choiceBox.setMinWidth(newWidth);
                choiceBox.setMaxWidth(newWidth);
            } else {
                // If nothing selected or empty, make it small (minimum size)
                double minWidth = 80; // Small default size
                choiceBox.setPrefWidth(minWidth);
                choiceBox.setMinWidth(minWidth);
                choiceBox.setMaxWidth(minWidth);
            }
        });
    }

    private void loadParametersForSelection(String selectedName) {
        parameterRows.clear();

        Map<String, Object> itemInfo;
        if (planRadBtn.isSelected()) {
            itemInfo = allowedPlans.get(selectedName);
        } else {
            itemInfo = allowedInstructions.get(selectedName);
        }

        if (itemInfo == null) return;

        List<Map<String, Object>> parameters = (List<Map<String, Object>>) itemInfo.get("parameters");
        if (parameters == null) parameters = new ArrayList<>();

        Map<String, Object> currentKwargs = new HashMap<>();
        if (isEditMode && currentItem != null) {
            currentKwargs = currentItem.kwargs() != null ? currentItem.kwargs() : new HashMap<>();
            // Store original values for reset functionality
            originalParameterValues.clear();
            originalParameterValues.putAll(currentKwargs);
        }

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
            boolean isEnabled = !isOptional;

            if (currentKwargs.containsKey(paramName)) {
                Object value = currentKwargs.get(paramName);
                currentValue = value != null ? String.valueOf(value) : "";
                isEnabled = true;
            } else if (defaultValue != null) {
                currentValue = String.valueOf(defaultValue);
            }

            ParameterRow row = new ParameterRow(paramName, isEnabled, currentValue, description, isOptional, defaultValue);
            parameterRows.add(row);
        }

        if (isEditMode && currentItem != null && currentItem.result() != null) {
            addMetadataAndResults(currentItem);
        }

        updateButtonStates();
        autoResizeColumns();
    }

    private void addMetadataAndResults(QueueItem item) {
        if (item.result() == null) {
            return;
        }

        Map<String, Object> result = item.result();
        
        // Add a separator row for metadata section
        if (!result.isEmpty()) {
            ParameterRow separator = new ParameterRow("--- Metadata & Results ---", false, "", 
                "Execution metadata and results (read-only)", false, null);
            parameterRows.add(separator);
        }

        // Add metadata fields as read-only rows
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String displayValue = formatResultValue(value);
            String description = "Result field: " + key + " (read-only)";
            
            ParameterRow row = new ParameterRow(key, false, displayValue, description, false, null);
            parameterRows.add(row);
        }
    }

    private String formatResultValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            return "Map (" + map.size() + " entries)";
        }
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            return "List (" + list.size() + " items)";
        }
        
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > 100) {
                return str.substring(0, 97) + "...";
            }
            return str;
        }
        
        return String.valueOf(value);
    }

    private void autoResizeColumns() {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (TableColumn<ParameterRow,?> col : table.getColumns()) {

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

    private boolean areParametersValid() {
        boolean allValid = true;
        for (ParameterRow row : parameterRows) {
            boolean rowValid = row.validate(pythonConverter);
            if (!rowValid) {
                allValid = false;
            }
            // Update visual validation state
            updateRowValidationStyle(row, rowValid);
        }
        return allValid;
    }

    private void updateRowValidationStyle(ParameterRow row, boolean isValid) {
        // This would need to be called to update the cell styling
        // For now, we'll handle this in the cell factory
        Platform.runLater(() -> {
            table.refresh();
        });
    }

    private void updateButtonStates() {
        boolean hasSelectedPlan = choiceBox.getSelectionModel().getSelectedItem() != null;
        boolean isValid = areParametersValid();
        boolean isConnected = true; // Assume connected for now - could check StatusBus later
        this.editorStateValid = isValid;

        planRadBtn.setDisable(isEditMode);
        instrRadBtn.setDisable(isEditMode);
        choiceBox.setDisable(isEditMode);

        addBtn.setDisable(!hasSelectedPlan || !isValid || !isConnected);

        saveBtn.setDisable(!isEditMode || !hasSelectedPlan || !isValid || !isConnected ||
                !"QUEUE ITEM".equals(currentItemSource));

        batchBtn.setDisable(!isConnected);

        resetBtn.setDisable(!isEditMode);
        cancelBtn.setDisable(!isEditMode);
    }

    /**
     * Build kwargs map using Python-based type conversion.
     * All type conversion is handled by Python script using ast.literal_eval.
     */
    private Map<String, Object> buildKwargsWithPython() {
        List<PythonParameterConverter.ParameterInfo> paramInfos = new ArrayList<>();

        for (ParameterRow row : parameterRows) {
            PythonParameterConverter.ParameterInfo paramInfo =
                    new PythonParameterConverter.ParameterInfo(
                            row.getName(),
                            row.getValue(),
                            row.isEnabled(),
                            row.isOptional(),
                            row.getDefaultValue()
                    );
            paramInfos.add(paramInfo);
        }

        // Use Python to convert parameters - no Java fallback
        return pythonConverter.convertParameters(paramInfos);
    }

    private void addItemToQueue() {
        if (!areParametersValid()) {
            showValidationError("Some parameters have invalid values. Please check the red fields.");
            return;
        }

        try {
            String selectedName = choiceBox.getSelectionModel().getSelectedItem();
            if (selectedName == null) {
                return;
            }

            String itemType = planRadBtn.isSelected() ? "plan" : "instruction";

            // Use Python-based parameter conversion
            Map<String, Object> kwargs = buildKwargsWithPython();

            QueueItem item = new QueueItem(
                    itemType,
                    selectedName,
                    List.of(),
                    kwargs,
                    null,
                    currentUser,
                    currentUserGroup,
                    null
            );

            QueueItemAdd request = new QueueItemAdd(
                    new QueueItemAdd.Item(item.itemType(), item.name(), item.args(), item.kwargs()),
                    currentUser,
                    currentUserGroup
            );

            new Thread(() -> {
                try {
                    var response = svc.queueItemAdd(request);
                    Platform.runLater(() -> {
                        if (response.success()) {
                            // Clear parameters but preserve radio button selection
                            parameterRows.clear();
                            choiceBox.getSelectionModel().clearSelection();
                            // Don't reset radio button - keep current selection
                            populateChoiceBox(planRadBtn.isSelected());
                            // Switch to view tab
                            TabSwitchEvent.getInstance().switchToTab("Plan Viewer");
                            exitEditMode();
                            showItemPreview();
                        } else {
                            showValidationError("Failed to add item to queue: " + response.msg());
                        }
                    });
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to add item to queue", e);
                    Platform.runLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = e.getClass().getSimpleName();
                        }
                        showValidationError("Failed to add item to queue: " + errorMsg);
                    });
                }
            }).start();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to add item to queue", e);
            Platform.runLater(() -> {
                showValidationError("Failed to add item: " + e.getMessage());
            });
        }
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void saveItem() {
        if (!isEditMode || currentItem == null) {
            return;
        }

        if (!areParametersValid()) {
            showValidationError("Some parameters have invalid values. Please check the red fields.");
            return;
        }

        try {
            String selectedName = choiceBox.getSelectionModel().getSelectedItem();
            if (selectedName == null) {
                return;
            }

            String itemType = planRadBtn.isSelected() ? "plan" : "instruction";

            // Use Python-based parameter conversion
            Map<String, Object> kwargs = buildKwargsWithPython();

            QueueItem updatedItem = new QueueItem(
                    itemType,
                    selectedName,
                    List.of(),
                    kwargs,
                    currentItem.itemUid(),
                    currentItem.user(),
                    currentItem.userGroup(),
                    currentItem.result()
            );

            new Thread(() -> {
                try {
                    var response = svc.queueItemUpdate(updatedItem);
                    Platform.runLater(() -> {
                        if (response.success()) {
                            // Notify viewer that the item was updated
                            ItemUpdateEvent.getInstance().notifyItemUpdated(updatedItem);

                            // Clear parameters but preserve radio button selection
                            parameterRows.clear();
                            choiceBox.getSelectionModel().clearSelection();
                            // Switch to view tab
                            TabSwitchEvent.getInstance().switchToTab("Plan Viewer");
                            exitEditMode();
                            showItemPreview();
                        } else {
                            showValidationError("Failed to save item: " + response.msg());
                        }
                    });
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to save item", e);
                    Platform.runLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = e.getClass().getSimpleName();
                        }
                        showValidationError("Failed to save item: " + errorMsg);
                    });
                }
            }).start();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to save item", e);
            Platform.runLater(() -> {
                showValidationError("Failed to save item: " + e.getMessage());
            });
        }
    }

    private void resetForm() {
        parameterRows.clear();
        choiceBox.getSelectionModel().clearSelection();
        planRadBtn.setSelected(true);
        populateChoiceBox(true);
    }

    private void resetParametersToDefaults() {
        if (isEditMode && currentItem != null) {
            // Reset to original values from when editing started
            for (ParameterRow row : parameterRows) {
                String paramName = row.getName();
                if (originalParameterValues.containsKey(paramName)) {
                    // Restore original value
                    Object originalValue = originalParameterValues.get(paramName);
                    row.setValue(originalValue != null ? String.valueOf(originalValue) : "");
                    row.setEnabled(true);
                } else {
                    // Parameter was not in original item, reset to default
                    Object defaultValue = row.getDefaultValue();
                    if (defaultValue != null) {
                        row.setValue(String.valueOf(defaultValue));
                    } else {
                        row.setValue("");
                    }
                    row.setEnabled(!row.isOptional());
                }
            }
        } else {
            // Reset to default values for new items
            for (ParameterRow row : parameterRows) {
                Object defaultValue = row.getDefaultValue();
                if (defaultValue != null) {
                    row.setValue(String.valueOf(defaultValue));
                } else {
                    row.setValue("");
                }
                // Reset enabled state based on whether parameter is optional
                row.setEnabled(!row.isOptional());
            }
        }
        // Keep edit mode active after reset - don't exit edit mode
        updateButtonStates();
        autoResizeColumns();
    }

    private void cancelEdit() {
        if (isEditMode) {
            // Reset to original state and exit edit mode
            parameterRows.clear();
            isEditMode = false;
            currentItem = null;
            currentItemSource = "";
            originalParameterValues.clear();
            choiceBox.getSelectionModel().clearSelection();
            planRadBtn.setSelected(true);
            populateChoiceBox(true);
            updateButtonStates();
            showItemPreview();
        } else {
            resetForm();
        }
    }

    public void editItem(QueueItem item) {
        currentItem = item;
        currentItemSource = "QUEUE ITEM";
        isEditMode = true;

        if ("plan".equals(item.itemType())) {
            planRadBtn.setSelected(true);
            populateChoiceBox(true);
        } else {
            instrRadBtn.setSelected(true);
            populateChoiceBox(false);
        }

        choiceBox.getSelectionModel().select(item.name());

        // Explicitly load parameters for the selected item since we're in edit mode
        loadParametersForSelection(item.name());

        // Update button states instead of visibility
        updateButtonStates();
    }

    private void switchToEditingMode() {
        if (!isEditMode) {
            isEditMode = true;
            currentItemSource = "NEW ITEM";
            updateButtonStates();
        }
    }

    private void exitEditMode() {
        isEditMode = false;
        currentItem = null;
        currentItemSource = "";
        originalParameterValues.clear();
        // Update button states instead of visibility
        updateButtonStates();
        resetForm();
    }

    private void openBatchUpload() {
        BatchUploadDialog dialog = new BatchUploadDialog(table.getScene().getWindow());
        Optional<BatchUploadDialog.Result> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            BatchUploadDialog.Result uploadResult = result.get();
            processBatchFile(uploadResult.getFilePath(), uploadResult.getFileType());
        }
    }

    private void processBatchFile(String filePath, String fileType) {
        new Thread(() -> {
            try {
                List<QueueItem> items = new ArrayList<>();
                java.io.File file = new java.io.File(filePath);

                if ("csv".equals(fileType) || filePath.toLowerCase().endsWith(".csv")) {
                    items = parseCSVFile(file);
                } else if ("xls".equals(fileType) || filePath.toLowerCase().endsWith(".xls")) {
                    items = parseExcelFile(file);
                }

                if (!items.isEmpty()) {
                    final int itemCount = items.size();
                    QueueItemAddBatch batchRequest = new QueueItemAddBatch(items, currentUser, currentUserGroup);
                    var response = svc.queueItemAddBatch(batchRequest);

                    Platform.runLater(() -> {
                        if (response.success()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Batch Upload Success");
                            alert.setHeaderText(null);
                            alert.setContentText("Successfully added " + itemCount + " items to queue.");
                            alert.showAndWait();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Batch Upload Failed");
                            alert.setHeaderText(null);
                            alert.setContentText("Failed to add items to queue: " + response.msg());
                            alert.showAndWait();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No Items Found");
                        alert.setHeaderText(null);
                        alert.setContentText("No valid items found in the file.");
                        alert.showAndWait();
                    });
                }

            } catch (Exception e) {
                LOG.log(Level.WARNING, "Batch file processing error", e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to process batch file: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private List<QueueItem> parseCSVFile(java.io.File file) throws Exception {
        List<QueueItem> items = new ArrayList<>();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line = reader.readLine();
            if (line == null) return items;

            // Parse header
            String[] headers = line.split(",");

            // Read data rows
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String itemType = values[0].trim();
                    String planName = values[1].trim();

                    Map<String, Object> kwargs = new HashMap<>();

                    // Parse additional parameters
                    for (int i = 2; i < Math.min(values.length, headers.length); i++) {
                        String paramName = headers[i].trim();
                        String paramValue = values[i].trim();

                        if (!paramValue.isEmpty()) {
                            try {
                                // Try to parse as number or boolean
                                if (paramValue.equals("true") || paramValue.equals("false")) {
                                    kwargs.put(paramName, Boolean.parseBoolean(paramValue));
                                } else if (paramValue.matches("-?\\d+")) {
                                    kwargs.put(paramName, Integer.parseInt(paramValue));
                                } else if (paramValue.matches("-?\\d+\\.\\d+")) {
                                    kwargs.put(paramName, Double.parseDouble(paramValue));
                                } else {
                                    kwargs.put(paramName, paramValue);
                                }
                            } catch (Exception e) {
                                kwargs.put(paramName, paramValue);
                            }
                        }
                    }

                    QueueItem item = new QueueItem(
                            itemType.isEmpty() ? "plan" : itemType,
                            planName,
                            List.of(),
                            kwargs,
                            null,
                            currentUser,
                            currentUserGroup,
                            null
                    );
                    items.add(item);
                }
            }
        }

        return items;
    }

    private List<QueueItem> parseExcelFile(java.io.File file) throws Exception {
        List<QueueItem> items = new ArrayList<>();
        
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
             Workbook workbook = new HSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Use first sheet
            
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return items;
            }
            
            // Parse header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return items;
            }
            
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String header = getCellValueAsString(cell);
                headers.add(header != null ? header.trim() : "");
            }
            
            // Parse data rows
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                // Skip empty rows
                boolean hasData = false;
                for (int i = 0; i < Math.min(2, headers.size()); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && getCellValueAsString(cell) != null && !getCellValueAsString(cell).trim().isEmpty()) {
                        hasData = true;
                        break;
                    }
                }
                if (!hasData) continue;
                
                String itemType = "";
                String planName = "";
                
                if (headers.size() >= 1) {
                    Cell cell = row.getCell(0);
                    itemType = getCellValueAsString(cell);
                    itemType = itemType != null ? itemType.trim() : "";
                }
                
                if (headers.size() >= 2) {
                    Cell cell = row.getCell(1);
                    planName = getCellValueAsString(cell);
                    planName = planName != null ? planName.trim() : "";
                }
                
                if (planName.isEmpty()) continue;
                
                Map<String, Object> kwargs = new HashMap<>();
                
                // Parse additional parameters
                for (int i = 2; i < Math.min(headers.size(), row.getLastCellNum()); i++) {
                    String paramName = headers.get(i).trim();
                    if (paramName.isEmpty()) continue;
                    
                    Cell cell = row.getCell(i);
                    Object paramValue = getCellValueAsObject(cell);
                    
                    if (paramValue != null) {
                        kwargs.put(paramName, paramValue);
                    }
                }
                
                QueueItem item = new QueueItem(
                        itemType.isEmpty() ? "plan" : itemType,
                        planName,
                        List.of(),
                        kwargs,
                        null,
                        currentUser,
                        currentUserGroup,
                        null
                );
                items.add(item);
            }
        }
        
        return items;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        double numValue = cell.getNumericCellValue();
                        if (numValue == Math.floor(numValue)) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    } catch (Exception e2) {
                        return null;
                    }
                }
            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }
    
    private Object getCellValueAsObject(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                String strValue = cell.getStringCellValue().trim();
                if (strValue.isEmpty()) return null;
                
                // Try to parse as boolean
                if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                    return Boolean.parseBoolean(strValue);
                }
                
                // Try to parse as number
                try {
                    if (strValue.contains(".")) {
                        return Double.parseDouble(strValue);
                    } else {
                        return Long.parseLong(strValue);
                    }
                } catch (NumberFormatException e) {
                    return strValue;
                }
                
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return (long) numValue;
                    } else {
                        return numValue;
                    }
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    // Handle formula cells by trying to get the cached result
                    CellType cachedType = cell.getCachedFormulaResultType();
                    switch (cachedType) {
                        case STRING:
                            return cell.getStringCellValue();
                        case NUMERIC:
                            double numValue = cell.getNumericCellValue();
                            if (numValue == Math.floor(numValue)) {
                                return (long) numValue;
                            } else {
                                return numValue;
                            }
                        case BOOLEAN:
                            return cell.getBooleanCellValue();
                        default:
                            return null;
                    }
                } catch (Exception e) {
                    return null;
                }
            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }

    private void showItemPreview() {
        String selectedItem = choiceBox.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            loadParametersForSelection(selectedItem);
        }
    }

    private void setChoiceBoxTooltip(String itemName) {
        Map<String, Object> itemInfo;
        if (planRadBtn.isSelected()) {
            itemInfo = allowedPlans.get(itemName);
        } else {
            itemInfo = allowedInstructions.get(itemName);
        }

        if (itemInfo != null) {
            String description = (String) itemInfo.get("description");
            if (description != null && !description.isEmpty()) {
                Tooltip tooltip = new Tooltip(description);
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(400);
                choiceBox.setTooltip(tooltip);
            } else {
                choiceBox.setTooltip(new Tooltip("Description for '" + itemName + "' was not found..."));
            }
        }
    }

    private static class BatchUploadDialog extends Dialog<BatchUploadDialog.Result> {
        
        public static class Result {
            private final String filePath;
            private final String fileType;
            
            public Result(String filePath, String fileType) {
                this.filePath = filePath;
                this.fileType = fileType;
            }
            
            public String getFilePath() { return filePath; }
            public String getFileType() { return fileType; }
        }
        
        private TextField filePathField;
        private ComboBox<String> fileTypeCombo;
        private Button browseButton;
        private String selectedFilePath;
        
        public BatchUploadDialog(javafx.stage.Window owner) {
            initOwner(owner);
            setTitle("Batch Upload");
            setHeaderText("Load Plans from Spreadsheet");
            
            // Create content
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            
            // File selection
            browseButton = new Button("...");
            browseButton.setOnAction(e -> selectFile());
            
            filePathField = new TextField();
            filePathField.setEditable(false);
            filePathField.setPrefWidth(300);
            
            // File type selection
            fileTypeCombo = new ComboBox<>();
            fileTypeCombo.getItems().addAll("xls", "csv");
            fileTypeCombo.setValue("xls");
            
            grid.add(browseButton, 0, 0);
            grid.add(filePathField, 1, 0);
            grid.add(new Label("Spreadsheet Type:"), 0, 1);
            grid.add(fileTypeCombo, 1, 1);
            
            getDialogPane().setContent(grid);
            
            // Add buttons
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Initially disable OK button
            getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            
            // Enable OK button when file is selected
            filePathField.textProperty().addListener((obs, oldVal, newVal) -> {
                getDialogPane().lookupButton(ButtonType.OK).setDisable(newVal == null || newVal.trim().isEmpty());
            });
            
            // Result converter
            setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK && selectedFilePath != null) {
                    return new Result(selectedFilePath, fileTypeCombo.getValue());
                }
                return null;
            });
        }
        
        private void selectFile() {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Spreadsheet File");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Excel Legacy Files (*.xls)", "*.xls"),
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            java.io.File file = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedFilePath = file.getAbsolutePath();
                filePathField.setText(selectedFilePath);
                
                // Auto-detect file type based on extension
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".xls")) {
                    fileTypeCombo.setValue("xls");
                } else if (fileName.endsWith(".csv")) {
                    fileTypeCombo.setValue("csv");
                }
            }
        }
    }

}