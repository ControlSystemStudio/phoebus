package org.phoebus.applications.filebrowser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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
@SuppressWarnings("nls")
public class FileBrowserController {

    private final DirectoryMonitor monitor;

    @FXML
    TextField path;
    @FXML
    Button browse;
    @FXML
    TreeView<File> treeView;

    private final MenuItem open = new MenuItem("Open", ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final MenuItem openWith = new MenuItem("Open With", ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final ContextMenu contextMenu = new ContextMenu();

    public FileBrowserController()
    {
        try
        {
            monitor = new DirectoryMonitor(this::handleFilesystemChanges);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Cannot monitor files", ex);
        }
    }

    private void handleFilesystemChanges(final File file, final boolean added)
    {
        System.out.println("Oh dear, " + file + (added ? " was added" : " was removed"));
    }

    /** Try to open resource, show error dialog on failure
     *  @param file Resource to open
     *  @param stage Stage to use to prompt for specific app.
     *               Otherwise <code>null</code> to use default app.
     */
    private void openResource(final File file, final Stage stage)
    {
        if (! ApplicationLauncherService.openFile(file, stage != null, stage))
        {
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setHeaderText("Cannot open\n  " + file + ",\nsee log for details");
            DialogHelper.positionDialog(alert, treeView, -300, -200);
            alert.showAndWait();
        }
    }

    @FXML
    public void initialize() {
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setCellFactory(f -> new FileTreeCell());

        // Prepare ContextMenu items
        open.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                        .getSelectedItems();
                selectedItems.forEach(s -> {
                    File selection = s.getValue();
                    if (selection.isFile()) {
                        openResource(selection, null);
                    }
                });
            }
        });
        openWith.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                        .getSelectedItems();
                selectedItems.forEach(s -> {
                    File selection = s.getValue();
                    if (selection.isFile()) {
                        openResource(selection, (Stage) treeView.getParent().getScene().getWindow());
                    }
                });
            }
        });
        contextMenu.getItems().addAll(open, openWith);
    }

    @FXML
    public void createContextMenu(ContextMenuEvent e) {
        final ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue().getSelectedItems();

        contextMenu.getItems().clear();

        if (selectedItems.stream().allMatch(item -> item.getValue().isFile()))
            contextMenu.getItems().addAll(open, openWith);

        if (! selectedItems.isEmpty())
            contextMenu.getItems().add(new CopyPath(selectedItems));
        if (selectedItems.size() == 1)
        {
            if (selectedItems.get(0).getValue().isFile())
                contextMenu.getItems().add(new DuplicateAction(treeView, selectedItems.get(0)));
            contextMenu.getItems().addAll(new RenameAction(treeView,  selectedItems.get(0)));
        }
        if (selectedItems.size() >= 1)
        {
            contextMenu.getItems().add(new DeleteAction(treeView, selectedItems));
            if (selectedItems.get(0).getValue().isDirectory())
                contextMenu.getItems().add(new RefreshAction(treeView,  selectedItems.get(0)));
        }

        if (selectedItems.size() == 1)
            contextMenu.getItems().addAll(new PropertiesAction(treeView,  selectedItems.get(0)));

        contextMenu.show(treeView.getScene().getWindow(), e.getScreenX(), e.getScreenY());
    }

    @FXML
    public void handleMouseClickEvents(MouseEvent e) {
        if (e.getClickCount() == 2) {
            ObservableList<TreeItem<File>> selectedItems = treeView.selectionModelProperty().getValue()
                    .getSelectedItems();
            selectedItems.forEach(s -> {
                File selection = s.getValue();
                if (selection.isFile()) {
                    // Open in 'default' application, no prompt.
                    // Use context menu "Open With" if need to pick app.
                    openResource(selection, null);
                }
            });
        }
    }

    @FXML
    public void setNewRoot() {
        Path p = Paths.get(path.getText());
        setRoot(p.toFile());
    }

    /** @param directory Desired root directory */
    public void setRoot(final File directory)
    {
        monitor.clear();
        path.setText(directory.toString());
        treeView.setRoot(new FileTreeItem(monitor, directory));
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
        if (newRootFile != null)
            setRoot(newRootFile);
    }

    /** Call when no longer needed */
    public void shutdown()
    {
        monitor.shutdown();
    }
}
