package org.phoebus.applications.filebrowser;

import static org.phoebus.applications.filebrowser.FileBrowser.logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

@SuppressWarnings("nls")
final class FileTreeCell extends TreeTableCell<FileInfo, File> {
    static final Image file_icon = ImageCache.getImage(ImageCache.class, "/icons/file_obj.png");
    static final Image folder_icon = ImageCache.getImage(ImageCache.class, "/icons/fldr_obj.png");
    static final Image newDisplayIcon = ImageCache.getImage(ImageCache.class, "/icons/display.png");

    private static final Border BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
                                                    new CornerRadii(5.0), BorderStroke.THIN));

    public FileTreeCell()
    {
        enableDragDrop();
    }

    private void enableDragDrop()
    {
        // Allow dragging this file
        setOnDragDetected(event ->
        {
            final File file = getItem();
            if (file != null)
            {
                // Drag not just this file, but all selected files
                final List<File> files = new ArrayList<>();
                files.add(file);
                for (TreeItem<FileInfo> sel : getTreeTableView().getSelectionModel().getSelectedItems())
                {
                    final File other = sel.getValue().file;
                    if (! files.contains(other))
                        files.add(other);
                }

                logger.log(Level.FINE, "Dragging " + files);
                final ClipboardContent content = new ClipboardContent();
                content.putFiles(files);
                content.putString(files.stream().map(File::getAbsolutePath).collect(Collectors.joining(", ")));

                final Dragboard db = startDragAndDrop(getTransferMode(event));
                db.setContent(content);
            }
            event.consume();
        });

        // Has file been moved elsewhere?
        setOnDragDone(event ->
        {
            final File file = getItem();

            if (event.getTransferMode() == TransferMode.MOVE  &&
                file != null)
            {
                logger.log(Level.FINE, "Drag (MOVE) completed. Deleting moved " + file);
                // Might want to check if file.exists() in case move failed,
                // but actual move is performed in background, so right now file
                // might still be present...
                final TreeItem<FileInfo> deleted_item = getTreeTableRow().getTreeItem();
                deleted_item.getParent().getChildren().remove(deleted_item);
            }
            else
                logger.log(Level.FINE, "Drag (COPY) completed.");

            event.consume();
        });

        // Indicate if file may be dropped
        // File(s) may not be dropped if drag board contains the drop target (see Github issue #836)
        setOnDragOver(event ->
        {
            final File file = getItem();
            if (file != null && event.getDragboard().hasFiles() && !event.getDragboard().getFiles().contains(file))
            {
                event.acceptTransferModes(event.getTransferMode());
                setBorder(BORDER);
            }
            event.consume();
        });

        setOnDragExited(event ->
        {
            setBorder(null);
            event.consume();
        });

        // A file has been dropped into this dir, or this file's directory
        setOnDragDropped(event ->
        {
            TreeItem<FileInfo> target_item = getTreeTableRow().getTreeItem();

            if (target_item.getValue() != null && !target_item.getValue().file.isDirectory())
                target_item = target_item.getParent();
            if (target_item.getValue() != null) {
                final Dragboard db = event.getDragboard();
                if (db.hasFiles())
                    for (File file : db.getFiles()) {
                        logger.log(Level.FINE, "Dropped " + file + " onto " + target_item.getValue() + " via " + event.getTransferMode());
                        move_or_copy(file, target_item, event.getTransferMode());
                    }
            }

            event.setDropCompleted(true);
            event.consume();
        });
    }

    /** @param file File to move or copy
     *  @param target_item Destination directory's tree item
     */
    private void move_or_copy(final File file, final TreeItem<FileInfo> target_item, final TransferMode transferMode)
    {
        final File dir = target_item.getValue().file;
        // Ignore NOP move
        if (file.getParentFile().equals(dir))
            return;

        JobManager.schedule(Messages.MoveOrCopyJobName, monitor ->
        {
            // System.out.println("Move " + file + " into " + dir);
            final File new_name = new File(dir, file.getName());

            final DirectoryMonitor mon = ((FileTreeItem)target_item).getMonitor();
            try
            {
                if (transferMode.equals(TransferMode.MOVE))
                    FileHelper.move(file, dir);
                else
                    FileHelper.copy(file, dir);
                Platform.runLater(() ->
                {
                    // System.out.println("Add tree item for " + new_name + " to " + target_item.getValue());
                    final ObservableList<TreeItem<FileInfo>> siblings = target_item.getChildren();
                    siblings.add(new FileTreeItem(mon, new_name));
                    FileTreeItem.sortSiblings(siblings);
                });
            }
            catch (Exception ex)
            {
                final TreeTableView<FileInfo> tree = getTreeTableView();
                ExceptionDetailsErrorDialog.openError(tree, Messages.MoveOrCopyAlertTitle,
                                                      MessageFormat.format(Messages.MoveOrCopyAlert, file, target_item.getValue()), ex);
                // Force full refresh
                Platform.runLater(() ->
                    tree.setRoot(new FileTreeItem(mon, tree.getRoot().getValue().file)) );
            }
        });
    }

    @Override
    protected void updateItem(final File file, final boolean empty) {
        super.updateItem(file, empty);

        if (empty || file == null || getTreeTableRow() == null || getTreeTableRow().getTreeItem() == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (getTreeTableRow().getTreeItem().getParent() == null) {
                // Root (actually hidden, so this is never called)
                setText(file.getAbsolutePath());
            } else {
                if (file.isDirectory())
                    setGraphic(new ImageView(folder_icon));
                else
                {
                    setGraphic(new ImageView(file_icon));
                    JobManager.schedule(Messages.LookupJobName, monitor -> lookup_icon(file));
                }
                setText(file.getName());
            }
        }
    }

    private void lookup_icon(final File file)
    {
        final URI resource = ResourceParser.getURI(file);
        final AppDescriptor app = ApplicationLauncherService.findApplication(resource, false, null);
        if (app == null)
            return;
        final URL icon_url = app.getIconURL();
        if (icon_url == null)
            return;
        final ImageView icon = ImageCache.getImageView(icon_url);
        if (icon != null)
            Platform.runLater(() -> setGraphic(icon));
    }

    /**
     * Determines the {@link TransferMode} based on the state of the modifier key.
     * This method must consider the
     * operating system as the identity of the modifier key varies (alt/option on Mac OS, ctrl on the rest).
     * @param event The mouse event containing information on key press.
     * @return {@link TransferMode#COPY} if modifier key is pressed, otherwise {@link TransferMode#MOVE}.
     */
    private TransferMode getTransferMode(MouseEvent event){
        if(event.isControlDown() && (PlatformInfo.is_linux || PlatformInfo.isWindows || PlatformInfo.isUnix)){
            return TransferMode.COPY;
        }
        else if(event.isAltDown() && PlatformInfo.is_mac_os_x){
            return TransferMode.COPY;
        }
        return TransferMode.MOVE;
    }
}