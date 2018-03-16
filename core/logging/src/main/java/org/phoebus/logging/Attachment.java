
package org.phoebus.logging;

/**
 *
 * @author Eric Berryman
 */
public interface Attachment {

    public String getFileName();

    public String getContentType();

    public Boolean getThumbnail();

    public Long getFileSize();

}
