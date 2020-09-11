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

import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.applications.saveandrestore.model.Node;

import java.io.IOException;


/**
 * A cell editor managing the different type of nodes in the save-and-restore tree.
 * Implements aspects like icon selection, text layout, context menu and editing.
 */
public class BrowserTreeCell extends TreeCell<Node> {

	private javafx.scene.Node folderBox;
	private javafx.scene.Node saveSetBox;
	private javafx.scene.Node snapshotBox;

	private ContextMenu folderContextMenu;
	private ContextMenu saveSetContextMenu;
	private ContextMenu snapshotContextMenu;
	private ContextMenu rootFolderContextMenu;

	public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu saveSetContextMenu,
			ContextMenu snapshotContextMenu, ContextMenu rootFolderContextMenu) {

		FXMLLoader loader = new FXMLLoader();

		try {
			loader.setLocation(BrowserTreeCell.class.getResource("TreeCellGraphic.fxml"));
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
	public void updateItem(Node node, boolean empty) {
		super.updateItem(node, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			setContextMenu(null);
			return;
		}

		switch(node.getNodeType()) {
		case SNAPSHOT:
			((Label) snapshotBox.lookup("#primaryLabel"))
					.setText(node.getName());
			((Label) snapshotBox.lookup("#secondaryLabel")).setText(node.getCreated() + " (" + node.getUserName() + ")");
			((ImageView) snapshotBox.lookup("#tagIcon")).setVisible(!node.getTags().isEmpty());
			setGraphic(snapshotBox);
			if(node.getProperty("golden") != null && Boolean.valueOf(node.getProperty("golden"))){
				((ImageView)snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-golden.png"));
			}
			else{
				((ImageView)snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot.png"));
			}
			setContextMenu(snapshotContextMenu);
			String comment = node.getProperty("comment");
			if(comment != null && !comment.isEmpty()){
				setTooltip(new Tooltip(comment));
			}
			setEditable(false);
			break;
		case CONFIGURATION:
			((Label) saveSetBox.lookup("#savesetLabel")).setText(node.getName());
			setGraphic(saveSetBox);
			String description = node.getProperty("description");
			if(description != null && !description.isEmpty()) {
				setTooltip(new Tooltip(description));
			}
			setContextMenu(saveSetContextMenu);
			break;
		case FOLDER:
			String labelText = node.getName();
			if (node.getProperty("root") != null && Boolean.valueOf(node.getProperty("root"))) {
				setContextMenu(rootFolderContextMenu);
			} else {
				setContextMenu(folderContextMenu);
			}
			((Label) folderBox.lookup("#folderLabel")).setText(labelText);
			setGraphic(folderBox);
			break;
		}
	}
}
