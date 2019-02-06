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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.FolderTreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNodeType;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Callback;
import se.esss.ics.masar.model.Node;

/**
 * @author georgweiss Created 9 Jan 2019
 */
public class TreeViewBrowser extends TreeView<TreeNode> {

	private static Executor UI_EXECUTOR = Platform::runLater;

	private EventHandler<TreeItem.TreeModificationEvent<TreeNode>> nodeExpandedHandler;

	private TreeNodeItem treeRootItem;

	private SaveAndRestoreService service;

	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;
	private ContextMenu rootFolderContextMenu;

	private static final int NEW_FOLDER_ID = -1;

	public TreeViewBrowser() {

		setEditable(true);
		service = SaveAndRestoreService.getInstance();

		nodeExpandedHandler = new EventHandler<TreeItem.TreeModificationEvent<TreeNode>>() {
			@Override
			public void handle(TreeModificationEvent<TreeNode> event) {
				expandTreeNode(event.getTreeItem());
			}
		};

		this.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				TreeItem<TreeNode> item = getSelectionModel().getSelectedItem();
				if (item != null) {
					if (mouseEvent.getClickCount() == 2) {
						nodeDoubleClicked(getSelectionModel().getSelectedItem());
					}
				}
			}
		});

		folderContextMenu = new ContextMenu();
		MenuItem newFolderMenuItem = new MenuItem("New folder");
		newFolderMenuItem.setOnAction(ae -> {
			handleNewFolder(getSelectionModel().getSelectedItem());
		});

		MenuItem deleteFolderMenuItem = new MenuItem("Delete folder");
		deleteFolderMenuItem.setOnAction(ae -> {
			handleDeleteFolder(getSelectionModel().getSelectedItem());
		});

		MenuItem newSaveSetMenuItem = new MenuItem("New save set");

		folderContextMenu.getItems().addAll(newFolderMenuItem, deleteFolderMenuItem, newSaveSetMenuItem);

		rootFolderContextMenu = new ContextMenu();
		MenuItem newRootFolderMenuItem = new MenuItem("New folder");
		newRootFolderMenuItem.setOnAction(ae -> {
			handleNewFolder(getSelectionModel().getSelectedItem());
		});
		rootFolderContextMenu.getItems().add(newRootFolderMenuItem);

		saveSetContextMenu = new ContextMenu();

		MenuItem deleteSaveSetMenuItem = new MenuItem("Delete save set");
		deleteSaveSetMenuItem.setOnAction(ae -> {
			handleDeleteSaveSet(getSelectionModel().getSelectedItem());
		});

		saveSetContextMenu.getItems().addAll(deleteSaveSetMenuItem);
		

		snapshotContextMenu = new ContextMenu();
		MenuItem deleteSnapshotMenuItem = new MenuItem("Delete snapshot");
		deleteSnapshotMenuItem.setOnAction(ae -> {
			handleDeleteSnapshot(getSelectionModel().getSelectedItem());
		});
		
		MenuItem compareSaveSetMenuItem = new MenuItem("Compare snapshots");
		snapshotContextMenu.getItems().addAll(deleteSnapshotMenuItem, compareSaveSetMenuItem);

		this.setCellFactory(new Callback<TreeView<TreeNode>, TreeCell<TreeNode>>() {
			@Override
			public TreeCell<TreeNode> call(TreeView<TreeNode> p) {
				return new BrowserTreeCell();
			}
		});
		
		

	}

	/**
	 * Loads the data for the tree root as provided (persisted) by the current
	 * {@link DataProvider}.
	 */
	public void loadInitialTreeData() {

		TreeNode treeRoot = service.getRootNode();
		treeRootItem = new TreeNodeItem(treeRoot);

		treeRootItem.addEventHandler(TreeItem.branchExpandedEvent(), nodeExpandedHandler);

		UI_EXECUTOR.execute(() -> {
			super.setRoot(treeRootItem);
			treeRootItem.setExpanded(true);
			// expandTreeNode(treeRootItem);
		});
	}

	/**
	 * Handles expansion of a tree node. Queries the {@link DataProvider} service
	 * for child nodes of the node associated with the event. The child nodes are
	 * sorted by name only, the type is not considered. This mimics the behavior
	 * of the Mac OS Finder, where objects in a folder are sorted alphabetically without
	 * taking the type (folder or file) into consideration.
	 * 
	 * @param event The event triggered by an expansion of a tree node.
	 */
	private void expandTreeNode(TreeItem<TreeNode> targetItem) {

		targetItem.getChildren().clear();
		List<TreeNodeItem> childItems = service.getChildNodes((FolderTreeNode) targetItem.getValue()).stream()
				.map(i -> new TreeNodeItem(i)).collect(Collectors.toList());
		Collections.sort(childItems);
		UI_EXECUTOR.execute(() -> {
			targetItem.getChildren().addAll(childItems);
		});
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
//			
//			Tab tab = new Tab();
//			tab.setText(getSelectionModel().getSelectedItem().getValue().getName());
//			tab.setContent(new SaveSetEditor().getUI());
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

	/**
	 * Renames a {@link TreeNode} of type {@link TreeNodeType#FOLDER} or
	 * {@link TreeNodeType#SAVESET}. The operation may fail if the remote service
	 * determines that the new name is invalid, e.g. due to a name clash. A blocking
	 * error dialog is displayed if the server indicates that the operation has
	 * failed.
	 * 
	 * @param treeNode An existing node that the user wishes to rename. Must be of
	 *                 type {@link TreeNodeType#FOLDER} or
	 *                 {@link TreeNodeType#SAVESET}.
	 * @param newName  The new name for the node
	 * @return {@code true} if operation is successful on the server, otherwise
	 *         {@code false} (including if the specified node is of type
	 *         {@link TreeNodeType#SNAPSHOT}.
	 */
	private boolean handleEditDone(TreeNode treeNode, String nodeName) {
		try {
			service.rename(treeNode, nodeName);
			treeNode.setName(new SimpleStringProperty(nodeName));
			return true;
		} catch (Exception e) {
			Alert dialog = new Alert(AlertType.ERROR);
			dialog.setTitle("Action failed");
			dialog.setHeaderText(e.getMessage());
			dialog.showAndWait();
			return false;
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

		DialogHelper.positionDialog(dialog, this, -200, -100);

		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			FolderTreeNode newFolderNode = FolderTreeNode.builder().id(NEW_FOLDER_ID).name(result.get())
					.type(TreeNodeType.FOLDER).build();
			try {
				TreeNode newTreeNode = service
						.createNewTreeNode(getSelectionModel().getSelectedItem().getValue().getId(), newFolderNode);
				parentTreeItem.getChildren().add(new TreeNodeItem(newTreeNode));
				parentTreeItem.setExpanded(true);
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Action failed");
				alert.setHeaderText(e.getMessage());
				alert.showAndWait();
			}
		}

//		
//		PauseTransition p = new PauseTransition(Duration.millis(150));
//		p.setOnFinished(new EventHandler<ActionEvent>() {
//			@Override
//			public void handle(ActionEvent event) {
//				edit(newFolderItem);
//			}
//		});
//		p.play();
	}

	private void handleDeleteFolder(TreeItem<TreeNode> treeItem) {
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
			});
		}
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

	/**
	 * Cell renderer for a tree node item. It uses icons for save set and snapshot
	 * nodes, and also adds date and user name to snapshot nodes.
	 * 
	 * @author georgweiss Created 11 Jan 2019
	 */
	private class BrowserTreeCell extends TreeCell<TreeNode> {

		private HBox folderBox = new HBox();
		private HBox saveSetBox = new HBox();
		private HBox snapshotBox = new HBox();
		private VBox snapshotLabels = new VBox();
		private Label folderNameLabel = new Label();
		private Label saveSetNameLabel = new Label();
		private Label snapshotNameLabel = new Label();
		private Label snapshotMetaDataLabel = new Label();
		private ImageView folderIcon = new ImageView(ImageCache.getImage(TreeViewBrowser.class, "/icons/fldr_obj.png"));
		private ImageView saveSetIcon = new ImageView(ImageCache.getImage(TreeViewBrowser.class, "/icons/txt.png"));
		private ImageView snapshotIcon = new ImageView(
				ImageCache.getImage(TreeViewBrowser.class, "/icons/ksnapshot.png"));

		private TextField textField;

		public BrowserTreeCell() {
			folderBox.getChildren().addAll(folderIcon, folderNameLabel);
			saveSetBox.getChildren().addAll(saveSetIcon, saveSetNameLabel);
			snapshotLabels.getChildren().addAll(snapshotNameLabel, snapshotMetaDataLabel);
			snapshotBox.getChildren().addAll(snapshotIcon, snapshotLabels);
			snapshotMetaDataLabel.setFont(Font.font(Font.getDefault().getSize() - 3));
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		public void updateItem(TreeNode treeNode, boolean empty) {
			super.updateItem(treeNode, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
				return;
			}

			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setText(null);
				setGraphic(textField);
			} else {
				if (treeNode.getType().equals(TreeNodeType.SNAPSHOT)) {
					snapshotNameLabel.setText(treeNode.getName().get());
					snapshotMetaDataLabel.setText(treeNode.getLastModified() + " (" + treeNode.getUserName() + ")");
					setGraphic(snapshotBox);
					setTooltip(new Tooltip("Double click to open snapshot"));
					setContextMenu(snapshotContextMenu);
				} else if (treeNode.getType().equals(TreeNodeType.SAVESET)) {
					saveSetNameLabel.setText(treeNode.getName().get());
					setGraphic(saveSetBox);
					setTooltip(new Tooltip("Double click to open saveset"));
					setContextMenu(saveSetContextMenu);
				} else if (treeNode.getType().equals(TreeNodeType.FOLDER)) {
					folderNameLabel.setText(treeNode.getName().get());
					setGraphic(folderBox);
					if (treeNode.getId() != Node.ROOT_NODE_ID) {
						setContextMenu(folderContextMenu);
					} else {
						setContextMenu(rootFolderContextMenu);
					}
				}
			}
		}

		@Override
		public void startEdit() {
			if (getItem().getType().equals(TreeNodeType.SNAPSHOT) || getItem().getId() == Node.ROOT_NODE_ID) {
				return;
			}
			super.startEdit();
			if (textField == null) {
				createTextField();
			}
			setText(null);
			setGraphic(textField);
			textField.selectAll();
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			textField.setText(getItem().getName().get());
			updateItem(getItem(), false);
		}

		private void createTextField() {
			textField = new TextField(getString());

			textField.setOnKeyPressed(keyEvent -> {				
				if (keyEvent.getCode() == KeyCode.ENTER) {
					if (getItem().getName().equals(textField.getText())
							|| handleEditDone(getItem(), textField.getText())) {
						
						cancelEdit();
					}
				}
			});
		}

		private String getString() {
			return getItem() == null ? "" : getItem().getName().get();
		}
	}

	protected String getNameForNewFolder(TreeItem<TreeNode> parentFolderItem) {

		ObservableList<TreeItem<TreeNode>> children = parentFolderItem.getChildren();
		String newFolderNameBase = "untitled folder";
		String newName = newFolderNameBase;
		int index = 1;
		boolean nameClashFound = false;
		do {
			if (index > 1) {
				newName = newFolderNameBase + " " + index;
			}

			for (TreeItem<TreeNode> child : children) {
				if (child.getValue().getName().equals(newName)) {
					nameClashFound = true;
					break;
				}
			}
			if (!nameClashFound) {
				break;
			}
			index++;
			nameClashFound = false;
		} while (true);
		return newName;
	}

}
