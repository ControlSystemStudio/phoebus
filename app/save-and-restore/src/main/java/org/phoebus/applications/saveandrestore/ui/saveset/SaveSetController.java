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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class SaveSetController {


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
	private RadioButton ca;

	@FXML
	private RadioButton pva;

	@FXML
	private ToggleGroup providerToggleGroup;

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
	private SaveAndRestoreService service;

	private static Executor UI_EXECUTOR = Platform::runLater;

	private SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);

	private ObservableList<ConfigPv> saveSetEntries = FXCollections.observableArrayList();

	private SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);
	private SimpleBooleanProperty singelSelection = new SimpleBooleanProperty(false);

	private static final String PROVIDER = "provider";

	private Node loadedConfig;

	private TableView.TableViewSelectionModel<ConfigPv> defaultSelectionModel;

	private static final String DESCRIPTION_PROPERTY = "description";

	@FXML
	public void initialize() {


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

		commentTextArea.textProperty().addListener(ce -> {
			dirty.set(true);
		});

		ContextMenu pvNameContextMenu = new ContextMenu();

		MenuItem deleteMenuItem = new MenuItem("Delete selected PV(s)");
		deleteMenuItem.setOnAction(ae -> {
			ObservableList<ConfigPv> selectedPvs = pvTable.getSelectionModel().getSelectedItems();
			if(selectedPvs == null || selectedPvs.isEmpty()){
				return;
			}
			UI_EXECUTOR.execute(() -> {
				saveSetEntries.removeAll(selectedPvs);
					pvTable.refresh();
			});
		});
		deleteMenuItem.disableProperty().bind(selectionEmpty);

		MenuItem renamePvMenuItem = new MenuItem("Edit PV Name");
		renamePvMenuItem.setOnAction(ae -> {

			ConfigPv configPv = pvTable.getSelectionModel().getSelectedItem();

			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Rename PV?");
			alert.setContentText("NOTE: Renaming the \"" + configPv.getPvName() + "\" will affect all save sets and snapshots referring to this PV.\n\nDo you wish to continue?");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.OK)) {

			}
		});
		renamePvMenuItem.disableProperty().bind(singelSelection.not());

		pvNameContextMenu.getItems().addAll(deleteMenuItem, renamePvMenuItem);

		pvNameColumn.setEditable(true);
		pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));
		//pvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		pvNameColumn.setOnEditCommit(
				new EventHandler<TableColumn.CellEditEvent<ConfigPv, String>>() {
					@Override
					public void handle(TableColumn.CellEditEvent<ConfigPv, String> t) {

					}
				}
		);

		pvNameColumn.setContextMenu(pvNameContextMenu);

		pvNameColumn.setCellFactory(new Callback<>() {
			@Override
			public TableCell call(TableColumn param) {

				final TableCell<ConfigPv, String> cell = new TableCell<>() {

//					private TextField textField;
//
//					@Override
//					public void startEdit() {
//						if (!isEmpty()) {
//							super.startEdit();
//							createTextField();
//							//setText(null);
//							setGraphic(textField);
//							textField.selectAll();
//						}
//					}
//
//					@Override
//					public void cancelEdit() {
//						super.cancelEdit();
//
//						setText((String) getItem());
//						setGraphic(null);
//					}

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

//					@Override
//					public void commitEdit(String newValue){
//						Alert alert = new Alert(AlertType.CONFIRMATION);
//						alert.setTitle("Rename PV?");
//						alert.setContentText("NOTE: Renaming the \"" + getItem() + "\" will affect all save sets and snapshots referring to this PV.\n\nDo you wish to continue?");
//						Optional<ButtonType> result = alert.showAndWait();
//						if (result.isPresent() && result.get().equals(ButtonType.OK)) {
//
//						}
//					}
//
//					private void createTextField() {
//						textField = new TextField(getString());
//						textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
//						textField.setOnKeyPressed(keyEvent -> {
//									if (keyEvent.getCode() == KeyCode.ENTER && !textField.getText().isEmpty()) {
//										setItem(textField.getText());
//										commitEdit(getItem());
//									} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
//										cancelEdit();
//									}
//							});
//					}
//
//					private String getString() {
//						return getItem() == null ? "" : getItem().toString();
//					}
				};
				cell.setContextMenu(pvNameContextMenu);
				return cell;
			}
		});

		ContextMenu readbackPvNameContextMenu = new ContextMenu();
		MenuItem renameReadbackPvMenuItem = new MenuItem("Edit read-back PV Name");
		renameReadbackPvMenuItem.setOnAction(ae -> {
			ObservableList<ConfigPv> selectedPvs = pvTable.getSelectionModel().getSelectedItems();
			if(selectedPvs == null || selectedPvs.isEmpty()){
				return;
			}

			// TODO: Launch dialog and call remote service
		});

		readbackPvNameContextMenu.getItems().add(renameReadbackPvMenuItem);
		readbackPvNameColumn.setCellFactory(new Callback<>() {
			@Override
			public TableCell<ConfigPv, String> call(TableColumn<ConfigPv, String> param) {
				final TableCell cell = new TableCell() {

					@Override
					public void updateItem(Object item, boolean empty) {
						if(item == null){
							setText(null);
							return;
						}
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
				cell.setContextMenu(readbackPvNameContextMenu);
				return cell;
			}
		});


		readOnlyColumn.setCellFactory(new Callback<>(){
			@Override
			public TableCell<ConfigPv, Boolean> call(TableColumn<ConfigPv, Boolean> param) {
				final TableCell cell = new TableCell(){
					@Override
					public void updateItem(Object item, boolean empty) {
						super.updateItem(item, empty);
						selectionEmpty.set(empty);
						if (empty) {
							setText(null);
						} else {
							if (isEditing()) {
								setText(null);
							} else {
								setText(((Boolean)item ? "yes" : "no"));
								setGraphic(null);
							}
						}
					}
				};
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
						//pvListUnchanged.set(false);
						dirty.setValue(true);
					}
				}
			}
		});

//		saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
//			return (commentUnchanged.get() && pvListUnchanged.get()) || commentTextProperty.isEmpty().get();
//		}, commentUnchanged, pvListUnchanged, commentTextProperty));

		saveButton.disableProperty().bind(dirty.not());
		addPvButton.disableProperty().bind(pvNameField.textProperty().isEmpty());

		readOnlyCheckBox.selectedProperty().bindBidirectional(readOnlyProperty);

	}

	@FXML
	public void saveSaveSet(ActionEvent event) {

		UI_EXECUTOR.execute(() -> {
			try {
				loadedConfig.putProperty(DESCRIPTION_PROPERTY, commentTextArea.textProperty().getValue());
				loadedConfig = service.updateSaveSet(loadedConfig, saveSetEntries);
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
		ConfigPv configPv = ConfigPv.builder()
				.pvName(pvNameProperty.get().trim())
				.readOnly(readOnlyProperty.get())
				.readbackPvName(readbackPvNameProperty.get() == null || readbackPvNameProperty.get().isEmpty() ?  null : readbackPvNameProperty.get().trim())
				.build();

		UI_EXECUTOR.execute(() -> {
			saveSetEntries.add(configPv);
			resetAddPv();
		});

	}

	private void resetAddPv(){
		pvNameProperty.set("");
		readOnlyProperty.set(false);
		readbackPvNameProperty.set("");
	}


	public String loadSaveSet(Node node) {
		try {
			List<ConfigPv> configPvs = service.getConfigPvs(node.getUniqueId());
			loadedConfig = node;
			Collections.sort(configPvs);
			UI_EXECUTOR.execute(() -> {
				commentTextArea.textProperty().setValue(loadedConfig.getProperty(DESCRIPTION_PROPERTY));
//				commentTextProperty.set(loadedConfig.getProperty("description"));
//				originalComment = loadedConfig.getProperty("description");
				saveSetEntries.setAll(configPvs);
				pvTable.setItems(saveSetEntries);
				pvTable.setEditable(true);
//				commentUnchanged.set(true);
//				pvListUnchanged.set(true);
				dirty.set(false);
			});
			return loadedConfig.getName();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
