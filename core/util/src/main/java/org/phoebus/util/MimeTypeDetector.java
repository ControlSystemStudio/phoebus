/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.util;

import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MimeTypeDetector {

    /**
     * Attempts to determine the file type based on its content and name. Cleverness delegated to Apache Tika.
     *
     * @param file A non-null {@link File} to inspect.
     * @return A MIME type string, e.g. image/jpeg
     * @throws IOException If there is a problem reading the file or if the provided {@link File} is
     *                     <code>null</code>.
     */
    public static String determineMimeType(File file) throws IOException {
        if (file == null) {
            throw new IOException("File must not be null");
        }
        try {
            return new Tika().detect(file);
        } catch (IOException e) {
            Logger.getLogger(MimeTypeDetector.class.getName())
                .log(Level.WARNING, "Unable to determine MIME type of " + file, e);
            throw e;
        }
    }
}
