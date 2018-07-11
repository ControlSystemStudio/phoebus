package org.phoebus.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A default implementation of {@link Attachment}
 * @author Kunal Shroff
 *
 */
public class AttachmentImpl implements Attachment {
    private final File file;
    private final String contentType;
    private final Boolean thumbnail;

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

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given attachement
     * @param attach 
     * @return a {@link Attachment} based on the given attachment
     */
    public static Attachment of(Attachment attach) {
        return new AttachmentImpl(  attach.getFile(),
                                    attach.getContentType(),
                                    attach.getThumbnail());
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given uri
     * @param uri - the attachment resource
     * @return a {@link Attachment} based on the given uri
     * @throws IOException 
     */
    public static Attachment of(String uri) throws IOException {
        return new AttachmentImpl(new File(uri), null, null);
    }

    /**
     * Create a new instance of a default implementation of the {@link Attachment} interface using the given file
     * @param file - the attachment file
     * @return a {@link Attachment} based on the given file
     * @throws FileNotFoundException 
     */
    public static Attachment of(File file) throws FileNotFoundException {
        return new AttachmentImpl(file, null, null);
    }

}
