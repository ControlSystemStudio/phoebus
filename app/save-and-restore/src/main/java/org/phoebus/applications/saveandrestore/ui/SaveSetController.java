/*
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
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.ConfigProperty;
import org.phoebus.applications.saveandrestore.ui.model.TreeNode;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
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
	private TextField saveSetNameField;

	@FXML
	private Tab tab;

	private SaveAndRestoreService service;

	private static Executor UI_EXECUTOR = Platform::runLater;

	public SaveSetController() {
		service = SaveAndRestoreService.getInstance();
	}

	private String originalComment;
	private String originalSaveSetName;
	private SimpleBooleanProperty commentUnchanged = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty saveSetNameUnchanged = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty pvListUnchanged = new SimpleBooleanProperty(true);
	private SimpleStringProperty tabTitle = new SimpleStringProperty();

	private SimpleStringProperty commentTextProperty = new SimpleStringProperty();
	private SimpleStringProperty saveSetNamePorperty = new SimpleStringProperty();	

	private Config config;
	
	private ConfigProperty configProperty = new ConfigProperty();

	@FXML
	public void initialize() {
		
	

		pvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		providerColumn.setCellFactory(
				param -> new RadioButtonCell<ConfigPv, Provider>(EnumSet.allOf(Provider.class)));

		providerColumn.setOnEditCommit(new EventHandler<CellEditEvent<ConfigPv, Provider>>() {
			@Override
			public void handle(CellEditEvent<ConfigPv, Provider> t) {
				((ConfigPv) t.getTableView().getItems().get(t.getTablePosition().getRow()))
					.setProvider(t.getNewValue());
			}
		});
		
		pvNameColumn.setCellValueFactory(new PropertyValueFactory<ConfigPv, String>("pvName"));

		//commentTextArea.textProperty().bindBidirectional(commentTextProperty);
		commentTextArea.textProperty().bindBidirectional(configProperty.getDescription());
		commentTextArea.textProperty().addListener(ce -> {
			commentUnchanged.set(commentTextArea.textProperty().get().equals(originalComment));
			config.setDescription(commentTextArea.textProperty().get());
			updateTabTitle();
		});
		
		saveSetNameField.textProperty().bindBidirectional(saveSetNamePorperty);
		saveSetNameField.textProperty().addListener(ce -> {
			saveSetNameUnchanged.set(saveSetNameField.textProperty().get().equals(originalSaveSetName));
			config.setName(saveSetNameField.textProperty().get());
			updateTabTitle();
		});

		ContextMenu contextMenu = new ContextMenu();
		MenuItem addMenuItem = new MenuItem("Add PV");
		MenuItem deleteMenuItem = new MenuItem("Delete PV");
		deleteMenuItem.setOnAction(ae -> {
			ObservableList<ConfigPv> selectedPvs = pvTable.getSelectionModel().getSelectedItems();
			UI_EXECUTOR.execute(() -> {
				pvTable.getItems().removeAll(selectedPvs);
				pvListUnchanged.set(false);
				updateTabTitle();
			});
		});

		contextMenu.getItems().addAll(addMenuItem, deleteMenuItem);

		pvTable.setContextMenu(contextMenu);
		pvTable.setEditable(true);

		pvTable.getItems().addListener(new ListChangeListener<ConfigPv>() {

			@Override
			public void onChanged(Change<? extends ConfigPv> change) {
				if (change.wasAdded() || change.wasRemoved()) {

				}
			}
		});

		saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
				return commentUnchanged.get() && saveSetNameUnchanged.get() && pvListUnchanged.get();}, 
				commentUnchanged, 
				saveSetNameUnchanged, 
				pvListUnchanged));
		
		saveButton.setOnAction(ae -> {
			saveSaveSet();
		});
		
		tab.textProperty().bind(tabTitle);

		tab.setOnCloseRequest(e -> {
			if (commentUnchanged.get() && pvListUnchanged.get()) {
				return;
			}

			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Save changes?");
			alert.setHeaderText("Content has changed. Do you wish to save the save set?");
			ButtonType save = new ButtonType("Save");
			ButtonType dontSave = new ButtonType("Don't save");
			ButtonType cancel = new ButtonType("Cancel");

			alert.getButtonTypes().setAll(cancel, dontSave, save);

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get().equals(dontSave)) {
				return;
			} else if (result.get().equals(cancel)) {
				e.consume();
			} else {
				saveSaveSet();
			}
		});
	}

	private void updateTabTitle() {
		if (commentUnchanged.get() && pvListUnchanged.get() && saveSetNameUnchanged.get()) {
			tabTitle.set(originalSaveSetName);
		} else {
			tabTitle.set("* " + saveSetNameField.textProperty().get());
		}
	}

	private void saveSaveSet() {
		
		UI_EXECUTOR.execute(() -> {
			//config.setDescription(commentTextProperty.get());
			//config.setName(saveSetNameField.textProperty().get());
			//config.setConfigPvList(toConfigPvList());
			try {
				int configId = config.getId();
				if (configId > 0) {
					service.updateSaveSet(config);
				} else {
					configId = service.saveSaveSet(config).getId();
				}
				loadSaveSet(configId);
			} catch (Exception e1) {
				Alert errorAlert = new Alert(AlertType.ERROR);
				errorAlert.setTitle("Action failed");
				errorAlert.setHeaderText(e1.getMessage());
				errorAlert.showAndWait();
			}
		});
		
	}
	
	public void loadSaveSet(TreeNode treeNode) {
		loadSaveSet(treeNode.getId());
		configProperty.setName(treeNode.getName());
	}

	private void loadSaveSet(int saveSetId) {
		try {
			config = service.getSaveSet(saveSetId);
			Collections.sort(config.getConfigPvList());
			UI_EXECUTOR.execute(() -> {
				configProperty.setDescription(config.getDescription());
				//configProperty.setName(config.getName());
				saveSetNamePorperty.set(config.getName());
				commentTextProperty.set(config.getDescription());
				originalComment = config.getDescription();
				pvTable.setItems(FXCollections.observableArrayList(config.getConfigPvList()));
				commentUnchanged.set(true);
				pvListUnchanged.set(true);
				originalSaveSetName = config.getName();
				tabTitle.set(originalSaveSetName);
				//saveSetNameField.textProperty().set(originalSaveSetName);
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class RadioButtonCell<S, T extends Enum<T>> extends TableCell<S, T> {

		private EnumSet<T> enumeration;

		public RadioButtonCell(EnumSet<T> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if (!empty) {
				// UI setup
				HBox hb = new HBox(7);
				hb.setAlignment(Pos.CENTER);
				final ToggleGroup group = new ToggleGroup();

				// create a radio button for each 'element' of the enumeration
				for (Enum<T> enumElement : enumeration) {
					RadioButton radioButton = new RadioButton(enumElement.toString());
					radioButton.setUserData(enumElement);
					radioButton.setToggleGroup(group);
					hb.getChildren().add(radioButton);
					if (enumElement.equals(item)) {
						radioButton.setSelected(true);
					}
				}

				// issue events on change of the selected radio button
				group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

					@SuppressWarnings("unchecked")
					@Override
					public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue,
							Toggle newValue) {
						getTableView().edit(getIndex(), getTableColumn());
						RadioButtonCell.this.commitEdit((T) newValue.getUserData());
					}
				});
				setGraphic(hb);
			}
		}
	}
}
