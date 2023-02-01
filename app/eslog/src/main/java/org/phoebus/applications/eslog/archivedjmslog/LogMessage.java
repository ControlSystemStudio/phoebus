package org.phoebus.applications.eslog.archivedjmslog;

import java.time.Instant;

abstract public class LogMessage implements Comparable<LogMessage>
{
    @Override
    /**
     * Very simplistic default implementation. This function almost certainly
     * needs to be overridden.
     */
    public int compareTo(final LogMessage other)
    {
        return getTime().compareTo(other.getTime());
    }

    abstract public String getPropertyValue(final String id);

    /** Get time in millis since the epoch. */
    abstract public Instant getTime();
}
