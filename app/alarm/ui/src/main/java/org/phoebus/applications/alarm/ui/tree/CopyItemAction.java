package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.javafx.ImageCache;


@SuppressWarnings("nls")
public class CopyItemAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param clipboard Property containing the clipboard
     *  @param isCut Property indicating whether the clipboard item should be removed when pasted
     *  @param item Parent item in alarm tree
     */
    public CopyItemAction(final Node node, final SimpleObjectProperty<AlarmTreeItem<?>> clipboard, final SimpleBooleanProperty isCut, final AlarmTreeItem<?> item)
    {
        super("Copy Item", ImageCache.getImageView(AlarmSystem.class, "/icons/copy.png"));
        setOnAction(event -> {
            clipboard.set(item);
            isCut.set(false);
        });
    }
}
