package org.phoebus.framework.spi;

import java.net.URL;

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
     * Icon for the application
     *
     * @return Icon URL, or <code>null</code> if no icon desired
     */
    public default URL getIconURL() {
        return null;
    }

    /**
     * Create the resources (connects, load libraries,...) required by this
     * particular application
     */
    public default void start() {
        // Default does nothing
    }

    /**
     * Create an instance of the application without any specific resources
     */
    public AppInstance create();

    /**
     * Cleanup the resources used by this application, also perform the action of
     * storing application state
     */
    public default void stop() {
        // Default does nothing

    }
}
