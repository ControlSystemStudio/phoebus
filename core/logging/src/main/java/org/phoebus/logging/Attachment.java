
package org.phoebus.logging;

import java.io.InputStream;

/**
 * An interface describing the attachments associated with a {@link LogEntry}
 * @author Eric Berryman
 */
public interface Attachment {

    public String getFileName();

    public InputStream getFileInputStream();

    public String getContentType();

    public Boolean getThumbnail();

    public Long getFileSize();

}
