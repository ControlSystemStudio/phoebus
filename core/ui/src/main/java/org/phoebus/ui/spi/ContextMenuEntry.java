package org.phoebus.ui.spi;

import java.util.List;

import org.phoebus.framework.selection.Selection;

import javafx.scene.image.Image;

/**
 * Context menu entry service interface
 *
 * @author Kunal Shroff
 * @param <V>
 *
 */
public interface ContextMenuEntry<V> {

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
    public List<Class> getSupportedTypes();

    /**
     * Invoke the context menu
     *
     * @param (TODO replace with the use of selectionService.getCurrentSelection(); ) selection Current selection
     * @return (TODO What does it return?? )
     * @throws Exception on error
     */
    public V callWithSelection(Selection selection) throws Exception;
}
