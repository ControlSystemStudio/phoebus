package org.phoebus.ui.spi;

import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.application.ContextMenuHelper;

import javafx.scene.Node;
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
     * Allow the context menu action to be enabled/disabled
     *
     * @return true if enabled
     */
    public default boolean isEnabled()
    {
        return true;
    }

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
     * @return Selection type for which this entry should be displayed
     */
    public Class<?> getSupportedType();

    /**
     * Invoke the action associated with this context menu contribution
     * @throws Exception on error
     */
    public default void call() throws Exception
    {
        throw new UnsupportedOperationException(getName() + ".call() Is not implemented.");
    }

    /**
     * Invoke the context menu
     *
     * @param selection The selection to be used to execute this action
     * @throws Exception on error
     */
    public default void call(Selection selection) throws Exception
    {
        throw new UnsupportedOperationException(getName() + ".call(selection) Is not implemented.");
    };

    /**
     * Invoke the context menu, with a calling Node
     * 
     * @param parent the parent Node
     * @throws Exception on error
     */
    public default void call(Node parent) throws Exception
    {
        throw new UnsupportedOperationException(getName() + ".call(node) Is not implemented.");
    };

    /**
     * Invoke the context menu, with a selection and a calling Node
     * 
     * @param parent the parent Node
     * @param selection The selection to be used to execute this action
     * @throws Exception on error
     */
    public default void call(Node parent, Selection selection) throws Exception
    {
        throw new UnsupportedOperationException(getName() + ".call(node, selection) Is not implemented.");
    };


}
