package org.phoebus.applications.filebrowser;

import static org.phoebus.applications.filebrowser.FileBrowser.logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

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
 * @author Kay Kasemir
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
        monitor = new DirectoryMonitor(this::handleFilesystemChanges);
    }

    private void handleFilesystemChanges(final File file, final boolean added)
    {
        // The notification might address a file that the file browser itself just added/renamed/removed,
        // and the file browser is already in the process of updating itself.
        // Wait a little to allow that to happen
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException ex)
        {
            return;
        }

        // Now check if the UI has already been updated
        if (added)
            assertTreeContains(treeView.getRoot(), file.toPath());
        else
            assertTreeDoesntContain(treeView.getRoot(), file.toPath());
    }

    private void assertTreeContains(final TreeItem<File> item, final Path file)
    {
        final Path dir = item.getValue().toPath();
        if (! file.startsWith(dir))
        {
            logger.log(Level.WARNING, "Cannot check for " + file + " within " + dir);
            return;
        }

        // Are we looking for a directory, and this one is it? Done!
        if (dir.equals(file))
            return;

        final int dir_len = dir.getNameCount();
        final File sub = new File(item.getValue(), file.getName(dir_len).toString());
        logger.log(Level.FINE, () -> "Looking for " + sub + " in " + dir);

        for (TreeItem<File> child : item.getChildren())
            if (sub.equals(child.getValue()))
            {
                logger.log(Level.FINE,"Found it!");
                if (sub.isDirectory())
                    assertTreeContains(child, file);
                return;
            }

        logger.log(Level.FINE, () -> "Forcing refresh of " + dir + " to show " + sub);
        ((FileTreeItem)item).forceRefresh();
    }

    private void assertTreeDoesntContain(final TreeItem<File> item, final Path file)
    {
        final Path dir = item.getValue().toPath();
        logger.log(Level.FINE, () -> "Does " + dir + " still contain " + file + "?");
        if (! file.startsWith(dir))
        {
            logger.log(Level.FINE, "Can't!");
            return;
        }

        final int dir_len = dir.getNameCount();
        final File sub = new File(item.getValue(), file.getName(dir_len).toString());
        for (TreeItem<File> child : item.getChildren())
            if (sub.equals(child.getValue()))
            {
                // Found file or sub path to it..
                if (sub.isDirectory())
                    assertTreeDoesntContain(child, file);
                else
                {   // Found the file still listed as a child of 'item',
                    // so refresh 'item'
                    logger.log(Level.FINE, () -> "Forcing refresh of " + dir + " to hide " + sub);
                    ((FileTreeItem)item).forceRefresh();
                }
                return;
            }

        logger.log(Level.FINE, "Not found, all good");
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
            final TreeItem<File> item = selectedItems.get(0);
            final boolean is_file = item.getValue().isFile();
            if (is_file)
            {
                // Plain file can be duplicated, directory can't
                contextMenu.getItems().add(new DuplicateAction(treeView, item));

                // Create directory within the _parent_
                contextMenu.getItems().addAll(new CreateDirectoryAction(treeView, item.getParent()));
            }
            else
                // Within a directory, a new directory can be created
                contextMenu.getItems().addAll(new CreateDirectoryAction(treeView, item));
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
