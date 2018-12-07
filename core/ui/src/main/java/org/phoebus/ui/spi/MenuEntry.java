package org.phoebus.ui.spi;

import java.util.concurrent.Callable;

import org.phoebus.ui.application.MenuEntryService;

import javafx.scene.image.Image;

/**
 * An interface used by the pheobus {@link MenuEntryService} to discover contributions
 * to the main menu.
 *
 * @author Kunal Shroff
 */
public interface MenuEntry extends Callable<Void> {

    /**
     * The name of the menu
     *
     * @return String display name of the menu
     */
    public String getName();

    /**
     * Returns a dot separated string representing the location of the menu entry
     * under the main application menu
     *
     * e.g. utility.logging
     *
     * @return String menu path
     */
    public String getMenuPath();

    /**
     * Icon for the menu
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
}
