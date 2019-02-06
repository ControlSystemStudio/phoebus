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

import org.phoebus.applications.saveandrestore.ui.model.TreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNodeType;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;


public class BrowserTreeCell extends TreeCell<TreeNode> {

	private javafx.scene.Node folderBox;
	private javafx.scene.Node saveSetBox;
	private javafx.scene.Node snapshotBox;

	private TextField textField;

	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;
	private ContextMenu rootFolderContextMenu;

	public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu saveSetContextMenu,
			ContextMenu snapshotContextMenu, ContextMenu rooFolderContextMenu) {

		FXMLLoader loader = new FXMLLoader();

		try {
			loader.setLocation(this.getClass().getResource("fxml/TreeCellGraphic.fxml"));
			Node rootNode = loader.load();
			folderBox = rootNode.lookup("#folder");
			saveSetBox = rootNode.lookup("#saveset");
			snapshotBox = rootNode.lookup("#snapshot");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		this.folderContextMenu = folderContextMenu;
		this.saveSetContextMenu = saveSetContextMenu;
		this.snapshotContextMenu = snapshotContextMenu;
		this.rootFolderContextMenu = rooFolderContextMenu;
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
				((Label) snapshotBox.lookup("#primaryLabel")).setText(treeNode.getName().get());
				((Label) snapshotBox.lookup("#secondaryLabel"))
						.setText(treeNode.getLastModified() + " (" + treeNode.getUserName() + ")");
				setGraphic(snapshotBox);
				setTooltip(new Tooltip("Double click to open snapshot"));
				setContextMenu(snapshotContextMenu);
				setEditable(false);
			} else if (treeNode.getType().equals(TreeNodeType.SAVESET)) {
				((Label) saveSetBox.lookup("#savesetLabel")).setText(treeNode.getName().get());
				setGraphic(saveSetBox);
				setTooltip(new Tooltip("Double click to open saveset"));
				setContextMenu(saveSetContextMenu);
			} else if (treeNode.getType().equals(TreeNodeType.FOLDER)) {
				((Label) folderBox.lookup("#folderLabel")).setText(treeNode.getName().get());
				setGraphic(folderBox);
				if (treeNode.getId() != se.esss.ics.masar.model.Node.ROOT_NODE_ID) {
					setContextMenu(folderContextMenu);
				} else {
					setContextMenu(rootFolderContextMenu);
				}
			}
		}
	}

	@Override
	public void startEdit() {
		if (getItem().getType().equals(TreeNodeType.SNAPSHOT) || 
				getItem().getId() == se.esss.ics.masar.model.Node.ROOT_NODE_ID) {
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
				/* || handleEditDone(getItem(), textField.getText()) */) {
					cancelEdit();
				}
			}
		});
	}

	private String getString() {
		return getItem() == null ? "" : getItem().getName().get();
	}
}
