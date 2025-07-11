package org.phoebus.applications.queueserver.controller;

import org.phoebus.applications.queueserver.api.QueueItem;
import org.phoebus.applications.queueserver.api.QueueItemAdd;
import org.phoebus.applications.queueserver.client.RunEngineService;
import javafx.application.Platform;
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

    // Custom editable table cell that works like Python Qt - immediate editing on click
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
                    
                    // Style based on enabled state
                    if (!row.isEnabled()) {
                        setStyle("-fx-text-fill: grey;");
                    } else {
                        setStyle("");
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
                if (!isEditMode) {
                    isEditMode = true;
                }
                updateButtonStates();
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
        
        public Object getParsedValue() {
            if (!enabled.get()) return defaultValue;
            
            String valueStr = value.get();
            if (valueStr == null || valueStr.trim().isEmpty()) {
                return defaultValue;
            }
            
            try {
                if (valueStr.equals("true") || valueStr.equals("false")) {
                    return Boolean.parseBoolean(valueStr);
                }
                if (valueStr.matches("-?\\d+")) {
                    return Integer.parseInt(valueStr);
                }
                if (valueStr.matches("-?\\d+\\.\\d+")) {
                    return Double.parseDouble(valueStr);
                }
                return valueStr;
            } catch (Exception e) {
                return valueStr;
            }
        }
        
        public boolean validate() {
            if (!enabled.get()) {
                return isOptional.get();
            }
            
            String valueStr = value.get();
            if (valueStr == null || valueStr.trim().isEmpty()) {
                return isOptional.get();
            }
            
            try {
                getParsedValue();
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
    }
    
    private void initializeTable() {
        paramCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        chkCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        chkCol.setCellFactory(CheckBoxTableCell.forTableColumn(chkCol));
        chkCol.setEditable(true);
        
        // Custom editable cell that works like Python - click to edit immediately
        valueCol.setCellFactory(column -> new EditableTableCell());
        
        valueCol.setOnEditCommit(event -> {
            ParameterRow row = event.getRowValue();
            row.setValue(event.getNewValue());
            // Trigger edit mode when value is changed (like Python)
            if (!isEditMode) {
                isEditMode = true;
            }
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
            
            // Trigger edit mode when checkbox is changed (like Python code)
            if (!isEditMode) {
                isEditMode = true;
            }
            
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
            }
            updateButtonStates();
            // Resize ChoiceBox to fit new selection
            resizeChoiceBoxToFitContent();
        });
        
        planRadBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                populateChoiceBox(true);
                updateButtonStates();
            }
        });
        
        instrRadBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                populateChoiceBox(false);
                updateButtonStates();
            }
        });
        
        addBtn.setOnAction(e -> addItemToQueue());
        batchBtn.setOnAction(e -> openBatchUpload());
        saveBtn.setOnAction(e -> saveItem());
        resetBtn.setOnAction(e -> resetParametersToDefaults());
        cancelBtn.setOnAction(e -> cancelEdit());
        
        // Don't use setVisible - use setDisable instead like Python code
        
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
                
                // Hardcode instructions like Python code - no instructions_allowed endpoint exists
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
        }
        
        for (Map<String, Object> paramInfo : parameters) {
            String paramName = (String) paramInfo.get("name");
            String description = (String) paramInfo.get("description");
            Object defaultValue = paramInfo.get("default");
            boolean isOptional = defaultValue != null;
            
            String currentValue = "";
            boolean isEnabled = !isOptional;
            if (currentKwargs.containsKey(paramName)) {
                currentValue = String.valueOf(currentKwargs.get(paramName));
                isEnabled = true;
            } else if (defaultValue != null) {
                currentValue = String.valueOf(defaultValue);
            }
            
            ParameterRow row = new ParameterRow(paramName, isEnabled, currentValue, description, isOptional, defaultValue);
            parameterRows.add(row);
        }
        
        updateButtonStates();
        autoResizeColumns();
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
        for (ParameterRow row : parameterRows) {
            if (!row.validate()) {
                return false;
            }
        }
        return true;
    }
    
    private void updateButtonStates() {
        boolean hasSelectedPlan = choiceBox.getSelectionModel().getSelectedItem() != null;
        boolean isValid = areParametersValid();
        boolean isConnected = true; // Assume connected for now - could check StatusBus later
        
        // Add button: enabled when plan selected, valid parameters, connected, and not in edit mode
        addBtn.setDisable(!hasSelectedPlan || !isValid || !isConnected || isEditMode);
        
        // Save button: enabled when in edit mode, plan selected, valid parameters, and connected
        saveBtn.setDisable(!isEditMode || !hasSelectedPlan || !isValid || !isConnected);
        
        // Batch upload button: enabled when connected
        batchBtn.setDisable(!isConnected);
        
        // Reset and Cancel buttons: enabled only in edit mode
        resetBtn.setDisable(!isEditMode);
        cancelBtn.setDisable(!isEditMode);
    }
    
    private void addItemToQueue() {
        if (!areParametersValid()) {
            return;
        }
        
        try {
            String selectedName = choiceBox.getSelectionModel().getSelectedItem();
            if (selectedName == null) {
                return;
            }
            
            String itemType = planRadBtn.isSelected() ? "plan" : "instruction";
            Map<String, Object> kwargs = new HashMap<>();
            
            for (ParameterRow row : parameterRows) {
                if (row.isEnabled()) {
                    kwargs.put(row.getName(), row.getParsedValue());
                }
            }
            
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
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveItem() {
        if (!isEditMode || currentItem == null) {
            return;
        }
        
        if (!areParametersValid()) {
            return;
        }
        
        try {
            String selectedName = choiceBox.getSelectionModel().getSelectedItem();
            if (selectedName == null) {
                return;
            }
            
            String itemType = planRadBtn.isSelected() ? "plan" : "instruction";
            Map<String, Object> kwargs = new HashMap<>();
            
            for (ParameterRow row : parameterRows) {
                if (row.isEnabled()) {
                    kwargs.put(row.getName(), row.getParsedValue());
                }
            }
            
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
                            exitEditMode();
                        } else {
                            System.err.println("Save failed: " + response.msg());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Save error: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void resetForm() {
        parameterRows.clear();
        choiceBox.getSelectionModel().clearSelection();
        planRadBtn.setSelected(true);
        populateChoiceBox(true);
    }
    
    private void resetParametersToDefaults() {
        // Reset all parameters to their default values (like Python reset functionality)
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
        // Keep edit mode active after reset - don't exit edit mode
        updateButtonStates();
        autoResizeColumns();
    }
    
    private void cancelEdit() {
        if (isEditMode) {
            exitEditMode();
        } else {
            resetForm();
        }
        updateButtonStates();
    }
    
    public void editItem(QueueItem item) {
        currentItem = item;
        isEditMode = true;
        
        if ("plan".equals(item.itemType())) {
            planRadBtn.setSelected(true);
            populateChoiceBox(true);
        } else {
            instrRadBtn.setSelected(true);
            populateChoiceBox(false);
        }
        
        choiceBox.getSelectionModel().select(item.name());
        
        // Update button states instead of visibility
        updateButtonStates();
    }
    
    private void exitEditMode() {
        isEditMode = false;
        currentItem = null;
        // Update button states instead of visibility
        updateButtonStates();
        resetForm();
    }
    
    private void openBatchUpload() {
        // Basic file chooser for batch upload - matches Python queue_upload_spreadsheet
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Spreadsheet File");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );
        
        java.io.File file = fileChooser.showOpenDialog(table.getScene().getWindow());
        if (file != null) {
            processBatchFile(file);
        }
    }
    
    private void processBatchFile(java.io.File file) {
        // Basic batch processing - simplified version of Python's queue_upload_spreadsheet
        new Thread(() -> {
            try {
                // For now, just show that batch upload was attempted
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Batch Upload");
                    alert.setHeaderText(null);
                    alert.setContentText("Batch upload processing would happen here, similar to Python's queue_upload_spreadsheet method.");
                    alert.showAndWait();
                });
            } catch (Exception e) {
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
    
}