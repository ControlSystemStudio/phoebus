package org.phoebus.applications.alarm.model;

/**
 * Exception thrown when attempting to construct an invalid alarm tree path.
 * <p>
 * Paths or path elements that start with more than one leading slash are not allowed.
 * A single leading slash is permitted for backward compatibility and for
 * representing absolute paths, but multiple leading slashes indicate an
 * invalid or ambiguous path specification.
 */
public class AlarmTreePathException extends IllegalArgumentException {
    public AlarmTreePathException(String message) {
        super(message);
    }
}
