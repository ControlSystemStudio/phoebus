/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.util;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MimeTypeDetector {

    /**
     * Attempts to determine the file type based on its content. Cleverness delegated to Apache Tika.
     * <p>
     * Note that the {@link InputStream} will be closed by this method.
     * </p>
     *
     * @param inputStream A non-null {@link InputStream} that will be closed by this method.
     * @return A MIME type string, e.g. image/jpeg
     * @throws IOException If there is a problem reading the stream or if the provided {@link InputStream} is
     *                     <code>null</code>.
     */
    public static String determineMimeType(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("InputStream must not be null");
        }
        try {
            return new Tika().detect(inputStream, new Metadata());
        } catch (IOException e) {
            Logger.getLogger(MimeTypeDetector.class.getName())
                    .log(Level.WARNING, "Unable to read input stream", e);
            throw e;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Logger.getLogger(MimeTypeDetector.class.getName())
                        .log(Level.WARNING, "Failed to close input stream", e);
            }
        }
    }
}
