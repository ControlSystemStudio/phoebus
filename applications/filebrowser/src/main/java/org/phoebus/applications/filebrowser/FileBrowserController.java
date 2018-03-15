package org.phoebus.applications.filebrowser;

import java.io.File;
import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

public class FileBrowserController {

    @FXML
    TextField path;
    @FXML
    TreeView<File> treeView;

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        File file = new File("C:\\git");
        TreeItem<File> root = new FileTreeItem(file);
        // treeView = new TreeView<File>();
        // root = populateFileTree(root);
        treeView.setCellFactory(f -> new FileTreeCell());
        treeView.setRoot(root);
    }

    private TreeItem<File> populateFileTree(TreeItem<File> parent) {
        Arrays.asList(parent.getValue().listFiles()).forEach(child -> {
            TreeItem<File> childTreeItem = new TreeItem<File>(child);
            if (child.isDirectory()) {
                parent.getChildren().add(childTreeItem);
            } else {
                parent.getChildren().add(childTreeItem);
            }
        });
        return parent;

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
                File f = (File) getValue();
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
        @SuppressWarnings("unchecked")
        public FileTreeCell() {
            super();
        }

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
                    String dir = "";
                    if (file.isDirectory()) {
                        dir = "D>";
                    }
                    setText(dir + file.getName());
                }
            }

        }
    }
    
    @FXML
    public void createContextMenu(ContextMenuEvent e) {
        //System.out.println("create context: " + e.toString());
    }
    
    @FXML
    public void handleMouseClickEvents(MouseEvent e) {
        //System.out.println("double click: " + e.toString());
        //System.out.println(e.getClickCount());
        ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue().getSelectedItems();
        selectedItems.forEach(s ->{
            File selection = s.getValue();
            if(selection.isFile()) {
                System.out.println("opening file " + selection.getAbsolutePath());
            }
        });
        
    }
}
