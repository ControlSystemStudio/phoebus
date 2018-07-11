/**
 * 
 */
package org.phoebus.logbook;

/**
 * An interface describing a logbook associated with {@link LogEntry}s
 * 
 * @author Eric Berryman taken from shroffk
 * 
 */
public interface Logbook {

    /**
     * Get logbook name
     * @return logbook name
     */
    public String getName();

    /**
     * Get logbook owner
     * @return logbook owner
     */
    public String getOwner();

}
