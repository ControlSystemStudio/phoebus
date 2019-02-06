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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.FolderTreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNodeType;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

public class SaveAndRestoreController implements Initializable{
	
	private static Executor UI_EXECUTOR = Platform::runLater;

	@FXML
	private TreeView<TreeNode> treeView;
	
	@FXML
	private TabPane tabPane;
	
	private SaveAndRestoreService service;
	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;
	private ContextMenu rootFolderContextMenu;
	
	private static final int NEW_FOLDER_ID = -1;
	
	public SaveAndRestoreController() {
		service = SaveAndRestoreService.getInstance();
		folderContextMenu = new ContextMenu();
		MenuItem newFolderMenuItem = new MenuItem("New folder");
		newFolderMenuItem.setOnAction(ae -> {
			handleNewFolder(treeView.getSelectionModel().getSelectedItem());
		});

		MenuItem deleteFolderMenuItem = new MenuItem("Delete folder");
		deleteFolderMenuItem.setOnAction(ae -> {
			deleteFolder(treeView.getSelectionModel().getSelectedItem());
		});

		MenuItem newSaveSetMenuItem = new MenuItem("New save set");

		folderContextMenu.getItems().addAll(newFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);

		rootFolderContextMenu = new ContextMenu();
		MenuItem newRootFolderMenuItem = new MenuItem("New folder");
		newRootFolderMenuItem.setOnAction(ae -> {
			handleNewFolder(treeView.getSelectionModel().getSelectedItem());
		});
		rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

		saveSetContextMenu = new ContextMenu();

		MenuItem deleteSaveSetMenuItem = new MenuItem("Delete save set");
		deleteSaveSetMenuItem.setOnAction(ae -> {
			handleDeleteSaveSet(treeView.getSelectionModel().getSelectedItem());
		});

		saveSetContextMenu.getItems().addAll(deleteSaveSetMenuItem);
		

		snapshotContextMenu = new ContextMenu();
		MenuItem deleteSnapshotMenuItem = new MenuItem("Delete snapshot");
		deleteSnapshotMenuItem.setOnAction(ae -> {
			handleDeleteSnapshot(treeView.getSelectionModel().getSelectedItem());
		});
		
		MenuItem compareSaveSetMenuItem = new MenuItem("Compare snapshots");
		snapshotContextMenu.getItems().addAll(deleteSnapshotMenuItem, compareSaveSetMenuItem);
		
	}
	
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		treeView.setEditable(true);
		treeView.setCellFactory(new Callback<TreeView<TreeNode>, TreeCell<TreeNode>>() {
			@Override
			public TreeCell<TreeNode> call(TreeView<TreeNode> p) {
				return new BrowserTreeCell(folderContextMenu, saveSetContextMenu, snapshotContextMenu, rootFolderContextMenu);
			}
		});
		
		treeView.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				TreeItem<TreeNode> item = treeView.getSelectionModel().getSelectedItem();
				if (item != null) {
					if (mouseEvent.getClickCount() == 2) {
						nodeDoubleClicked(treeView.getSelectionModel().getSelectedItem());
					}
				}
			}
		});
		
		loadInitialTreeData();
	}
	
	/**
	 * Loads the data for the tree root as provided (persisted) by the current
	 * {@link DataProvider}.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	private void loadInitialTreeData() {

		TreeNode treeRoot = service.getRootNode();
		TreeNodeItem treeRootItem = new TreeNodeItem(treeRoot);

		treeRootItem.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
			expandTreeNode(((TreeItem.TreeModificationEvent)e).getTreeItem());
		});

		UI_EXECUTOR.execute(() -> {
			treeView.setRoot(treeRootItem);
			treeRootItem.setExpanded(true);
			// expandTreeNode(treeRootItem);
		});
	}
	
	/**
	 * Handles expansion of a tree node. Queries the {@link DataProvider} service
	 * for child nodes of the node associated with the event.
	 * 
	 * @param event The event triggered by an expansion of a tree node.
	 */
	private void expandTreeNode(TreeItem<TreeNode> targetItem) {

		targetItem.getChildren().clear();
		ObservableList<TreeNodeItem> childItems = FXCollections.observableArrayList(service.getChildNodes((FolderTreeNode) targetItem.getValue()).stream()
				.map(i -> new TreeNodeItem(i)).collect(Collectors.toList()));
		Collections.sort(childItems);
		UI_EXECUTOR.execute(() -> {
			targetItem.getChildren().addAll(childItems);
		});
	}
	
	private void handleDeleteSnapshot(TreeItem<TreeNode> treeItem) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Delete snapshot?");
		alert.setContentText("Deletion is irreversible. Do you wish to continue?");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.OK)) {
			TreeItem<TreeNode> parent = treeItem.getParent();
			service.deleteNode(treeItem.getValue());
			UI_EXECUTOR.execute(() -> {
				parent.getChildren().remove(treeItem);
			});
		}
	}
	
	private void handleDeleteSaveSet(TreeItem<TreeNode> treeItem) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Delete save set?");
		alert.setHeaderText("All snapshots for this save set will be deleted!");
		alert.setContentText("Deletion is irreversible. Do you wish to continue?");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.OK)) {
			TreeItem<TreeNode> parent = treeItem.getParent();
			service.deleteNode(treeItem.getValue());
			UI_EXECUTOR.execute(() -> {
				parent.getChildren().remove(treeItem);
				for(Tab tab : tabPane.getTabs()) {
					if(tab.getText().equals(treeItem.getValue().getName().get())) {
						tabPane.getTabs().remove(tab);
						break;
					}
				}
			});
		}
	}
	
	private void handleNewFolder(TreeItem<TreeNode> parentTreeItem) {

		List<String> existingFolderNames = parentTreeItem.getChildren().stream().map(item -> item.getValue().getName().get())
				.collect(Collectors.toList());

		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("New Folder");
		dialog.setContentText("Specify a folder name:");
		dialog.setHeaderText(null);
		dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

		dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			String value = newValue.trim();
			dialog.getDialogPane().lookupButton(ButtonType.OK)
					.setDisable(existingFolderNames.contains(value) || value.isBlank());
		});

		// NewFolderInputDialog dialog = new NewFolderInputDialog(existingFolderNames);

		DialogHelper.positionDialog(dialog, treeView, -200, -100);

		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			FolderTreeNode newFolderNode = FolderTreeNode.builder().id(NEW_FOLDER_ID).name(result.get())
					.type(TreeNodeType.FOLDER).build();
			try {
				TreeNode newTreeNode = service
						.createNewTreeNode(treeView.getSelectionModel().getSelectedItem().getValue().getId(), newFolderNode);
				parentTreeItem.getChildren().add(new TreeNodeItem(newTreeNode));
				parentTreeItem.setExpanded(true);
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Action failed");
				alert.setHeaderText(e.getMessage());
				alert.showAndWait();
			}
		}
	}
	
	private void deleteFolder(TreeItem<TreeNode> treeItem) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Delete folder?");
		alert.setHeaderText("All folders, save sets and snapshots in this folder and sub-folders will be deleted!");
		alert.setContentText("Deletion is irreversible. Do you wish to continue?");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.OK)) {
			TreeItem<TreeNode> parent = treeItem.getParent();
			service.deleteNode(treeItem.getValue());
			UI_EXECUTOR.execute(() -> {
				parent.getChildren().remove(treeItem);
			});
		}
	}
	
	/**
	 * 
	 * Handles selection of a node. The action taken depends on the node type found
	 * in {@link TreeNode#type}.
	 * 
	 * @param newValue Tree node associated with the selection
	 */
	private void nodeDoubleClicked(TreeItem<TreeNode> newValue) {

		switch (newValue.getValue().getType()) {

		case SNAPSHOT:
//			SERVICE_EXECUTOR.accept("Snapshot node selected", () -> {
//
//				SaveSet saveSet = new SaveSet(new Branch(), Optional.empty(),
//						new String[] { newValue.getValue().getName() },
//						SaveRestoreService.getInstance().getSelectedDataProvider().getId());
//				SnapshotTreeNode snapshotTreeNode = (SnapshotTreeNode) newValue.getValue();
//				Snapshot snapshot = new Snapshot(saveSet,
//						Instant.ofEpochMilli(snapshotTreeNode.getLastModified().getTime()),
//						snapshotTreeNode.getComment(), snapshotTreeNode.getUserName());
//				snapshot.setSnapshotId(Integer.toString(newValue.getValue().getId()));
//				actionManager.openSnapshot(snapshot);
//			});
			break;
		case SAVESET:
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(this.getClass().getResource("fxml/SaveSetEditor.fxml"));
			
			
			
			try {
				Tab ui = loader.load();
				SaveSetController controller = loader.getController();
				tabPane.getTabs().add(ui);
				controller.loadSaveSet(treeView.getSelectionModel().getSelectedItem().getValue());
				tabPane.getSelectionModel().select(ui);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			
//			tabPane.getTabs().add(new Tab());
//			SERVICE_EXECUTOR.accept("SaveSet node selected", () -> {
//				SaveSet saveSet = new SaveSet(new Branch(), Optional.empty(),
//						new String[] { newValue.getValue().getName() },
//						SaveRestoreService.getInstance().getSelectedDataProvider().getId());
//				saveSet.setSaveSetId(Integer.toString(newValue.getValue().getId()));
//				saveSet.setFullyQualifiedName("/config/" + newValue.getValue().getId());
//				saveSet.setLastModified(newValue.getValue().getLastModified());
//				saveSet.setUserName(newValue.getValue().getUserName());
//				actionManager.openSaveSet(saveSet);
//			});
			break;
		case FOLDER:
		default:
		}
	}
}
