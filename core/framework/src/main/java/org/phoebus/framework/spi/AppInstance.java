package org.phoebus.framework.spi;

import java.awt.geom.Rectangle2D;
import java.util.Optional;
import org.phoebus.framework.persistence.Memento;

public interface AppInstance {

    public AppDescriptor getAppDescriptor();

    /** Is this application instance transient,
     *  i.e. it should _not_ be saved on shutdown?
     *  @return <code>true</code> for transient instance
     */
    public default boolean isTransient()
    {
        return false;
    }

    /**
     * If application instance that was just opened via call to `open` is an
     * instance re-opened from a previous run, framework will call this method to
     * allow instance to restore state from previously saved memento content. us
     * instance from memento
     *
     * @param memento
     *            Memento where previous application instance saved state
     */
    public default void restore(Memento memento) {
        // Default does nothing
    }

    /**
     * When shutting down, the framework calls this method to allow application
     * instances to save their state for restoral on restart.
     *
     * @param memento
     *            Memento to store state
     */
    public default void save(Memento memento) {
        // Default does nothing
    }

    /**
     * Get possible window size and position for the application instance, if defined
     * (for example, Display Runtime may want the window to be the size defined in the display)
     *
     * (Note: Use Rectangle2D to avoid loading AWT toolkit; we only need a data container)
     *
     * @return Empty optional if no dimension is specified by the application instance.
     */
    public default Optional<Rectangle2D> getPositionAndSizeHint() { return Optional.empty(); }

}
