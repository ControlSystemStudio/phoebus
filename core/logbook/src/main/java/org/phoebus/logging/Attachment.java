
package org.phoebus.logging;

import java.io.File;

/**
 * An interface describing the attachments associated with a {@link LogEntry}
 * @author Eric Berryman
 */
public interface Attachment {

    public File getFile();

    public String getContentType();

    public Boolean getThumbnail();

}
