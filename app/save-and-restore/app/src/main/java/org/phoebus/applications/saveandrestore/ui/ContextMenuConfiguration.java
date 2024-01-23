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

import javafx.beans.binding.Bindings;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Context menu for {@link org.phoebus.applications.saveandrestore.model.Node}s of type
 * {@link org.phoebus.applications.saveandrestore.model.NodeType#CONFIGURATION}.
 */
public class ContextMenuConfiguration extends ContextMenuBase {

    public ContextMenuConfiguration(SaveAndRestoreController saveAndRestoreController) {
        super(saveAndRestoreController);

        Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");

        MenuItem openConfigurationMenuItem = new MenuItem(Messages.contextMenuCreateSnapshot, new ImageView(ImageRepository.CONFIGURATION));
        openConfigurationMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty));
        openConfigurationMenuItem.setOnAction(ae -> saveAndRestoreController.openConfigurationForSnapshot());

        ImageView exportConfigurationIconImageView = new ImageView(csvExportIcon);
        exportConfigurationIconImageView.setFitWidth(18);
        exportConfigurationIconImageView.setFitHeight(18);

        MenuItem exportConfigurationMenuItem = new MenuItem(Messages.exportConfigurationLabel, exportConfigurationIconImageView);
        exportConfigurationMenuItem.disableProperty().bind(multipleNodesSelectedProperty);
        exportConfigurationMenuItem.setOnAction(ae -> saveAndRestoreController.exportConfiguration());

        ImageView importSnapshotIconImageView = new ImageView(csvImportIcon);
        importSnapshotIconImageView.setFitWidth(18);
        importSnapshotIconImageView.setFitHeight(18);

        MenuItem importSnapshotMenuItem = new MenuItem(Messages.importSnapshotLabel, importSnapshotIconImageView);
        importSnapshotMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty));
        importSnapshotMenuItem.setOnAction(ae -> saveAndRestoreController.importSnapshot());

        Image copyIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/copy.png");
        MenuItem copyMenuItem = new MenuItem(Messages.copy, new ImageView(copyIcon));
        copyMenuItem.setOnAction(action -> saveAndRestoreController.copySelectionToClipboard());
        copyMenuItem.disableProperty().bind(mayCopyProperty.not());

        Image pasteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/paste.png");
        MenuItem pasteMenuItem = new MenuItem(Messages.paste, new ImageView(pasteIcon));
        pasteMenuItem.setOnAction(ae -> saveAndRestoreController.pasteFromClipboard());
        pasteMenuItem.disableProperty().bind(mayPasteProperty.not());

        getItems().addAll(openConfigurationMenuItem,
                copyMenuItem,
                pasteMenuItem,
                deleteNodesMenuItem,
                copyUniqueIdToClipboardMenuItem,
                exportConfigurationMenuItem,
                importSnapshotMenuItem);
    }

    /**
     * Execute common checks (see {@link ContextMenuBase#runChecks()}) and:
     * <ul>
     *     <li>If copy operation is possible on selected {@link org.phoebus.applications.saveandrestore.model.Node}s</li>
     *     <li>If paste operation is possible on selected {@link org.phoebus.applications.saveandrestore.model.Node}s</li>
     * </ul>
     */
    @Override
    protected void runChecks() {
        super.runChecks();
        mayPasteProperty.set(saveAndRestoreController.mayPaste());
        mayCopyProperty.set(saveAndRestoreController.mayCopy());
    }
}
