/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class SaveSetController implements NodeChangedListener {


	@FXML
	private TableColumn<ConfigPv, String> pvNameColumn;

	@FXML
	private TableColumn<ConfigPv, String> readbackPvNameColumn;

	@FXML
	private TableColumn<ConfigPv, Boolean> readOnlyColumn;

	@FXML
	private TableView<ConfigPv> pvTable;

	@FXML
	private TextArea commentTextArea;

	@FXML
	private Button saveButton;

	@FXML
	private TextField pvNameField;

	@FXML
	private TextField readbackPvNameField;

	@FXML
	private Button addPvButton;

	@FXML
	private CheckBox readOnlyCheckBox;

	@FXML
	private SimpleStringProperty pvNameProperty = new SimpleStringProperty("");
	@FXML
	private SimpleStringProperty readbackPvNameProperty = new SimpleStringProperty("");

	@FXML
	private SimpleBooleanProperty readOnlyProperty = new SimpleBooleanProperty(false);

	@Autowired
	private SaveAndRestoreService saveAndRestoreService;

	private static Executor UI_EXECUTOR = Platform::runLater;

	private SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);

	private ObservableList<ConfigPv> saveSetEntries = FXCollections.observableArrayList();

	private SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);
	private SimpleBooleanProperty singelSelection = new SimpleBooleanProperty(false);
	private SimpleStringProperty saveSetCommentProperty = new SimpleStringProperty();
	private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();
	private Node loadedConfig;

	private TableView.TableViewSelectionModel<ConfigPv> defaultSelectionModel;

	private static final String DESCRIPTION_PROPERTY = "description";

	private String saveSetName;

	@FXML
	public void initialize() {

		dirty.addListener((observable, oldValue, newValue) -> {
			if(newValue){
				tabTitleProperty.set("* " + saveSetName);
			}
			else{
				tabTitleProperty.set(saveSetName);
			}
		});

		commentTextArea.textProperty().bindBidirectional(saveSetCommentProperty);

		pvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		pvTable.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			selectionEmpty.set(nv == null);
		});

		pvTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<ConfigPv>() {
			@Override
			public void onChanged(Change<? extends ConfigPv> c) {
				singelSelection.set(c.getList().size() == 1);
			}
		});

		defaultSelectionModel = pvTable.getSelectionModel();

		pvTable.setRowFactory( tv -> {
			TableRow<ConfigPv> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (row.isEmpty()) {
					pvTable.setSelectionModel(null);
				}
				else{
					pvTable.setSelectionModel(defaultSelectionModel);
				}
			});
			return row ;
		});

		ContextMenu pvNameContextMenu = new ContextMenu();

		MenuItem deleteMenuItem = new MenuItem(Messages.menuItemDeleteSelectedPVs);
		deleteMenuItem.setOnAction(ae -> {
			ObservableList<ConfigPv> selectedPvs = pvTable.getSelectionModel().getSelectedItems();
			if(selectedPvs == null || selectedPvs.isEmpty()){
				return;
			}
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle(Messages.promptDeletePVTitle);
			alert.setContentText(Messages.promptDeletePVFromSaveSet);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.OK)) {
				UI_EXECUTOR.execute(() -> {
					saveSetEntries.removeAll(selectedPvs);
					pvTable.refresh();
				});
			}
		});

		deleteMenuItem.disableProperty().bind(selectionEmpty);

		pvNameContextMenu.getItems().addAll(deleteMenuItem);

		pvNameColumn.setEditable(true);
		pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));

		pvNameColumn.setCellFactory(new Callback<>() {
			@Override
			public TableCell call(TableColumn param) {
				final TableCell<ConfigPv, String> cell = new TableCell<>() {
					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						selectionEmpty.set(empty);
						if (empty) {
							setText(null);
						} else {
							if (isEditing()) {
								setText(null);
							} else {
								setText(getItem().toString());
								setGraphic(null);
							}
						}
					}
				};
				cell.setContextMenu(pvNameContextMenu);
				return cell;
			}
		});

		pvNameField.textProperty().bindBidirectional(pvNameProperty);
		readbackPvNameField.textProperty().bindBidirectional(readbackPvNameProperty);

		saveSetEntries.addListener(new ListChangeListener<ConfigPv>() {
			@Override
			public void onChanged(Change<? extends ConfigPv> change) {
				while (change.next()) {
					if (change.wasAdded() || change.wasRemoved()) {
						FXCollections.sort(saveSetEntries);
						dirty.setValue(true);
					}
				}
			}
		});

		saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return dirty.not().get() || saveSetCommentProperty.isEmpty().get();},
				dirty, saveSetCommentProperty));

		addPvButton.disableProperty().bind(pvNameField.textProperty().isEmpty());

		readOnlyCheckBox.selectedProperty().bindBidirectional(readOnlyProperty);

		commentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
			if(!newValue.equals(oldValue)) {
				dirty.set(true);
			}
		});
	}

	@FXML
	public void saveSaveSet(ActionEvent event) {

		UI_EXECUTOR.execute(() -> {
			try {
				loadedConfig.putProperty(DESCRIPTION_PROPERTY, saveSetCommentProperty.getValue());
				loadedConfig = saveAndRestoreService.updateSaveSet(loadedConfig, saveSetEntries);
				loadSaveSet(loadedConfig);
			} catch (Exception e1) {
				Alert errorAlert = new Alert(AlertType.ERROR);
				errorAlert.setTitle("Action failed");
				errorAlert.setHeaderText(e1.getMessage());
				errorAlert.showAndWait();
			}
		});
	}

	@FXML
	public void addPv(ActionEvent event){

		UI_EXECUTOR.execute(() -> {
			ConfigPv configPv = ConfigPv.builder()
					.pvName(pvNameProperty.get().trim())
					.readOnly(readOnlyProperty.get())
					.readbackPvName(readbackPvNameProperty.get() == null || readbackPvNameProperty.get().isEmpty() ?  null : readbackPvNameProperty.get().trim())
					.build();
			saveSetEntries.add(configPv);
			resetAddPv();
		});

	}

	private void resetAddPv(){
		pvNameProperty.set("");
		readOnlyProperty.set(false);
		readbackPvNameProperty.set("");

		pvNameField.requestFocus();
	}


	public void loadSaveSet(Node node) {

		UI_EXECUTOR.execute(() -> {
			try {
				List<ConfigPv> configPvs = saveAndRestoreService.getConfigPvs(node.getUniqueId());
				loadedConfig = node;
				Collections.sort(configPvs);
				saveSetName = node.getName();
				tabTitleProperty.set(saveSetName);
				saveSetCommentProperty.set(loadedConfig.getProperty(DESCRIPTION_PROPERTY));
				saveSetEntries.setAll(configPvs);
				pvTable.setItems(saveSetEntries);
				pvTable.setEditable(true);
				dirty.set(false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public boolean handleSaveSetTabClosed(){
		if(dirty.get()){
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Close tab?");
			alert.setContentText("Save set has been modified but is not saved. Do you wish to continue?");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.OK)) {
				return true;
			}
			else{
				return false;
			}
		}
		return true;
	}


	@Override
	public void nodeChanged(Node node){
		if(loadedConfig.getUniqueId().equals(node.getUniqueId())){
			tabTitleProperty.set(node.getName());
		}
	}
}
