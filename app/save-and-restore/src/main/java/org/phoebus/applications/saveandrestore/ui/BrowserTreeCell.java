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

import java.io.IOException;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;


import javafx.fxml.FXMLLoader;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

public class BrowserTreeCell extends TreeCell<Node> {

	private javafx.scene.Node folderBox;
	private javafx.scene.Node saveSetBox;
	private javafx.scene.Node snapshotBox;

	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;
	private ContextMenu rootFolderContextMenu;

	private TextField textField;

	public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu saveSetContextMenu,
			ContextMenu snapshotContextMenu, ContextMenu rootFolderContextMenu) {

		FXMLLoader loader = new FXMLLoader();

		try {
			loader.setLocation(this.getClass().getResource("fxml/TreeCellGraphic.fxml"));
			javafx.scene.Node rootNode = loader.load();
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
		this.rootFolderContextMenu = rootFolderContextMenu;
	}

	@Override
	public void startEdit() {

		if (getItem().getNodeType().equals(NodeType.SNAPSHOT) || getItem().getId() == Node.ROOT_NODE_ID) {
			return;
		}
		super.startEdit();
		if (textField == null) {
			createTextField();
		}
		//setText(null);
		textField.setText(getItem().getName());
		setGraphic(textField);
		textField.selectAll();
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();
		if(getItem().getNodeType().equals(NodeType.CONFIGURATION)){
			setGraphic(saveSetBox);
		}
		else if(getItem().getNodeType().equals(NodeType.FOLDER)){
			setGraphic(folderBox);
		}
	}

	@Override
	public void updateItem(Node treeNode, boolean empty) {
		super.updateItem(treeNode, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			return;
		}

		switch(treeNode.getNodeType()) {
		case SNAPSHOT: 
			((Label) snapshotBox.lookup("#primaryLabel")).setText(treeNode.getName());
			((Label) snapshotBox.lookup("#secondaryLabel"))
					.setText(treeNode.getLastModified() + " (" + treeNode.getUserName() + ")");
			setGraphic(snapshotBox);
			setTooltip(new Tooltip("Double click to open snapshot"));
			setContextMenu(snapshotContextMenu);
			setEditable(false);
			break;
		case CONFIGURATION:
			((Label) saveSetBox.lookup("#savesetLabel")).setText(treeNode.getName());
			setGraphic(saveSetBox);
			setTooltip(new Tooltip("Double click to open save set"));
			setContextMenu(saveSetContextMenu);
			break;
		case FOLDER:
			String labelText = treeNode.getName();
			if (treeNode.getId() != se.esss.ics.masar.model.Node.ROOT_NODE_ID) {
				setContextMenu(folderContextMenu);
			} else {
				//labelText = SaveAndRestoreService.getInstance().getServiceIdentifier();
				setContextMenu(rootFolderContextMenu);
			}
			((Label) folderBox.lookup("#folderLabel")).setText(labelText);
			setGraphic(folderBox);
			break;
		}
	}

	private void createTextField() {
		textField = new TextField(getString());

		textField.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER && isNewNameValid()) {
				getItem().setName(textField.getText());
				super.commitEdit(getItem());
			}
			else if(keyEvent.getCode() == KeyCode.ESCAPE){
				cancelEdit();
			}
		});
	}

	private String getString() {
		return getItem() == null ? "" : getItem().getName();
	}

	/**
	 * Checks if the specified new name for a node is valid. It cannot be empty,
	 * and cannot have the same value as a sibling node of the same type.
	 * @return <code>true</code> if the name is valid, otherwise <code>false</code>.
	 */
	private boolean isNewNameValid(){
		if(textField.getText().isEmpty()){
			return false;
		}
		ObservableList<TreeItem<Node>> siblings = getTreeItem().getParent().getChildren();
		for(TreeItem<Node> sibling : siblings){
			if(sibling.getValue().getId() != getItem().getId() &&
					sibling.getValue().getName().equals(textField.getText()) &&
					sibling.getValue().getNodeType().equals(getItem().getNodeType())){
				return false;
			}
		}

		return true;
	}
}
