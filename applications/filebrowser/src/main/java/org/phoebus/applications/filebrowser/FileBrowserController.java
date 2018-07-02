package org.phoebus.applications.filebrowser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Controller for the file browser app
 *
 * @author Kunal Shroff
 *
 */
public class FileBrowserController {

    @FXML
    TextField path;
    @FXML
    Button browse;
    @FXML
    TreeView<File> treeView;

    private ContextMenu contextMenu;

    @FXML
    public void initialize() {
        File file = Paths.get(System.getProperty("user.home")).toFile();
        TreeItem<File> root = new FileTreeItem(file);
        path.setText(file.getAbsolutePath());
        treeView.setCellFactory(f -> new FileTreeCell());
        treeView.setRoot(root);

        // Create ContextMenu
        contextMenu = new ContextMenu();

        MenuItem open = new MenuItem("Open");
        open.setGraphic(ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
        open.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                        .getSelectedItems();
                selectedItems.forEach(s -> {
                    File selection = s.getValue();
                    if (selection.isFile()) {
                        ApplicationLauncherService.openFile(selection, false, null);
                    }
                });
            }
        });
        MenuItem openWith = new MenuItem("Open With");
        openWith.setGraphic(ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
        openWith.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                        .getSelectedItems();
                selectedItems.forEach(s -> {
                    File selection = s.getValue();
                    if (selection.isFile()) {
                        ApplicationLauncherService.openFile(selection, true,
                                (Stage) treeView.getParent().getScene().getWindow());
                    }
                });
            }
        });
        contextMenu.getItems().addAll(open, openWith);
    }

    private final class FileTreeItem extends TreeItem<File> {

        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private boolean isLeaf;

        public FileTreeItem(File childFile) {
            super(childFile);
        }

        @Override
        public ObservableList<TreeItem<File>> getChildren() {

            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            if (isFirstTimeLeaf) {
                isFirstTimeLeaf = false;
                File f = getValue();
                isLeaf = f.isFile();
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
            File f = TreeItem.getValue();
            if (f != null && f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null) {
                    ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                    for (File childFile : files) {
                        children.add(new FileTreeItem(childFile));
                    }

                    return children;
                }
            }

            return FXCollections.emptyObservableList();
        }
    }


    private final class FileTreeCell extends TreeCell<File> {
        public FileTreeCell() {
            super();
        }

        ImageView image = ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png");
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
                        setGraphic(image);
                    }
                    setText(file.getName());
                }
            }

        }
    }

    @FXML
    public void createContextMenu(ContextMenuEvent e) {
        treeView.setContextMenu(null);
        ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue().getSelectedItems();
        if (selectedItems.stream().allMatch(item -> {
            return item.getValue().isFile();
        })) {
            treeView.setContextMenu(contextMenu);
        }
    }

    @FXML
    public void handleMouseClickEvents(MouseEvent e) {
        if (e.getClickCount() == 2) {
            ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                    .getSelectedItems();
            selectedItems.forEach(s -> {
                File selection = s.getValue();
                if (selection.isFile()) {
                    ApplicationLauncherService.openFile(selection, true,
                            (Stage) treeView.getParent().getScene().getWindow());
                }
            });
        }
    }

    @FXML
    public void setNewRoot() {
        Path p = Paths.get(path.getText());
        File newRootFile = p.toFile();
        treeView.setRoot(new FileTreeItem(newRootFile));
    }

    /** @param directory Desired root directory */
    public void setRoot(final File directory)
    {
        path.setText(directory.toString());
        treeView.setRoot(new FileTreeItem(directory));
    }

    /** @return Root directory */
    public File getRoot()
    {
        return treeView.getRoot().getValue();
    }

    @FXML
    public void browseNewRoot() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Browser Root");
        if (Paths.get(path.getText()).toFile().isDirectory()) {
            directoryChooser.setInitialDirectory(Paths.get(path.getText()).toFile());
        }
        File newRootFile = directoryChooser.showDialog(treeView.getParent().getScene().getWindow());
        path.setText(newRootFile.getAbsolutePath());
        treeView.setRoot(new FileTreeItem(newRootFile));
    }
}
