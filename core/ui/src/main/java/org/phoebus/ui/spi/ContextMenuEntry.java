package org.phoebus.ui.spi;

import java.io.InputStream;
import java.util.List;

import org.phoebus.framework.selection.Selection;

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
     * @return Stream for icon, or <code>null</code>
     */
    public default InputStream getIcon()
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
