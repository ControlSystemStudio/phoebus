/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MimeTypeDetectorTest {

    private static File resource(String name) throws URISyntaxException {
        return new File(MimeTypeDetector.class.getResource(name).toURI());
    }

    @Test
    public void testHeic() throws Exception {
        assertEquals("image/heic",
                MimeTypeDetector.determineMimeType(resource("ios_original.HEIC")));

        assertEquals("image/heic",
                MimeTypeDetector.determineMimeType(resource("ios_heic_converted.JPEG")));
    }

    @Test
    public void testJpeg() throws Exception {
        assertEquals("image/jpeg",
                MimeTypeDetector.determineMimeType(resource("proper_jpeg.jpeg")));
    }

    @Test
    public void testPdf() throws Exception {
        assertEquals("application/pdf",
                MimeTypeDetector.determineMimeType(resource("mimetest.pdf")));
    }

    @Test
    public void testNullFile() {
        assertThrows(IOException.class, () ->
                MimeTypeDetector.determineMimeType(null));
    }
}
