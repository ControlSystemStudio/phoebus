package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
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
            final AlarmTreeItem<?> item = clipboard.get();

            // Get new name, prompting user if any child of the new parent already has the same name
            String newName = item.getName();
            if (parent.getChildren().stream().anyMatch(child -> child.getName().equals(item.getName()))) {
                // Item already exists in the parent node we're copying to; prompt for rename
                final RenameDialog dialog = new RenameDialog(parent, item.getName());
                dialog.setHeaderText(MessageFormat.format(Messages.itemAlreadyExists, parent.getName(), item.getName()));
                DialogHelper.positionDialog(dialog, node, -100, -50);
                final Optional<String> name = dialog.showAndWait();
                if (name.isPresent()) {
                    newName = name.get();
                } else {
                    return;
                }
            }

            final String finalNewName = newName; // lambda variables must be final
            JobManager.schedule("Paste alarm tree item", _ -> {
                try {
                    final String newPath = AlarmTreePath.makePath(parent.getPathName(), finalNewName);
                    AlarmTreeHelper.moveItem(model, item, newPath, isCut);
                } catch (final Exception ex) {
                    ExceptionDetailsErrorDialog.openError(Messages.error, Messages.pasteItemFailed, ex);
                }
            });
        });
    }
}
