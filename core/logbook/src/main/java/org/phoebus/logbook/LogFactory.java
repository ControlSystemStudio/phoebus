package org.phoebus.logbook;

/**
 * An interface for registering log factories, each factory returns and instance
 * of a client implementing {@link LogClient}
 * 
 * @author Kunal Shroff
 *
 */
public interface LogFactory {

    public String getId();

    /** Retrieve a read only log client */
    public LogClient getLogClient();

    /** Retrieve a log client with authentication token */
    public LogClient getLogClient(Object authToken);

}
