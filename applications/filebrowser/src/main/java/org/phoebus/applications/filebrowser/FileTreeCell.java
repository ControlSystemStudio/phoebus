package org.phoebus.applications.filebrowser;

import java.io.File;

import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

@SuppressWarnings("nls")
final class FileTreeCell extends TreeCell<File> {

    private static final Image folder = ImageCache.getImage(PhoebusApplication.class, "/icons/fldr_obj.png");

    @Override
    protected void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);

        if (empty || file == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (getTreeItem().getParent() == null) {
                setText(file.getAbsolutePath());
            } else {
                if (file.isDirectory()) {
                    setGraphic(new ImageView(folder));
                }
                setText(file.getName());
            }
        }

    }
}