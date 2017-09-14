package org.phoebus.framework.spi;

import org.phoebus.framework.persistence.Memento;

/**
 * Basic interface for defining phoebus applications via java services
 *
 * <p>
 * The framework creates one instance for each application. The
 * <code>start()</code> and <code>stop()</code> methods allow an application to
 * manage global resources.
 *
 *
 * @author Kunal Shroff
 *
 */
public interface AppDescriptor {

    /**
     * Get the application name
     *
     * @return the name of the application
     */
    public String getName();

    /**
     * Get the applications display name
     *
     * @return the Display Name of the application
     */
    public default String getDisplayName() {
        return getName();
    }

    /**
     * Create the resources (connects, load libraries,...) required by this
     * particular application
     */
    public default void start() {
        // Default does nothing
    }

    /**
     * Open the application without any specific resources
     */
    public void open();

    /**
     * If application instance that was just opened
     * via call to `open` is an instance re-opened
     * from a previous run,
     * framework will call this method to allow
     * instance to restore state from previously
     * saved memento content. us instance from memento
     *
     * @param memento Memento where previous application instance saved state
     */
    public default void restore(Memento memento) {
        // Default does nothing
    }

    /**
     * When shutting down, the framework calls
     * this method to allow application instances
     * to save their state for restoral on restart.
     *
     * @param memento Memento to store state
     */
    public default void save(Memento memento) {
        // Default does nothing
    }

    /**
     * Cleanup the resources used by this application, also perform the action of
     * storing application state
     */
    public default void stop() {
        // Default does nothing

    }
}
