package org.phoebus.logbook;

public interface Tag {

    /**
     * An interface representing a Tag associated with a {@link LogEntry}s
     * @author berryman from shroffk
     *
     */

    /**
     * Get Tag name
     * @return tag name
     */
    public String getName();

    /**
     * Get the Tag state
     * @return tag state
     */
    public String getState();

}
