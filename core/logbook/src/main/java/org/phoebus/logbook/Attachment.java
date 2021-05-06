
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
     * @param id
     */
    default void setId(String id){}
    
    public String getName();

    public File getFile();

    public String getContentType();

    public Boolean getThumbnail();

}
