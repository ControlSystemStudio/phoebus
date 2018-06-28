package org.phoebus.logging;

import java.io.File;

/**
 * A default implementation of {@link Attachment}
 * @author Kunal Shroff
 *
 */
public class AttachmentImpl implements Attachment {
    private final String fileName;
    private final String contentType;
    private final Boolean thumbnail;
    private final Long fileSize;

    private AttachmentImpl(String fileName, String contentType, Boolean thumbnail, Long fileSize) {
        super();
        this.fileName = fileName;
        this.contentType = contentType;
        this.thumbnail = thumbnail;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
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

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given attachement
     * @param attach 
     * @return a {@link Attachment} based on the given attachment
     */
    public static Attachment of(Attachment attach) {
        return new AttachmentImpl(  attach.getFileName(),
                                    attach.getContentType(),
                                    attach.getThumbnail(),
                                    attach.getFileSize());
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given uri
     * @param uri - the attachment resource
     * @return a {@link Attachment} based on the given uri
     */
    public static Attachment of(String uri) {
        File fileToUpload = new File(uri);
        return new AttachmentImpl(fileToUpload.getName(), null, null, null);
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param file - the attachment file
     * @return a {@link Attachment} based on the given file
     */
    public static Attachment of(File file) {
        return new AttachmentImpl(file.getName(), null, null, null);
    }

}
