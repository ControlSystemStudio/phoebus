package org.phoebus.archive.reader.appliance;

/**
 *
 * <code>ArchiveApplianceException</code> describes an unexpected behaviour
 * of the archiver appliance. The exception is thrown by the {@link ApplianceValueIterator}
 * when it encounters problems with reading the archived data.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ArchiverApplianceException extends Exception {
    private static final long serialVersionUID = 819955164823427944L;

    /**
     * Constructs a new exception.
     *
     * @param message the message of the exception
     */
    public ArchiverApplianceException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param message the message of the exception
     * @param cause the cause of the exception (may be null)
     */
    public ArchiverApplianceException(String message, Throwable cause) {
        super(message, cause);
    }
}
