package org.phoebus.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A default implementation of {@link Attachment}
 * @author Kunal Shroff
 *
 */
public class AttachmentImpl implements Attachment {
    private final String fileName;
    private InputStream fileInputStream;
    private final String contentType;
    private final Boolean thumbnail;
    private final Long fileSize;

    private AttachmentImpl(String fileName, InputStream fileInputStream, String contentType, Boolean thumbnail, Long fileSize) {
        super();
        this.fileName = fileName;
        this.fileInputStream = fileInputStream;
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

    @Override
    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given attachement
     * @param attach 
     * @return a {@link Attachment} based on the given attachment
     */
    public static Attachment of(Attachment attach) {
        return new AttachmentImpl(  attach.getFileName(),
                                    attach.getFileInputStream(),
                                    attach.getContentType(),
                                    attach.getThumbnail(),
                                    attach.getFileSize());
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given uri
     * @param uri - the attachment resource
     * @return a {@link Attachment} based on the given uri
     * @throws IOException 
     */
    public static Attachment of(String uri) throws IOException {
        return new AttachmentImpl(uri.toString(), Files.newInputStream(Paths.get(uri)), null, null, null);
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param file - the attachment file
     * @return a {@link Attachment} based on the given file
     * @throws FileNotFoundException 
     */
    public static Attachment of(File file) throws FileNotFoundException {
        return new AttachmentImpl(file.toURI().toString(), new FileInputStream(file), null, null, null);
    }


}
