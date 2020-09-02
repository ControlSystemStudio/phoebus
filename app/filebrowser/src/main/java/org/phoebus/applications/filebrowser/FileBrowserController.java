package org.phoebus.applications.filebrowser;

import static org.phoebus.applications.filebrowser.FileBrowser.logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.phoebus.ui.spi.ContextMenuEntry;

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
    TreeTableView<FileInfo> treeView;

    private final MenuItem open = new MenuItem(Messages.Open, ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final Menu openWith = new Menu(Messages.OpenWith, ImageCache.getImageView(PhoebusApplication.class, "/icons/fldr_obj.png"));
    private final ContextMenu contextMenu = new ContextMenu();

    public FileBrowserController()
    {
        monitor = new DirectoryMonitor(this::handleFilesystemChanges);
    }

    private void handleFilesystemChanges(final File file, final DirectoryMonitor.Change change)
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
        if (change == DirectoryMonitor.Change.ADDED)
            assertTreeContains(treeView.getRoot(), file.toPath());
        else if (change == DirectoryMonitor.Change.CHANGED)
            refreshTreeItem(treeView.getRoot(), file.toPath());
        else if (change == DirectoryMonitor.Change.REMOVED)
            assertTreeDoesntContain(treeView.getRoot(), file.toPath());
    }

    private void assertTreeContains(final TreeItem<FileInfo> item, final Path file)
    {
        final Path dir = item.getValue().file.toPath();
        if (! file.startsWith(dir))
        {
            logger.log(Level.WARNING, "Cannot check for " + file + " within " + dir);
            return;
        }

        // Are we looking for a directory, and this one is it? Done!
        if (dir.equals(file))
            return;

        final int dir_len = dir.getNameCount();
        final File sub = new File(item.getValue().file, file.getName(dir_len).toString());
        logger.log(Level.FINE, () -> "Looking for " + sub + " in " + dir);

        for (TreeItem<FileInfo> child : item.getChildren())
            if (sub.equals(child.getValue().file))
            {
                logger.log(Level.FINE,"Found it!");
                if (sub.isDirectory())
                    assertTreeContains(child, file);
                return;
            }

        logger.log(Level.FINE, () -> "Forcing refresh of " + dir + " to show " + sub);
        Platform.runLater(() -> ((FileTreeItem)item).forceRefresh());
    }

    private void refreshTreeItem(final TreeItem<FileInfo> item, final Path file)
    {
        final Path dir = item.getValue().file.toPath();
        if (dir.equals(file))
        {
            logger.log(Level.FINE, () -> "Forcing refresh of " + item);
            Platform.runLater(() ->
            {
                // Update and show the latest size, time, ...
                item.getValue().update();
                // Force tree to re-sort in case column sort is active
                treeView.sort();
            });
            return;
        }

        if (! file.startsWith(dir))
        {
            logger.log(Level.WARNING, "Cannot refresh " + file + " within " + dir);
            return;
        }

        final int dir_len = dir.getNameCount();
        final File sub = new File(item.getValue().file, file.getName(dir_len).toString());
        logger.log(Level.FINE, () -> "Looking to refresh " + sub + " in " + dir);

        for (TreeItem<FileInfo> child : item.getChildren())
            if (sub.equals(child.getValue().file))
                refreshTreeItem(child, file);
    }


    private void assertTreeDoesntContain(final TreeItem<FileInfo> item, final Path file)
    {
        final Path dir = item.getValue().file.toPath();
        logger.log(Level.FINE, () -> "Does " + dir + " still contain " + file + "?");
        if (! file.startsWith(dir))
        {
            logger.log(Level.FINE, "Can't!");
            return;
        }

        final int dir_len = dir.getNameCount();
        final File sub = new File(item.getValue().file, file.getName(dir_len).toString());
        for (TreeItem<FileInfo> child : item.getChildren())
            if (sub.equals(child.getValue().file))
            {
                // Found file or sub path to it..
                if (sub.isDirectory())
                    assertTreeDoesntContain(child, file);
                else
                {   // Found the file still listed as a child of 'item',
                    // so refresh 'item'
                    logger.log(Level.FINE, () -> "Forcing refresh of " + dir + " to hide " + sub);
                    Platform.runLater(() -> ((FileTreeItem)item).forceRefresh());
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
            alert.setHeaderText(Messages.OpenAlert1 + file + Messages.OpenAlert2);
            DialogHelper.positionDialog(alert, treeView, -300, -200);
            alert.showAndWait();
        }
    }

    /** Try to open all the currently selected resources */
    private void openSelectedResources()
    {
        treeView.selectionModelProperty()
                .getValue()
                .getSelectedItems()
                .forEach(item ->
        {
            if (item.isLeaf())
                openResource(item.getValue().file, null);
        });
    }

    @FXML
    public void initialize() {
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Create table columns
        final TreeTableColumn<FileInfo, File> name_col = new TreeTableColumn<>(Messages.ColName);
        name_col.setPrefWidth(200);
        name_col.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getValue().file));
        name_col.setCellFactory(info -> new FileTreeCell());
        name_col.setComparator(FileTreeItem.fileTreeItemComparator);
        treeView.getColumns().add(name_col);

        // Linux (Gnome) and Mac file browsers list size before time
        final TreeTableColumn<FileInfo, Number> size_col = new TreeTableColumn<>(Messages.ColSize);
        size_col.setCellValueFactory(p -> p.getValue().getValue().size);
        size_col.setCellFactory(info -> new FileSizeCell());
        treeView.getColumns().add(size_col);

        final TreeTableColumn<FileInfo, String> time_col = new TreeTableColumn<>(Messages.ColTime);
        time_col.setCellValueFactory(p -> p.getValue().getValue().time);
        treeView.getColumns().add(time_col);

        // This would cause columns to fill table width,
        // but _always_ does that, not allowing us to restore
        // saved widths from memento:
        // treeView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        // Last column fills remaining space

        final InvalidationListener resize = prop ->
        {
            // Available with, less space used for the TableMenuButton '+' on the right
            // so that the up/down column sort markers remain visible
            double available = treeView.getWidth() - 10;
            if (name_col.isVisible())
            {
                // Only name visible? Use the space!
                if (!size_col.isVisible() && !time_col.isVisible())
                    name_col.setPrefWidth(available);
                else
                    available -= name_col.getWidth();
            }
            if (size_col.isVisible())
            {
                if (! time_col.isVisible())
                    size_col.setPrefWidth(available);
                else
                    available -= size_col.getWidth();
            }
            if (time_col.isVisible())
                time_col.setPrefWidth(available);
        };
        treeView.widthProperty().addListener(resize);
        name_col.widthProperty().addListener(resize);
        size_col.widthProperty().addListener(resize);
        name_col.visibleProperty().addListener(resize);
        size_col.visibleProperty().addListener(resize);
        time_col.visibleProperty().addListener(resize);

        // Allow users to show/hide columns
        treeView.setTableMenuButtonVisible(true);

        // Prepare ContextMenu items
        open.setOnAction(event -> openSelectedResources());
        contextMenu.getItems().addAll(open, openWith);

        treeView.setOnKeyPressed(this::handleKeys);
    }

    TreeTableView<FileInfo> getView()
    {
        return treeView;
    }

    private void handleKeys(final KeyEvent event)
    {
        switch(event.getCode())
        {
        case ENTER: // Open
        {
            openSelectedResources();
            event.consume();
            break;
        }
        case F2: // Rename file
        {
            final ObservableList<TreeItem<FileInfo>> items = treeView.selectionModelProperty().getValue().getSelectedItems();
            if (items.size() == 1)
            {
                final TreeItem<FileInfo> item = items.get(0);
                if (item.isLeaf())
                    new RenameAction(treeView, item).fire();
            }
            event.consume();
            break;
        }
        case DELETE: // Delete
        {
            final ObservableList<TreeItem<FileInfo>> items = treeView.selectionModelProperty().getValue().getSelectedItems();
            if (items.size() > 0)
                new DeleteAction(treeView, items).fire();
            event.consume();
            break;
        }
        case C: // Copy
        {
            if (event.isShortcutDown())
            {
                final ObservableList<TreeItem<FileInfo>> items = treeView.selectionModelProperty().getValue().getSelectedItems();
                new CopyPath(items).fire();
                event.consume();
            }
            break;
        }
        case V: // Paste
        {
            if (event.isShortcutDown())
            {
                TreeItem<FileInfo> item = treeView.selectionModelProperty().getValue().getSelectedItem();
                if (item == null)
                    item = treeView.getRoot();
                else if (item.isLeaf())
                    item = item.getParent();
                new PasteFiles(treeView, item).fire();
                event.consume();
            }
            break;
        }
        case ESCAPE: // De-select
            treeView.selectionModelProperty().get().clearSelection();
            break;
        default:
            if ((event.getCode().compareTo(KeyCode.A) >= 0  &&  event.getCode().compareTo(KeyCode.Z) <= 0) ||
                (event.getCode().compareTo(KeyCode.DIGIT0) >= 0  &&  event.getCode().compareTo(KeyCode.DIGIT9) <= 0))
            {
                // Move selection to first/next file that starts with that character
                final String ch = event.getCode().getChar().toLowerCase();

                final TreeItem<FileInfo> selected = treeView.selectionModelProperty().getValue().getSelectedItem();
                final ObservableList<TreeItem<FileInfo>> siblings;
                int index;
                if (selected != null)
                {   // Start after the selected item
                    siblings = selected.getParent().getChildren();
                    index = siblings.indexOf(selected);
                }
                else if (!treeView.getRoot().getChildren().isEmpty())
                {   // Start at the root
                    siblings = treeView.getRoot().getChildren();
                    index = -1;
                }
                else
                    break;
                for (++index;  index < siblings.size();  ++index)
                    if (siblings.get(index).getValue().file.getName().toLowerCase().startsWith(ch))
                    {
                        treeView.selectionModelProperty().get().clearSelection();
                        treeView.selectionModelProperty().get().select(siblings.get(index));
                        break;
                    }
            }
        }
    }

    @FXML
    public void createContextMenu(ContextMenuEvent e) {
        final ObservableList<TreeItem<FileInfo>> selectedItems = treeView.selectionModelProperty().getValue().getSelectedItems();

        contextMenu.getItems().clear();

        if (selectedItems.isEmpty())
        {
            // Create directory at root
            contextMenu.getItems().addAll(new CreateDirectoryAction(treeView, treeView.getRoot()));
            // Paste files at root
            if (Clipboard.getSystemClipboard().hasFiles())
                contextMenu.getItems().addAll(new PasteFiles(treeView, treeView.getRoot()));
        }
        else
        {
            // allMatch() would return true for empty, so only check if there are items
            if (selectedItems.stream().allMatch(item -> item.isLeaf()))
                contextMenu.getItems().add(open);

            // If just one entry selected, check if there are multiple apps from which to select
            if (selectedItems.size() == 1)
            {
                final File file = selectedItems.get(0).getValue().file;
                final URI resource = ResourceParser.getURI(file);
                final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
                if (applications.size() > 0)
                {
                    openWith.getItems().clear();
                    for (AppResourceDescriptor app : applications)
                    {
                        final MenuItem open_app = new MenuItem(app.getDisplayName());
                        final URL icon_url = app.getIconURL();
                        if (icon_url != null)
                            open_app.setGraphic(new ImageView(icon_url.toExternalForm()));
                        open_app.setOnAction(event -> app.create(resource));
                        openWith.getItems().add(open_app);
                    }
                    contextMenu.getItems().add(openWith);
                }

                if (file.isDirectory())
                {
                    contextMenu.getItems().add(new SetBaseDirectory(file, this::setRoot));
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }

                SelectionService.getInstance().setSelection(this, Arrays.asList(file));
                List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
                supported.stream().forEach(action -> {
                    MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                    menuItem.setOnAction((ee) -> {
                        try {
                            action.call(SelectionService.getInstance().getSelection());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Failed to execute " + action.getName() + " from file browser.", ex);
                        }
                    });
                    contextMenu.getItems().add(menuItem);
                });
                if(!supported.isEmpty()){
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }
            }

            contextMenu.getItems().add(new CopyPath(selectedItems));
            contextMenu.getItems().add(new SeparatorMenuItem());
        }
        if (selectedItems.size() >= 1)
        {
            final TreeItem<FileInfo> item = selectedItems.get(0);
            final boolean is_file = item.isLeaf();

            if (selectedItems.size() == 1)
            {
                if (is_file)
                {
                    // Create directory within the _parent_
                    contextMenu.getItems().addAll(new CreateDirectoryAction(treeView, item.getParent()));

                    // Plain file can be duplicated, directory can't
                    contextMenu.getItems().add(new DuplicateAction(treeView, item));
                }
                else
                    // Within a directory, a new directory can be created
                    contextMenu.getItems().addAll(new CreateDirectoryAction(treeView, item));
                contextMenu.getItems().addAll(new RenameAction(treeView,  selectedItems.get(0)));

                if (Clipboard.getSystemClipboard().hasFiles())
                    contextMenu.getItems().addAll(new PasteFiles(treeView, selectedItems.get(0)));
            }

            contextMenu.getItems().add(new DeleteAction(treeView, selectedItems));
            contextMenu.getItems().add(new SeparatorMenuItem());

            if (is_file)
                contextMenu.getItems().add(new RefreshAction(treeView, item.getParent()));
            else
                contextMenu.getItems().add(new RefreshAction(treeView, item));
        }

        if (selectedItems.size() == 1){
            contextMenu.getItems().addAll(new PropertiesAction(treeView,  selectedItems.get(0)));
        }
        contextMenu.show(treeView.getScene().getWindow(), e.getScreenX(), e.getScreenY());
    }

    @FXML
    public void handleMouseClickEvents(final MouseEvent event)
    {
        if (event.getClickCount() == 2)
            openSelectedResources();
    }

    @FXML
    public void setNewRoot() {
        Path p = Paths.get(path.getText());
        setRoot(p.toFile());
    }

    /** @param directory Desired root directory */
    public void setRoot(final File directory)
    {
        monitor.setRoot(directory);
        path.setText(directory.toString());
        treeView.setRoot(new FileTreeItem(monitor, directory));
    }

    /** @return Root directory */
    public File getRoot()
    {
        return treeView.getRoot().getValue().file;
    }


    @FXML
    public void home() {
        setRoot(FileBrowserApp.default_root);
    }

    @FXML
    public void browseNewRoot() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(Messages.BrowserRootTitle);
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
