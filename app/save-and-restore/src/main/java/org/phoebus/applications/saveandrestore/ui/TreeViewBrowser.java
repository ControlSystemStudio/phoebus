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

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.FolderTreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
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
import javafx.util.Duration;
import se.esss.ics.masar.model.Node;

/**
 * @author georgweiss Created 9 Jan 2019
 */
public class TreeViewBrowser extends TreeView<TreeNode> {

	private static Executor UI_EXECUTOR = Platform::runLater;

	private EventHandler<TreeItem.TreeModificationEvent<TreeNode>> nodeExpandedHandler;

	private TreeNodeItem treeRootItem;

	private TreeNode selectedTreeNode;

	private SaveAndRestoreService service;

	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;

	private static final int NEW_FOLDER_ID = -1;

	public TreeViewBrowser() {

		setEditable(true);
		service = SaveAndRestoreService.getInstance();

		nodeExpandedHandler = new EventHandler<TreeItem.TreeModificationEvent<TreeNode>>() {
			@Override
			public void handle(TreeModificationEvent<TreeNode> event) {
				expandTreeNode(event);
			}
		};

		this.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				TreeItem<TreeNode> item = getSelectionModel().getSelectedItem();
				if (item != null) {
					selectedTreeNode = item.getValue();
					if (mouseEvent.getClickCount() == 2) {
						nodeDoubleClicked(getSelectionModel().getSelectedItem());
					}
				}
			}
		});

		folderContextMenu = new ContextMenu();
		MenuItem newFolderMenuItem = new MenuItem("New");
		newFolderMenuItem.setOnAction(ae -> {
			handleNewFolder(getSelectionModel().getSelectedItem());
		});
		MenuItem deleteFolderMenuItem = new MenuItem("Delete");

		folderContextMenu.getItems().addAll(newFolderMenuItem, deleteFolderMenuItem);

		saveSetContextMenu = new ContextMenu();
		MenuItem newSaveSetMenuItem = new MenuItem("New");
		MenuItem deleteSaveSetMenuItem = new MenuItem("Delete");
		saveSetContextMenu.getItems().addAll(newSaveSetMenuItem, deleteSaveSetMenuItem);

		snapshotContextMenu = new ContextMenu();
		MenuItem deleteSnapshotMenuItem = new MenuItem("Delete");
		MenuItem compareSaveSetMenuItem = new MenuItem("Compare");
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
	 * {@link DataProvider}. This should be called when the UI is set up and when
	 * the {@link DataProvider} is changed or reconfigured, e.g. when specifying a
	 * different service URL.
	 */
	public void loadInitialTreeData() {

		if (treeRootItem != null) {
			treeRootItem.removeEventHandler(TreeItem.branchExpandedEvent(), nodeExpandedHandler);
		}

		TreeNode treeRoot = service.getRootNode();
		treeRootItem = new TreeNodeItem(treeRoot);

		for (TreeNode childNode : ((FolderTreeNode) treeRoot).getChildren()) {
			treeRootItem.getChildren().add(new TreeNodeItem(childNode));
		}

		treeRootItem.setExpanded(true);
		treeRootItem.addEventHandler(TreeItem.branchExpandedEvent(), nodeExpandedHandler);

		UI_EXECUTOR.execute(() -> super.setRoot(treeRootItem));
	}

	/**
	 * Handles expansion of a tree node. Queries the {@link DataProvider} service
	 * for child nodes of the node associated with the event.
	 * 
	 * @param event The event triggered by an expansion of a tree node.
	 */
	private void expandTreeNode(TreeModificationEvent<TreeNode> event) {

		TreeItem<TreeNode> targetItem = event.getTreeItem();
		targetItem.getChildren().clear();
		List<TreeNodeItem> childItems = service.getChildNodes((FolderTreeNode) targetItem.getValue()).stream()
				.map(i -> new TreeNodeItem(i)).collect(Collectors.toList());
		childItems.sort(new Comparator<TreeNodeItem>() {
			
			@Override
			public int compare(TreeNodeItem item1, TreeNodeItem item2) {
				return item1.getValue().getName().compareTo(item2.getValue().getName());
			}
		});
		UI_EXECUTOR.execute(() -> targetItem.getChildren().addAll(childItems));

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
//	private boolean handleRenameNode(TreeNode treeNode, String newName) {
//		if (treeNode.getType().equals(TreeNodeType.SNAPSHOT)) {
//			return false;
//		}
//		try {
//			if (treeNode.getId() != NEW_FOLDER_ID) {
//				service.rename(treeNode, newName);
//			}
//			return true;
//		} catch (Exception e) {
//			Alert dialog = new Alert(AlertType.ERROR);
//			dialog.setTitle("Rename failed");
//			dialog.setHeaderText(e.getMessage());
//			dialog.showAndWait();
//			return false;
//		}
//	}

	private boolean handleEditDone(TreeNode treeNode, String nodeName) {
		if (treeNode.getType().equals(TreeNodeType.SNAPSHOT)) {
			return false;
		}
//		if (treeNode.getId() == NEW_FOLDER_ID) {
//			try {
//				treeNode.setName(nodeName);
//				TreeNode newTreeNode = 
//						service.createNewTreeNode(getSelectionModel().getSelectedItem().getParent().getValue().getId(), treeNode);
//				treeNode.setId(newTreeNode.getId());
//				return true;
//			} catch (Exception e) {
//				Alert dialog = new Alert(AlertType.ERROR);
//				dialog.setTitle("Create new node failed");
//				dialog.setHeaderText(e.getMessage());
//				dialog.showAndWait();
//				return false;
//			}
//
//		} else {
//			try {
//				service.rename(treeNode, nodeName);
//				return true;
//			} catch (Exception e) {
//				Alert dialog = new Alert(AlertType.ERROR);
//				dialog.setTitle("Rename failed");
//				dialog.setHeaderText(e.getMessage());
//				dialog.showAndWait();
//				return false;
//			}
//		}
		try {
			if (treeNode.getId() == NEW_FOLDER_ID) {
				treeNode.setName(nodeName);
				TreeNode newTreeNode = service.createNewTreeNode(
						getSelectionModel().getSelectedItem().getParent().getValue().getId(), treeNode);
				treeNode.setId(newTreeNode.getId());		
			} else {
				service.rename(treeNode, nodeName);
				treeNode.setName(nodeName);
			}
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
		if (!parentTreeItem.isExpanded()) {
			parentTreeItem.setExpanded(true);
		}
		// Construct a new node and add it to the parent
		FolderTreeNode newFolderNode = FolderTreeNode.builder().id(NEW_FOLDER_ID)
				.name(getNameForNewFolder(parentTreeItem)).type(TreeNodeType.FOLDER).build();

		TreeNodeItem newFolderItem = new TreeNodeItem(newFolderNode);
		parentTreeItem.getChildren().add(newFolderItem);
		getSelectionModel().select(newFolderItem);
		PauseTransition p = new PauseTransition(Duration.millis(150));
		p.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				edit(newFolderItem);
			}
		});
		p.play();

	}

	/**
	 * Subclass of {@link TreeItem} using {@link TreeNode} (and subclasses) to hold
	 * business data.
	 * 
	 * @author georgweiss Created 3 Jan 2019
	 */
	private class TreeNodeItem extends TreeItem<TreeNode> {

		TreeNode treeNode;

		TreeNodeItem(TreeNode treeNode) {
			super(treeNode);
			this.treeNode = treeNode;
		}

		@Override
		public boolean isLeaf() {
			return treeNode.isLeaf();
		}
		
		@Override
		public String toString() {
			return treeNode.getName();
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
					snapshotNameLabel.setText(treeNode.getName());
					snapshotMetaDataLabel.setText(treeNode.getLastModified() + " (" + treeNode.getUserName() + ")");
					setGraphic(snapshotBox);
					setTooltip(new Tooltip("Double click to open snapshot"));
					setContextMenu(snapshotContextMenu);
				} else if (treeNode.getType().equals(TreeNodeType.SAVESET)) {
					saveSetNameLabel.setText(treeNode.getName());
					setGraphic(saveSetBox);
					setTooltip(new Tooltip("Double click to open saveset"));
					setContextMenu(saveSetContextMenu);
				} else {
					folderNameLabel.setText(treeNode.getName());
					setGraphic(folderBox);
					if(treeNode.getId() != Node.ROOT_NODE_ID) {
						setContextMenu(folderContextMenu);
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
			if (getItem() != null) {
				textField.setText(getItem().getName());
				updateItem(getItem(), false);
			}
		}

		private void createTextField() {
			textField = new TextField(getString());

			textField.setOnKeyPressed(keyEvent -> {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					if (textField.getText().equals(getItem().getName())) {
						cancelEdit();
					} else if (handleEditDone(getItem(), textField.getText())) {
						//getItem().setName(textField.getText());
						cancelEdit();
					}
				} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
					cancelEdit();
				}
			});
		}

		private String getString() {
			return getItem() == null ? "" : getItem().getName();
		}
	}

	private String getNameForNewFolder(TreeItem<TreeNode> parentFolderItem) {

		ObservableList<TreeItem<TreeNode>> children = parentFolderItem.getParent().getChildren();

		int index = 0;
		while (true) {
			String newFolderName = "untitled folder";
			if (index > 0) {
				newFolderName += " " + index;
			}
			for (TreeItem<TreeNode> child : children) {
				if (child.getValue().getName().equals(newFolderName)) {
					continue;
				}
			}
			return newFolderName;
		}
	}

}
