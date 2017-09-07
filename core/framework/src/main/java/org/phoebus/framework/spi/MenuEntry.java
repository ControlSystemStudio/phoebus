package org.phoebus.framework.spi;

import java.util.concurrent.Callable;

import org.phoebus.framework.workbench.MenuEntryService;

/**
 * An interface used by the pheobus {@link MenuEntryService} to discover contributions
 * to the main menu.
 * 
 * @author Kunal Shroff
 *
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
}
