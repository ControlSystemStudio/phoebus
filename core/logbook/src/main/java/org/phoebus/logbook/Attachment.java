
package org.phoebus.logbook;

import java.io.File;

/**
 * An interface describing the attachments associated with a {@link LogEntry}
 * @author Eric Berryman
 */
public interface Attachment {

    public static final String CONTENT_IMAGE = "image";
    public static final String CONTENT_FILE = "file";

    public String getName();

    public File getFile();

    public String getContentType();

    public Boolean getThumbnail();

}
