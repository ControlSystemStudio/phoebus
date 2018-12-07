package org.phoebus.ui.spi;

import javafx.scene.image.Image;

/**
 * An interface describing the contributions to the main toolbar.
 *
 * @author Kunal Shroff
 *
 */
public interface ToolbarEntry {

    /**
     * The name of the toolbar entry
     *
     * @return String display name of the toolbar entry
     */
    public String getName();

    /**
     * Optional icon
     * @return Image for icon or <code>null</code>
     */
    public default Image getIcon()
    {
        return null;
    }

    /**
     * Called by the UI framework to invoke the tool bar entry
     *
     * @throws Exception on error
     */
    public void call() throws Exception;

}
