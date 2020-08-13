package org.phoebus.applications.display.navigation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DisplayNavigationViewController {

    private File rootFile;
    // Model
    private ProcessOPI model;
    private Task<Set<File>> allLinks;

    private ExecutorService service = Executors.newCachedThreadPool();

    @FXML
    TextField rootFileTextField;
    @FXML
    ListView<File> listView;
    @FXML
    TreeView<File> treeView;

    // Trying to replicate the context menu similar to the one in file browser
    private final MenuItem open = new MenuItem(Messages.Open, ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final MenuItem copy = new MenuItem("Copy File Names", ImageCache.getImageView(PhoebusApplication.class, "/icons/copy.png"));
    private final Menu openWith = new Menu(Messages.OpenWith, ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final ContextMenu contextMenu = new ContextMenu();

    @FXML
    public void initialize() {
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * The Root file for which the navigation path needs to be displayed.
     * The file must be an .opi or .bob
     *
     * @param rootFile the display files whose navigation path is to be displayed
     */
    public void setRootFile(File rootFile) {
        this.rootFile = rootFile;
        model = new ProcessOPI(this.rootFile);
        refresh();
    }


    @FXML
    public void browseNewRoot() {
        FileChooser fileChooser = new FileChooser();
        if (Paths.get(rootFileTextField.getText()).toFile().getParent() != null) {
            fileChooser.setInitialDirectory(Paths.get(rootFileTextField.getText()).toFile().getParentFile());
        }

        File newRootFile = fileChooser.showOpenDialog(treeView.getParent().getScene().getWindow());
        if (newRootFile != null)
            setRootFile(newRootFile);
    }

    public void refresh() {
        rootFileTextField.setText(rootFile.getPath());

        // update the list view
        allLinks = new ProcessOPIAllLinksTask(rootFile);
        allLinks.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                listView.setItems(FXCollections.observableArrayList(allLinks.getValue()));
            }
        });
        service.submit(allLinks);

        // update the tree view
        reconstructTree();

        contextMenu.getItems().addAll(open, openWith);
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
            if (isFirstTimeChildren.getAndSet(false)) {
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            if (isFirstTimeLeaf.getAndSet(false)) {
                isLeaf = getChildren().isEmpty();
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
            File item = treeItem.getValue();
            ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
            for (File child : ProcessOPI.getLinkedFiles(item)) {
                children.add(new DisplayNavigationTreeItem(child));
            }
            return children;
        }
    }


    /**
     * Try to open resource, show error dialog on failure
     *
     * @param file  Resource to open
     * @param stage Stage to use to prompt for specific app.
     *              Otherwise <code>null</code> to use default app.
     */
    private void openResource(final File file, final Stage stage) {
        if (!ApplicationLauncherService.openFile(file, stage != null, stage)) {
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setHeaderText("Failed to open file: " + file);
            DialogHelper.positionDialog(alert, treeView, -300, -200);
            alert.showAndWait();
        }
    }

    @FXML
    public void createTreeContextMenu(ContextMenuEvent e) {
        final ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue().getSelectedItems();
        List<File> selectedFiles = selectedItems.stream().map(item -> {
            return item.getValue();
        }).collect(Collectors.toList());

        createContextMenu(e, selectedFiles, treeView);
    }

    @FXML
    public void createListContextMenu(ContextMenuEvent e) {

        final ObservableList<File> selectedItems = listView.selectionModelProperty().getValue().getSelectedItems();

        createContextMenu(e, selectedItems, listView);
    }

    private void createContextMenu(ContextMenuEvent e,
                                   final List<File> selectedItems,
                                   Control control) {
        contextMenu.getItems().clear();
        if (!selectedItems.isEmpty()) {
            open.setOnAction(event -> {
                selectedItems.forEach(item -> {
                    openResource(item, null);
                });
            });
            copy.setOnAction(event -> {
                final ClipboardContent content = new ClipboardContent();
                content.putString(selectedItems.stream()
                        .map(f -> {
                            return f.getPath();
                        })
                        .collect(Collectors.joining(System.getProperty("line.separator"))));
                Clipboard.getSystemClipboard().setContent(content);
            });
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(open);
        }
        // If just one entry selected, check if there are multiple apps from which to select
        if (selectedItems.size() == 1) {
            final File file = selectedItems.get(0);
            final URI resource = ResourceParser.getURI(file);
            final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
            if (applications.size() > 0) {
                openWith.getItems().clear();
                for (AppResourceDescriptor app : applications) {
                    final MenuItem open_app = new MenuItem(app.getDisplayName());
                    final URL icon_url = app.getIconURL();
                    if (icon_url != null)
                        open_app.setGraphic(new ImageView(icon_url.toExternalForm()));
                    open_app.setOnAction(event -> app.create(resource));
                    openWith.getItems().add(open_app);
                }
                contextMenu.getItems().add(openWith);
            }
        }
        contextMenu.show(control.getScene().getWindow(), e.getScreenX(), e.getScreenY());
    }
}
