package org.phoebus.logging;

import java.time.Instant;

/**
 * An interface describing a log entry
 *
 * @author Kunal Shroff
 *
 */
public interface LogEntry {

    public String getText();

    public String getOwner();

    public Instant getTime();

    public Instant getModifyTime();

}
