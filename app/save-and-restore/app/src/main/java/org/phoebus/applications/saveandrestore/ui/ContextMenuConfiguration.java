/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.ui.javafx.ImageCache;

public class ContextMenuConfiguration extends ContextMenuBase{

    public ContextMenuConfiguration(SaveAndRestoreController saveAndRestoreController,
                                    TreeView<Node> treeView) {
        super(saveAndRestoreController, treeView);

        Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");

        MenuItem openConfigurationMenuItem = new MenuItem(Messages.contextMenuCreateSnapshot, new ImageView(ImageRepository.CONFIGURATION));
        openConfigurationMenuItem.disableProperty().bind(multipleSelection);
        openConfigurationMenuItem.setOnAction(ae -> saveAndRestoreController.openConfigurationForSnapshot());

        MenuItem editConfigurationMenuItem = new MenuItem(Messages.Edit, new ImageView(ImageRepository.EDIT_CONFIGURATION));
        editConfigurationMenuItem.disableProperty().bind(multipleSelection);
        editConfigurationMenuItem.setOnAction(ae -> saveAndRestoreController.nodeDoubleClicked());

        ImageView exportConfigurationIconImageView = new ImageView(csvExportIcon);
        exportConfigurationIconImageView.setFitWidth(18);
        exportConfigurationIconImageView.setFitHeight(18);

        MenuItem exportConfigurationMenuItem = new MenuItem(Messages.exportConfigurationLabel, exportConfigurationIconImageView);
        exportConfigurationMenuItem.disableProperty().bind(multipleSelection);
        exportConfigurationMenuItem.setOnAction(ae -> saveAndRestoreController.exportConfiguration());

        ImageView importSnapshotIconImageView = new ImageView(csvImportIcon);
        importSnapshotIconImageView.setFitWidth(18);
        importSnapshotIconImageView.setFitHeight(18);

        MenuItem importSnapshotMenuItem = new MenuItem(Messages.importSnapshotLabel, importSnapshotIconImageView);
        importSnapshotMenuItem.disableProperty().bind(multipleSelection);
        importSnapshotMenuItem.setOnAction(ae -> saveAndRestoreController.importSnapshot());

        Image copyIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/copy.png");
        MenuItem copyMenuItem = new MenuItem(Messages.copy, new ImageView(copyIcon));
        copyMenuItem.setOnAction(action -> saveAndRestoreController.copySelectionToClipboard());

        Image pasteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/paste.png");
        MenuItem pasteMenuItem = new MenuItem(Messages.paste, new ImageView(pasteIcon));
        pasteMenuItem.setOnAction(ae -> saveAndRestoreController.pasteFromClipboard());

        setOnShowing(event -> {
            pasteMenuItem.setDisable(!saveAndRestoreController.mayPaste());
            copyMenuItem.setDisable(!saveAndRestoreController.mayCopy());
        });

        getItems().addAll(openConfigurationMenuItem,
                editConfigurationMenuItem,
                copyMenuItem,
                pasteMenuItem,
                deleteNodesMenuItem,
                copyUniqueIdToClipboardMenuItem,
                exportConfigurationMenuItem,
                importSnapshotMenuItem);
    }
}
