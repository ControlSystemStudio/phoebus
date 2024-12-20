/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.util.http;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpRequestMultipartBodyTest {

    @Test
    public void testContentType() {
        String contentType = new HttpRequestMultipartBody().getContentType();
        // Assume generated random boundary string is at least 50 chars
        assertTrue(("multipart/form-data; boundary=".length() + 50) < contentType.length());
    }

    @Test
    public void testBody() throws IOException {
        HttpRequestMultipartBody httpRequestMultipartBody = new HttpRequestMultipartBody();
        String boundary = httpRequestMultipartBody.getContentType().substring(httpRequestMultipartBody.getContentType().indexOf("=") + 1);

        httpRequestMultipartBody.addTextPart("fieldName", "{\"json\":\"content\"}", "application/json");

        File file = File.createTempFile("prefix", "tmp");
        file.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write("fileContent".getBytes(StandardCharsets.UTF_8));
        fileOutputStream.flush();
        fileOutputStream.close();

        httpRequestMultipartBody.addFilePart(file);

        String body = new String(httpRequestMultipartBody.getBytes());

        String expected = "\r\n--" + boundary +
                "\r\nContent-Disposition: form-data; name=\"fieldName\"" +
                "\r\nContent-Type: application/json" +
                "\r\n\r\n" +
                "{\"json\":\"content\"}" +
                "\r\n--" + boundary +
                "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"" + file.getName() + "\"" +
                "\r\nContent-Type: application/octet-stream" +
                "\r\n\r\n" +
                "fileContent" +
                "\r\n--" + boundary + "--";

        assertEquals(expected, body);
    }
}
