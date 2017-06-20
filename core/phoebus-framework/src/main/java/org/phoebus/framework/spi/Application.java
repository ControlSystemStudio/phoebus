package org.phoebus.framework.spi;

/**
 * Basic interface for defining phoebus applications via java services
 * 
 * @author Kunal Shroff
 *
 */
public interface Application {

    /**
     * Get the application name
     * 
     * @return the name of the application
     */
    public String getName();

    /**
     * Create the resources (connects, load libraries,...) required by this particular application
     */
    public void start();

    /**
     * Cleanup the resources used by this application, also perform the action of
     * storing application state
     */
    public void stop();

}
