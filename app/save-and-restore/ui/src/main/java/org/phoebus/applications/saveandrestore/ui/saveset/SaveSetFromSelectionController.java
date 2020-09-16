/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.filehandler.csv.CSVCommon;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.preferences.PreferencesReader;

import java.net.URL;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Interface with ChannelFinder or {@link ProcessVariable} class in clipboard
 * providing an convenient way to create a saveset with many PVs.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SaveSetFromSelectionController implements Initializable {

    private final SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");
    private final PreferencesReader preferencesReader = (PreferencesReader) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("preferencesReader");

    private final Logger LOGGER = Logger.getLogger(SaveAndRestoreService.class.getName());

    private final String DESCRIPTION_PROPERTY = "description";

    private final SimpleIntegerProperty numSelected = new SimpleIntegerProperty();

    private class TableRowEntry {
        private boolean selected;
        private ConfigPv pv;
    }

    private static final DateTimeFormatter savesetTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final List<String> nodeListInFolder = new ArrayList<>();

    @FXML
    private TextField locationTextField;

    @FXML
    private Button browseButton;

    @FXML
    private TextField saveSetName;

    @FXML
    private TextArea description;

    @FXML
    private Label numSelectedLabel;

    @FXML
    private Label numTotalLabel;

    @FXML
    private TableView<TableRowEntry> pvTable;

    @FXML
    private TableColumn<TableRowEntry, Boolean> selectColumn;

    @FXML
    private TableColumn<TableRowEntry, String> pvNameColumn;

    @FXML
    private TableColumn<TableRowEntry, String> readbackPvName;

    @FXML
    private TableColumn<TableRowEntry, Boolean> readOnlyColumn;

    @FXML
    private Button saveButton;

    @FXML
    private Button discardButton;

    private SimpleObjectProperty<Node> targetNode = new SimpleObjectProperty<>();

    private boolean isDisabledSaveSetSelectionInBrowsing;

    public void disableSaveSetSelectionInBrowsing() {
        isDisabledSaveSetSelectionInBrowsing = true;
    }

    private SimpleObjectProperty<Node> createdSaveset = null;

    public void setCreatedSavesetProperty(SimpleObjectProperty<Node> createdSaveset) {
        this.createdSaveset = createdSaveset;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        targetNode.addListener((observableValue, node, newNode) -> {
            if (newNode != null) {
                try {
                    if (newNode.getNodeType() == NodeType.CONFIGURATION) {
                        saveSetName.setText(newNode.getName());
                        description.setText(newNode.getProperty(DESCRIPTION_PROPERTY));

                        saveSetName.setEditable(false);
                        description.setEditable(true);

                        Node parentNode = saveAndRestoreService.getParentNode(newNode.getUniqueId());
                        locationTextField.setText(DirectoryUtilities.CreateLocationString(parentNode, false));

                        nodeListInFolder.clear();
                        saveAndRestoreService.getChildNodes(parentNode).forEach(item -> nodeListInFolder.add(item.getName()));

                        saveButton.setDisable(false);

                        saveSetName.getStyleClass().remove("input-error");
                        saveSetName.setTooltip(null);
                    } else {
                        saveSetName.setText("");
                        description.setText("");

                        saveSetName.setEditable(true);
                        description.setEditable(true);

                        locationTextField.setText(DirectoryUtilities.CreateLocationString(newNode, false));

                        nodeListInFolder.clear();
                        saveAndRestoreService.getChildNodes(newNode).forEach(item -> nodeListInFolder.add(item.getName()));

                        saveSetName.getStyleClass().remove("input-error");
                        saveSetName.setTooltip(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        targetNode.set(saveAndRestoreService.getChildNodes(saveAndRestoreService.getRootNode()).get(0));

        browseButton.setOnAction(action -> {
            try {
                SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();

                Stage dialog = new Stage();
                dialog.setTitle("Choose a folder, a saveset, or create one");
                dialog.initModality(Modality.APPLICATION_MODAL);
                if (preferencesReader.getBoolean("splitSaveset")) {
                    dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetSelectorWithSplit.fxml")));
                } else {
                    dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetSelector.fxml")));
                }

                final BaseSaveSetSelectionController saveSetSelectionController = springFxmlLoader.getLoader().getController();
                if (isDisabledSaveSetSelectionInBrowsing) {
                    saveSetSelectionController.disableSavesetSelection();
                }

                dialog.showAndWait();

                final Node selectedNode = saveSetSelectionController.getSelectedNode();
                if (selectedNode != null) {
                    targetNode.set(selectedNode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveSetName.setPromptText(savesetTimeFormat.format(Instant.now()));
        saveSetName.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        saveSetName.textProperty().addListener((observableValue, oldName, newName) -> {
            saveButton.setDisable(nodeListInFolder.contains(newName));

            if (saveButton.isDisabled()) {
                saveSetName.getStyleClass().add("input-error");
                saveSetName.setTooltip(new Tooltip(Messages.toolTipSaveSetExists + (!isDisabledSaveSetSelectionInBrowsing ? System.lineSeparator() + Messages.toolTipSaveSetExistsOption : "")));
            } else {
                saveSetName.getStyleClass().remove("input-error");
                saveSetName.setTooltip(null);
            }
        });

        description.setPromptText("Saveset created at " + savesetTimeFormat.format(Instant.now()));

        selectColumn.setReorderable(false);
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setCellValueFactory(cell -> {
            final SimpleBooleanProperty selected = new SimpleBooleanProperty(cell.getValue().selected);
            selected.addListener((observable, oldValue, newValue) -> {
                cell.getValue().selected = newValue;
                numSelected.setValue(numSelected.getValue() + (newValue ? 1 : -1));
            });
            return selected;
        });

        pvNameColumn.setReorderable(false);
        pvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        pvNameColumn.setCellValueFactory(cell -> {
            final SimpleStringProperty pvName = new SimpleStringProperty(cell.getValue().pv.getPvName());
            pvName.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setPvName(newValue));
            return pvName;
        });

        readbackPvName.setReorderable(false);
        readbackPvName.setCellFactory(TextFieldTableCell.forTableColumn());
        readbackPvName.setCellValueFactory(cell -> {
            final SimpleStringProperty readbackPvName = new SimpleStringProperty(cell.getValue().pv.getReadbackPvName());
            readbackPvName.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setReadbackPvName(newValue));
            return readbackPvName;
        });

        readOnlyColumn.setReorderable(false);
        readOnlyColumn.setCellFactory(CheckBoxTableCell.forTableColumn(readOnlyColumn));
        readOnlyColumn.setCellValueFactory(cell -> {
            final SimpleBooleanProperty readOnly = new SimpleBooleanProperty(cell.getValue().pv.isReadOnly());
            readOnly.addListener((observable, oldValue, newValue) -> cell.getValue().pv.setReadOnly(newValue));
            return readOnly;
        });

        discardButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText("No information is saved if discarded.\nDo you want to proceed?");
            Optional<ButtonType> response = alert.showAndWait();
            response.ifPresent(type -> {
                if (type == ButtonType.OK) {
                    ((Stage) discardButton.getScene().getWindow()).close();
                }
            });
        });

        numSelected.addListener((observableValue, number, newValue) -> numSelectedLabel.setText(NumberFormat.getIntegerInstance().format(newValue)));
    }

    public void setSelection(List<ProcessVariable> pvList) {
        for (ProcessVariable pv : pvList) {
            final TableRowEntry rowEntry = new TableRowEntry();
            rowEntry.selected = true;
            rowEntry.pv = ConfigPv.builder()
                    .pvName(pv.getName())
                    .readbackPvName(null)
                    .readOnly(false)
                    .build();
            pvTable.getItems().add(rowEntry);

            numSelected.set(pvList.size());
            numTotalLabel.setText(NumberFormat.getIntegerInstance().format(pvList.size()));
        }
    }

    public void setData(Node targetNode, String name, String description, List<Map<String, String>> entries) {
        if (targetNode != null) {
            this.targetNode.set(targetNode);
        }

        this.saveSetName.setText(name);
        this.description.setText(description);

        for (Map<String, String> entry : entries) {
            final TableRowEntry rowEntry = new TableRowEntry();
            rowEntry.selected = true;
            rowEntry.pv = ConfigPv.builder()
                    .pvName(entry.get(CSVCommon.H_PV_NAME))
                    .readbackPvName(entry.get(CSVCommon.H_READBACK))
                    .readOnly(Boolean.parseBoolean(entry.get(CSVCommon.H_READ_ONLY)) || "1".equals(entry.get(CSVCommon.H_READ_ONLY)))
                    .build();
            pvTable.getItems().add(rowEntry);

            numSelected.set(entries.size());
            numTotalLabel.setText(NumberFormat.getIntegerInstance().format(entries.size()));
        }
    }

    @FXML
    private void save(ActionEvent ae) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Duplicate PVs are removed automatically.\nAre you sure to save?");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() != ButtonType.OK) {
            return;
        }

        List<ConfigPv> pvs = new ArrayList<>();
        for (TableRowEntry item : pvTable.getItems()) {
            if (item.selected && !item.pv.getPvName().isEmpty()) {
                pvs.add(item.pv);
            }
        }

        Node selectedNode = targetNode.get();
        if (selectedNode.getNodeType() == NodeType.FOLDER) {
            Node newSaveSetBuild = Node.builder()
                    .nodeType(NodeType.CONFIGURATION)
                    .name(saveSetName.getText().trim().isEmpty() ? saveSetName.getPromptText() : saveSetName.getText().trim())
                    .build();

            Node parentNode = selectedNode;

            try {
                Node newSaveSet = saveAndRestoreService.createNode(parentNode.getUniqueId(), newSaveSetBuild);

                newSaveSet.putProperty(DESCRIPTION_PROPERTY, (description.getText().trim().isEmpty() ? description.getPromptText() : description.getText().trim()));
                newSaveSet = saveAndRestoreService.updateSaveSet(newSaveSet, pvs);

                if (createdSaveset != null) {
                    createdSaveset.set(newSaveSet);
                }
            } catch (Exception e) {
                String alertMessage = "Cannot save PVs in parent node: " + parentNode.getName() + "(" + parentNode.getUniqueId() + ")";

                alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOGGER.severe(alertMessage);

                e.printStackTrace();
            }
        } else { // NodeType.CONFIGURATION
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setContentText(Messages.alertAddingPVsToSaveset + selectedNode.getName() + System.lineSeparator() + Messages.alertContinue);
            Optional<ButtonType> confirmationResponse = confirmation.showAndWait();

            if (confirmationResponse.isPresent() && confirmationResponse.get() != ButtonType.OK) {
                return;
            }

            Node parentNode = null;
            try {
                parentNode = saveAndRestoreService.getParentNode(selectedNode.getUniqueId());

                List<ConfigPv> storedPvs = saveAndRestoreService.getConfigPvs(selectedNode.getUniqueId());
                Set<ConfigPv> pvSet = new HashSet<ConfigPv>();
                pvSet.addAll(storedPvs);
                pvSet.addAll(pvs);

                selectedNode.removeProperty(DESCRIPTION_PROPERTY);
                selectedNode.putProperty(DESCRIPTION_PROPERTY, (description.getText().trim().isEmpty() ? description.getPromptText() : description.getText().trim()));

                saveAndRestoreService.updateSaveSet(selectedNode, new ArrayList<>(pvSet));
            } catch (Exception e) {
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Cannot save PVs in parent node: " + parentNode.getName() + "(" + parentNode.getUniqueId() + ")");
                alert.show();

                e.printStackTrace();
            }
        }
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
