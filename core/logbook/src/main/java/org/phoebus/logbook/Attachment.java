
package org.phoebus.logbook;

import java.io.File;

/**
 * An interface describing the attachments associated with a {@link LogEntry}
 * @author Eric Berryman
 */
public interface Attachment {

    /**
     * @return A unique id set by either client or log service.
     */
    default String getId(){
        return null;
    }

    /**
     * In some cases the client must be able to set the (unique) id.
     * @param id A unique id for the {@link Attachment}
     */
    default void setId(String id){}
    
    String getName();

    File getFile();

    String getContentType();

    Boolean getThumbnail();

    /**
     * Implementations must make sure this returns a string that:
     * <ul>
     *     <li>Is unique between all attachments in a log entry submission.</li>
     *     <li>Preserves the file extension, if the original file defines it.</li>
     * </ul>
     * Note that this is practice <i>not</i> the same as the name of a file
     * picked from the file system.
     * @return A {@link String} unique between all attachments in a log entry submission.
     */
    default String getUniqueFilename(){
        return null;
    }
}
