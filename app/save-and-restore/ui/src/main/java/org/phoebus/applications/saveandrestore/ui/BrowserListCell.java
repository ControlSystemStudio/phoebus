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
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;


/**
 * A cell editor managing the SNAPSHOT node type in the save-and-restore snapshot list.
 * Implements aspects like icon selection, text layout, context menu and editing.
 */
public class BrowserListCell extends ListCell<Node> {

	private javafx.scene.Node snapshotBox;

	private ContextMenu snapshotContextMenu;

	public BrowserListCell(ContextMenu snapshotContextMenu) {

		FXMLLoader loader = new FXMLLoader();

		try {
			loader.setLocation(BrowserListCell.class.getResource("TreeCellGraphic.fxml"));
			javafx.scene.Node rootNode = loader.load();
			snapshotBox = rootNode.lookup("#snapshot");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		this.snapshotContextMenu = snapshotContextMenu;
	}

	@Override
	public void updateItem(Node node, boolean empty) {
		super.updateItem(node, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			return;
		}

		((Label) snapshotBox.lookup("#primaryLabel"))
				.setText(node.getName());
		((Label) snapshotBox.lookup("#secondaryLabel")).setText(node.getCreated() + " (" + node.getUserName() + ")");
		((ImageView) snapshotBox.lookup("#tagIcon")).setVisible(!node.getTags().isEmpty());
		setGraphic(snapshotBox);
		if(node.getProperty("golden") != null && Boolean.valueOf(node.getProperty("golden"))){
			((ImageView)snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserListCell.class, "/icons/save-and-restore/snapshot-golden.png"));
		}
		else{
			((ImageView)snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserListCell.class, "/icons/save-and-restore/snapshot.png"));
		}
		setContextMenu(snapshotContextMenu);
		String comment = node.getProperty("comment");
		if(comment != null && !comment.isEmpty()){
			setTooltip(new Tooltip(comment));
		}
		setEditable(false);
	}
}
