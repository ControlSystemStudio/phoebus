package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import java.text.MessageFormat;
import java.util.Optional;


@SuppressWarnings("nls")
public class PasteItemAction extends MenuItem
{

    /** @param node Node to position dialog
     *  @param model {@link AlarmClient}
     *  @param parent Parent to paste the item into
     *  @param clipboard Property containing the clipboard
     *  @param isCut Property indicating whether the clipboard item should be removed when pasted
     */
    public PasteItemAction(
            final Node node,
            final AlarmClient model,
            final AlarmTreeItem<?> parent,
            final SimpleObjectProperty<AlarmTreeItem<?>> clipboard,
            final boolean isCut)
    {
        super(Messages.pasteItem, ImageCache.getImageView(AlarmSystem.class, "/icons/paste.png"));

        setDisable(clipboard.get() == null);

        setOnAction(event -> {
            AlarmTreeItem<?> item = clipboard.get();
            final String itemName = item.getName();

            // Cross-config cut/paste is forbidden. (Why? User cuts from config A -> switches to
            // config B -> pastes to config B -> config A is no longer loaded so how can we remove
            // the old item?)
            final String itemConfig = AlarmTreePath.splitPath(item.getPathName())[0];
            final String modelConfig = AlarmTreePath.splitPath(model.getRoot().getPathName())[0];
            if (isCut && !itemConfig.equals(modelConfig)) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                DialogHelper.positionDialog(alert, node, -100, -50);
                alert.setTitle(Messages.pasteItemFailed);
                alert.setHeaderText(Messages.cutPasteAcrossConfigs);
                alert.show();
                return;
            }

            // Cutting+pasting a node into one of its children is forbidden for obvious reasons
            if (parent.getPathName().startsWith(item.getPathName() + "/")) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                DialogHelper.positionDialog(alert, node, -100, -50);
                alert.setTitle(Messages.pasteItemFailed);
                alert.setHeaderText(Messages.cutPasteIntoChild);
                alert.show();
                return;
            }

            // Get new name
            final RenameDialog renameDialog = new RenameDialog(model, parent, item);
            renameDialog.setHeaderText(MessageFormat.format(Messages.enterNewName, parent.getName(), item.getName()));
            DialogHelper.positionDialog(renameDialog, node, -100, -50);
            final Optional<String> name = renameDialog.showAndWait();
            if (!name.isPresent()) {
                return;
            }
            final String newName = name.get();

            // Prompt user whether they want to paste the children too
            if (item instanceof final AlarmClientNode alarmNode && !alarmNode.getChildren().isEmpty()) {
                final Dialog<ButtonType> dialog = new Dialog<>();
                DialogHelper.positionDialog(dialog, node, -100, -50);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
                dialog.setTitle(Messages.copyChildrenDialogTitle);
                dialog.setHeaderText(Messages.copyChildrenDialogHeader);

                final ButtonType result = dialog.showAndWait().orElse(null);
                if (result == null || result == ButtonType.CANCEL) {
                    return;
                }
                if (result == ButtonType.NO) {
                    item = alarmNode.createDetachedCopy();
                }
            }

            // Finally paste the item (on a separate thread of course)
            // lambda variables must be final
            final AlarmTreeItem<?> finalItem = item;
            JobManager.schedule("Paste alarm tree item", _ -> {
                try {
                    final String newPath = AlarmTreePath.makePath(parent.getPathName(), newName);
                    AlarmTreeHelper.moveItem(model, finalItem, newPath, isCut);
                } catch (final Exception ex) {
                    ExceptionDetailsErrorDialog.openError(Messages.error, Messages.pasteItemFailed, ex);
                }
            });
        });
    }
}
