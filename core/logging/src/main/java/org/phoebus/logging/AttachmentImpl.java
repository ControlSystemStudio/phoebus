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

    public static class AttachmentBuilder {
        private String fileName;
        private String contentType;
        private Boolean thumbnail;
        private Long fileSize;

        public static AttachmentBuilder attachment(Attachment attach) {
            AttachmentBuilder builder = new AttachmentBuilder();
            builder.fileName = attach.getFileName();
            builder.contentType = attach.getContentType();
            builder.thumbnail = attach.getThumbnail();
            builder.fileSize = attach.getFileSize();
            return builder;
        }

        public static AttachmentBuilder attachment(String uri) {
            AttachmentBuilder builder = new AttachmentBuilder();
            File fileToUpload = new File(uri);
            builder.fileName = fileToUpload.getName();
            return builder;
        }

        public static AttachmentBuilder attachment(File file) {
            AttachmentBuilder builder = new AttachmentBuilder();
            builder.fileName = file.getName();
            return builder;
        }

        public Attachment build() {
            return new AttachmentImpl(fileName, contentType, thumbnail, fileSize);
        }

    }
}
