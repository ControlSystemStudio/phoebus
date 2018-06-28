
package org.phoebus.logging;

/**
 * An interface describing the attachments associated with a {@link LogEntry}
 * @author Eric Berryman
 */
public interface Attachment {

    public String getFileName();

    public String getContentType();

    public Boolean getThumbnail();

    public Long getFileSize();

}
