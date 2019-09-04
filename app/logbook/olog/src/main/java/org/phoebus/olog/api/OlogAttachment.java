
package org.phoebus.olog.api;

import java.io.File;

import org.phoebus.logbook.Attachment;

/**
 *
 * @author Eric Berryman
 */
public class OlogAttachment implements Attachment {
    private final String fileName;
    private final String contentType;
    private final Boolean thumbnail;
    private final Long fileSize;

    private File file;

    OlogAttachment(XmlAttachment xml) {
        this.fileName = xml.getFileName();
        this.contentType = xml.getContentType();
        this.thumbnail = xml.getThumbnail();
        this.fileSize = xml.getFileSize();
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getContentType() {
        return contentType;
    }

    public Boolean getThumbnail() {
        return thumbnail;
    }

    public Long getFileSize() {
        return fileSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OlogAttachment))
            return false;
        OlogAttachment other = (OlogAttachment) obj;
        if (fileName == null) {
            if (other.fileName != null)
                return false;
        } else if (!fileName.equals(other.fileName))
            return false;
        return true;
    }

}
