package org.phoebus.logbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.FileNameMap;
import java.net.URLConnection;


/**
 * A default implementation of {@link Attachment}
 * @author Kunal Shroff
 *
 */
public class AttachmentImpl implements Attachment {
    private final File file;
    private final String contentType;
    private final Boolean thumbnail;
    private String id;

    private static FileNameMap fileNameMap = URLConnection.getFileNameMap();

    private AttachmentImpl(File file, String contentType, Boolean thumbnail) {
        super();
        this.file = file;
        this.contentType = contentType;
        this.thumbnail = thumbnail;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Boolean getThumbnail() {
        return thumbnail;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getId(){
        return id;
    }

    @Override
    public void setId(String id){
        this.id = id;
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param file - the attachment file
     * @return a {@link Attachment} based on the given file
     * @throws FileNotFoundException 
     */
    public static Attachment of(File file) throws FileNotFoundException {
        String mimeType = fileNameMap.getContentTypeFor(file.getName());
        return new AttachmentImpl(file, mimeType, null);
    }
    
    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param file - the attachment file
     * @param contentType - the type of the attached content ("image", "file", etc...)
     * @param thumbnail - Whether the attachment has a thumbnail.
     * @return a {@link Attachment} based on the given file
     * @throws FileNotFoundException 
     */
    public static Attachment of(File file, String contentType, boolean thumbnail) throws FileNotFoundException {
        return new AttachmentImpl(file, contentType, thumbnail);
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param id A unique id
     * @param file - the attachment file
     * @param contentType - the type of the attached content ("image", "file", etc...)
     * @param thumbnail - Whether the attachment has a thumbnail.
     * @return a {@link Attachment} based on the given file
     * @throws FileNotFoundException
     */
    public static Attachment of(String id, File file, String contentType, boolean thumbnail) throws FileNotFoundException {
        AttachmentImpl attachment = new AttachmentImpl(file, contentType, thumbnail);
        attachment.setId(id);
        return  attachment;
    }

}
