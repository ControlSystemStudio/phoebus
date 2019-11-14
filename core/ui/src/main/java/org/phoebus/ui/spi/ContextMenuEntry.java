package org.phoebus.ui.spi;

import java.util.List;

import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.application.ContextMenuHelper;

import javafx.scene.image.Image;

/**
 * Context menu entry service interface
 *
 * <p>Applications can use this to register context menu
 * entries that are applicable to certain types found
 * in the current selection.
 *
 * <p>Applications with context menus to their UI
 * should use the {@link ContextMenuHelper} to add
 * SPI-provided additions to the menu.
 *
 * @author Kunal Shroff
 */
public interface ContextMenuEntry {

    /**
     * The display name of the context menu entry
     *
     * @return the display name
     */
    public String getName();

    /**
     * Icon for the context menu
     *
     * <p>Implementation should always returns the same, cached, icon,
     * unless it needs to provide a different looking icon based
     * on for example its state.
     *
     * @return Icon, or <code>null</code> if no icon desired
     */
    public default Image getIcon()
    {
        return null;
    }

    /**
     * @return Selection types for which this entry should be displayed
     */
    public List<Class<?>> getSupportedTypes();

    /**
     * Invoke the context menu
     *
     * @param selection Current selection
     * @throws Exception on error
     */
    public void callWithSelection(Selection selection) throws Exception;
}
