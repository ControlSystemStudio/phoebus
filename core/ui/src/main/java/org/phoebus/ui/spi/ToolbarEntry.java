package org.phoebus.ui.spi;

import java.util.concurrent.Callable;

import org.phoebus.ui.application.ToolbarEntryService;

import javafx.scene.image.Image;

/**
 * An interface describing the contributions to the main toolbar.
 *
 * <p>Used by the {@link ToolbarEntryService}
 *
 * @author Kunal Shroff
 *
 */
public interface ToolbarEntry extends Callable<Void> {

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
}
