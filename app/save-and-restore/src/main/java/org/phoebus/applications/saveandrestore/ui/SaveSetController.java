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

package org.phoebus.applications.saveandrestore.ui;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
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
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Provider;

public class SaveSetController {

	@FXML
	private TableColumn<ConfigPv, Provider> providerColumn;

	@FXML
	private TableColumn<ConfigPv, String> pvNameColumn;

	@FXML
	private TableView<ConfigPv> pvTable;

	@FXML
	private TextArea commentTextArea;

	@FXML
	private Button saveButton;

	@FXML
	private TextField pvNameField;

	@FXML
	private RadioButton ca;

	@FXML
	private RadioButton pva;

	@FXML
	private ToggleGroup providerToggleGroup;

	@FXML
	private Button addPvButton;

	private SimpleStringProperty pvNameProperty = new SimpleStringProperty("");

	private ObjectProperty<Provider> providerChoice = new SimpleObjectProperty<>(Provider.ca);

	private SaveAndRestoreService service;

	private static Executor UI_EXECUTOR = Platform::runLater;

	public SaveSetController() {
		service = SaveAndRestoreService.getInstance();
	}

	private String originalComment;
	private SimpleBooleanProperty commentUnchanged = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty pvListUnchanged = new SimpleBooleanProperty(true);

	private SimpleStringProperty commentTextProperty = new SimpleStringProperty();

	private ObservableList<ConfigPv> saveSetEntries = FXCollections.observableArrayList();

	private SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);

	private static final String PROVIDER = "provider";

	private Config loadedConfig;

	@FXML
	public void initialize() {

		pvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		pvTable.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			selectionEmpty.set(nv == null);
		});

		providerColumn.setCellValueFactory(new PropertyValueFactory<>(PROVIDER));
		providerColumn.setOnEditCommit(t ->
				(t.getTableView().getItems().get(t.getTablePosition().getRow()))
						.setProvider(t.getNewValue()));
		
		commentTextArea.textProperty().bindBidirectional(commentTextProperty);
		commentTextArea.textProperty().addListener(ce -> {
			commentUnchanged.set(commentTextArea.textProperty().get().equals(originalComment));
		});

		ContextMenu contextMenu = new ContextMenu();

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

		contextMenu.getItems().addAll(deleteMenuItem);

		pvNameColumn.setEditable(true);
		pvNameColumn.setCellFactory(new Callback<TableColumn<ConfigPv, String>, TableCell<ConfigPv, String>>() {
			@Override
			public TableCell<ConfigPv, String> call(TableColumn<ConfigPv, String> param) {
				final TableCell cell = new TableCell() {

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
								setText(getItem().toString());
								setGraphic(null);
							}
						}
					}
				};
				// This way I will have context menu only for specific column
				cell.setContextMenu(contextMenu);
				return cell;
			}
		});




		ca.getProperties().put(PROVIDER, Provider.ca);
		pva.getProperties().put(PROVIDER, Provider.pva);

		providerToggleGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
			providerChoice.set((Provider)nv.getProperties().get(PROVIDER));
		});

		pvNameField.textProperty().bindBidirectional(pvNameProperty);
		pvNameField.textProperty().addListener((ce -> {
			if(pvNameField.textProperty().get().isEmpty()){
				addPvButton.setDisable(true);
			}
			else{
				addPvButton.setDisable(false);
			}
		}));

		saveSetEntries.addListener(new ListChangeListener<ConfigPv>() {

			@Override
			public void onChanged(Change<? extends ConfigPv> change) {
				while (change.next()) {
					if (change.wasAdded() || change.wasRemoved()) {
						FXCollections.sort(saveSetEntries);
						pvListUnchanged.set(false);
					}
				}
			}
		});

		saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return commentUnchanged.get() && pvListUnchanged.get();
		}, commentUnchanged, pvListUnchanged));


	}

	@FXML
	public void saveSaveSet(ActionEvent event) {

		UI_EXECUTOR.execute(() -> {
			try {
				Config config = Config.builder().name(loadedConfig.getName()).description(commentTextProperty.get()).build();
				config.setConfigPvList(saveSetEntries);
				int id = loadedConfig.getId();
				if (id > 0) {
					config.setId(id);
					service.updateSaveSet(config);
				} else {
					id = service.saveSaveSet(config).getId();
				}
				loadSaveSet(id);
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
				.provider(providerChoice.get())
				.build();

		UI_EXECUTOR.execute(() -> {
			saveSetEntries.add(configPv);
			resetAddPv();
		});

	}

	private void resetAddPv(){
		pvNameProperty.set("");
		providerChoice.set(Provider.ca);
	}

	public String loadSaveSet(Node treeNode) {
		return loadSaveSet(treeNode.getId());
	}

	private String loadSaveSet(int saveSetId) {
		try {
			loadedConfig = service.getSaveSet(saveSetId);
			Collections.sort(loadedConfig.getConfigPvList());
			UI_EXECUTOR.execute(() -> {
				commentTextProperty.set(loadedConfig.getDescription());
				originalComment = loadedConfig.getDescription();
				saveSetEntries.setAll(loadedConfig.getConfigPvList());
				pvTable.setItems(saveSetEntries);
				commentUnchanged.set(true);
				pvListUnchanged.set(true);
			});
			return loadedConfig.getName();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
