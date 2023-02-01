package org.phoebus.applications.eslog.util;

/**
 * JMS Connection Listener Interface
 * <p>
 * Some JMS implementations like Apache ActiveMQ might offer a connection
 * listener, but only when directly using the ActiveMQ API.
 * <p>
 * This interface removes the direct ActiveMQ dependency from client code.
 *
 * @see JMSConnectionFactory#addListener
 * @author Kay Kasemir
 */
public interface JMSConnectionListener
{
    /** Invoked when the connection to the JMS server is interrupted */
    public void linkDown();

    /**
     * Invoked when the connection to the JMS server is re-connected (resumed)
     * 
     * @param server
     *            Name of the JMS server to which we are connected.
     */
    public void linkUp(String server);
}