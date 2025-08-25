/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MimeTypeDetectorTest {

    @Test
    public void testHeic() throws Exception {
        assertEquals("image/heic",
                MimeTypeDetector
                        .determineMimeType(MimeTypeDetector.class.getResourceAsStream("ios_original.HEIC")));

        assertEquals("image/heic",
                MimeTypeDetector
                        .determineMimeType(MimeTypeDetector.class.getResourceAsStream("ios_heic_converted.JPEG")));
    }

    @Test
    public void testJpeg() throws Exception {
        assertEquals("image/jpeg",
                MimeTypeDetector
                        .determineMimeType(MimeTypeDetector.class.getResourceAsStream("proper_jpeg.jpeg")));
    }

    @Test
    public void testPdf() throws Exception {
        assertEquals("application/pdf",
                MimeTypeDetector
                        .determineMimeType(MimeTypeDetector.class.getResourceAsStream("mimetest.pdf")));
    }

    @Test
    public void testNullInputStream() {
        assertThrows(IOException.class, () ->
                MimeTypeDetector
                        .determineMimeType(MimeTypeDetector.class.getResourceAsStream("does_no_exist")));
    }
}
