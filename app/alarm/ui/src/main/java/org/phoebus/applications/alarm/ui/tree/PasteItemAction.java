package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
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
import java.util.Set;
import java.util.stream.Collectors;


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
            final TreeItem<AlarmTreeItem<?>> viewParent,
            final SimpleObjectProperty<AlarmTreeItem<?>> clipboard,
            final boolean isCut)
    {
        super(Messages.pasteItem, ImageCache.getImageView(AlarmSystem.class, "/icons/paste.png"));

        final AlarmTreeItem<?> parent = viewParent.getValue();

        setDisable(clipboard.get() == null || isCut && parent.getPathName().equals(clipboard.get().getParent().getPathName()));

        setOnAction(event -> {
            final AlarmTreeItem<?> item = clipboard.get();
            final String itemName = item.getName();

            final String itemConfig = AlarmTreePath.splitPath(item.getPathName())[0];
            final String modelConfig = AlarmTreePath.splitPath(model.getRoot().getPathName())[0];
            final boolean isCrossConfig = !itemConfig.equals(modelConfig);

            // Cross-config cut/paste is forbidden. (Why? User cuts from config A -> switches to
            // config B -> pastes to config B -> config A is no longer loaded so how can we remove
            // the old item?)
            if (isCut && isCrossConfig) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                DialogHelper.positionDialog(alert, node, -100, -50);
                alert.setTitle(Messages.pasteItemFailed);
                alert.setHeaderText(Messages.cutPasteAcrossConfigs);
                alert.show();
                return;
            }

            // Cutting+pasting a node into one of its children is forbidden for obvious reasons
            final boolean isPasteIntoSelf = parent.getPathName().equals(item.getPathName());
            final boolean isPasteIntoChild = parent.getPathName().startsWith(item.getPathName() + "/");
            if (isCut && (isPasteIntoSelf || isPasteIntoChild)) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                DialogHelper.positionDialog(alert, node, -100, -50);
                alert.setTitle(Messages.pasteItemFailed);
                alert.setHeaderText(Messages.cutPasteIntoChild);
                alert.show();
                return;
            }

            // Figure out if this item needs renaming
            final boolean needsRename;
            final Set<String> allPvsInNewConfig = AlarmTreeHelper.getLeafNames(model.getRoot());
            if (item instanceof AlarmClientNode) {
                // Needs to be renamed if new name matches any child
                needsRename = parent.getChildren().stream().anyMatch(child -> item.getName().equals(child.getName()));
            } else {
                // Needs to be renamed if pasting would result in a duplicate PV
                needsRename = !isCut && allPvsInNewConfig.contains(item.getName());
            }

            // Get final name
            final String finalName;
            if (needsRename) {
                final RenameDialog renameDialog = new RenameDialog(model, parent, item);
                renameDialog.setHeaderText(MessageFormat.format(Messages.enterNewName, parent.getName(), item.getName()));
                DialogHelper.positionDialog(renameDialog, node, -100, -50);
                final Optional<String> name = renameDialog.showAndWait();
                if (!name.isPresent()) {
                    return;
                }
                finalName = name.get();
            } else {
                finalName = item.getName();
            }

            // Special case: if a node is pasted across configs, some of the PVs in that node
            // may already exist. But the user still may have intended to copy across all the
            // PVs and so we should not just fail silently. Best-effort copying is unlikely
            // to be helpful here, and likely to cause unintended behaviour, so we do not offer
            // that option and just display an error instead.
            if (isCrossConfig && item instanceof AlarmClientNode) {
                final Set<String> duplicatePvs = AlarmTreeHelper.getLeafNames(item);
                duplicatePvs.retainAll(allPvsInNewConfig);

                if (!duplicatePvs.isEmpty()) {
                    final Alert alert = new Alert(Alert.AlertType.ERROR);
                    DialogHelper.positionDialog(alert, node, -100, -50);
                    alert.setTitle(Messages.pasteItemFailed);
                    alert.setHeaderText(Messages.nodePvsAlreadyExist);

                    final VBox pvPane = new VBox();
                    for (final String pv : duplicatePvs) {
                        pvPane.getChildren().add(new Label(pv));
                    }
                    final ScrollPane scrollPane = new ScrollPane(pvPane);
                    scrollPane.setMaxHeight(400.0);

                    alert.getDialogPane().setContent(scrollPane);
                    alert.show();
                    return;
                }
            }

            // Expand the node which we're adding to in anticipation of new children being added
            viewParent.setExpanded(true);

            // Finally paste the item (on a separate thread of course)
            final AlarmTreeItem<?> finalItem = item; // lambda variables must be final
            JobManager.schedule("Paste alarm tree item", _ -> {
                try {
                    final String newPath = AlarmTreePath.makePath(parent.getPathName(), finalName);
                    model.sendItemConfigurationUpdate(newPath, item);
                    if (isCut) {
                        // If we're cutting, there's never any risk of duplicate PVs so we can copy all the children too
                        AlarmTreeHelper.rebuildTree(model, item, newPath);
                        model.removeComponent(item);
                    } else if (isCrossConfig && item instanceof AlarmClientNode) {
                        // Cross-config pasting should copy all the children
                        AlarmTreeHelper.rebuildTree(model, item, newPath);
                    }
                } catch (final Exception ex) {
                    ExceptionDetailsErrorDialog.openError(Messages.error, Messages.pasteItemFailed, ex);
                }
            });
        });
    }
}
