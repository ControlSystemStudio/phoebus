package org.phoebus.applications.display.navigation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DisplayNavigationViewController {

    private File rootFile;
    // Model
    private ProcessOPI model;

    @FXML
    TextField rootFileLabel;
    @FXML
    ListView<File> listView;
    @FXML
    TreeView<File> treeView;

    @FXML
    public void initialize() {
    }
    /**
     * The Root file for which the navigation path needs to be displayed.
     * The file must be an .opi or .bob
     * @param rootFile the display files whose navigation path is to be displayed
     */
    public void setRootFile(File rootFile) {
        this.rootFile = rootFile;
        model = new ProcessOPI(this.rootFile);
        refresh();
    }

    public void refresh() {
        rootFileLabel.setText(rootFile.getPath());
        listView.setItems(FXCollections.observableArrayList(model.process()));
        reconstructTree();
    }

    /**
     * Dispose the existing model, recreate a new one and
     */
    @FXML
    private void reconstructTree() {
        DisplayNavigationTreeItem root = new DisplayNavigationTreeItem(this.rootFile);
        treeView.setRoot(root);
    }

    private class DisplayNavigationTreeItem extends TreeItem<File> {

        private AtomicBoolean isFirstTimeLeaf = new AtomicBoolean(true);
        private AtomicBoolean isFirstTimeChildren = new AtomicBoolean(true);
        private volatile boolean isLeaf;

        public DisplayNavigationTreeItem(File root) {
            super(root);
        }

        @Override
        public ObservableList<TreeItem<File>> getChildren() {
            if(isFirstTimeChildren.getAndSet(false)){
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            if (isFirstTimeLeaf.getAndSet(false))
            {
                isLeaf = getChildren().isEmpty();
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
            File item = treeItem.getValue();
            Set<File> childrens = ProcessOPI.getLinkedFiles(item);
            ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
            for (File child : ProcessOPI.getLinkedFiles(item)) {
                children.add(new DisplayNavigationTreeItem(child));
            }
            return children;
        }
    }
}
