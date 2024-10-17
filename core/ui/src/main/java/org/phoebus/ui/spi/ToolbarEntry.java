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

    /**
     * A unique id for the {@link ToolbarEntry}. Implementations <b>must</b> ensure that this
     * is indeed unique and immutable. In particular, it should be insensitive to localization and
     * customization of an app name.
     * @return Unique id of the {@link ToolbarEntry}.
     */
    String getId();
}
