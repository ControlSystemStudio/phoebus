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

    public LogClient getLogClient();

    public LogClient getLogClient(Object authToken);

}
