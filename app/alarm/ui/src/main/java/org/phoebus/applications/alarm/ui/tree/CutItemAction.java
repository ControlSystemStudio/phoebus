package org.phoebus.applications.alarm.ui.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.javafx.ImageCache;


@SuppressWarnings("nls")
public class CutItemAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param clipboard Property containing the clipboard
     *  @param isCut Property indicating whether the clipboard item should be removed when pasted
     *  @param item Parent item in alarm tree
     */
    public CutItemAction(final Node node, final SimpleObjectProperty<AlarmTreeItem<?>> clipboard, final SimpleBooleanProperty isCut, final AlarmTreeItem<?> item)
    {
        super("Cut Item", ImageCache.getImageView(AlarmSystem.class, "/icons/cut.png"));
        setOnAction(event -> {
            clipboard.set(item);
            isCut.set(true);
        });
    }
}
