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

import java.io.ObjectInputFilter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;

import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Provider;

public class SaveSetController {

	@FXML
	private TableColumn<ConfigPv, Provider> providerColumn;

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
	private ObjectProperty<Provider> providerProperty = new SimpleObjectProperty<>(Provider.ca);

	@FXML
	private SimpleBooleanProperty readOnlyProperty = new SimpleBooleanProperty(false);

	private SaveAndRestoreService service;

	private static Executor UI_EXECUTOR = Platform::runLater;

	public SaveSetController() {
		service = SaveAndRestoreService.getInstance();
	}

	private SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);

	//private String originalComment;
//	private SimpleBooleanProperty commentUnchanged = new SimpleBooleanProperty(true);
//	private SimpleBooleanProperty pvListUnchanged = new SimpleBooleanProperty(true);
//	private SimpleStringProperty commentTextProperty = new SimpleStringProperty();

	private ObservableList<ConfigPv> saveSetEntries = FXCollections.observableArrayList();

	private SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);

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

		providerColumn.setCellValueFactory(new PropertyValueFactory<>(PROVIDER));
		providerColumn.setOnEditCommit(t ->
				(t.getTableView().getItems().get(t.getTablePosition().getRow()))
						.setProvider(t.getNewValue()));
		
//		commentTextArea.textProperty().bindBidirectional(commentTextProperty);
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
			ObservableList<ConfigPv> selectedPvs = pvTable.getSelectionModel().getSelectedItems();
			if(selectedPvs == null || selectedPvs.isEmpty()){
				return;
			}

			// TODO: Launch dialog and call remote service

		});

		pvNameContextMenu.getItems().addAll(deleteMenuItem, renamePvMenuItem);

		pvNameColumn.setEditable(true);
		pvNameColumn.setCellValueFactory(
				new PropertyValueFactory<ConfigPv, String>("pvName"));
		pvNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
//		pvNameColumn.setOnEditCommit(
//				new EventHandler<TableColumn.CellEditEvent<ConfigPv, String>>() {
//					@Override
//					public void handle(TableColumn.CellEditEvent<ConfigPv, String> t) {
//
//					}
//				}
//		);
//		pvNameColumn.setCellFactory(new Callback<>() {
//			@Override
//			public TableCell<ConfigPv, String> call(TableColumn<ConfigPv, String> param) {
//				final TableCell cell = new TableCell() {
//
//					@Override
//					public void updateItem(Object item, boolean empty) {
//						super.updateItem(item, empty);
//						selectionEmpty.set(empty);
//						if (empty) {
//							setText(null);
//						} else {
//							if (isEditing()) {
//								setText(null);
//							} else {
//								setText(getItem().toString());
//								setGraphic(null);
//							}
//						}
//					}
//				};
//				cell.setContextMenu(pvNameContextMenu);
//				return cell;
//			}
//		});

		ContextMenu readbackPvNameContextMenu = new ContextMenu();
		MenuItem renameReadbackPvMenuItem = new MenuItem("Edit read-back PV Name");
		renamePvMenuItem.setOnAction(ae -> {
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


		ca.getProperties().put(PROVIDER, Provider.ca);
		pva.getProperties().put(PROVIDER, Provider.pva);

		providerToggleGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
			providerProperty.set((Provider)nv.getProperties().get(PROVIDER));
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
				.provider(providerProperty.get())
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
		providerProperty.set(Provider.ca);
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
