/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.util.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Utility class for the purpose of creating a multipart HTTP request body. Supports text (String) and
 * binary (files), or a mix.
 * Inspired by <a href="https://gist.github.com/varaprasadh/4c588a87257e19d4d10e357325ed1130#file-httprequestmultipartbody-java">this example</a>.
 * </p>
 * <p>
 * Each added part is appended as a byte array representation to a {@link ByteArrayOutputStream}.
 * When all parts have been added, client code must call {@link #getBytes()} to acquire the body used in the call to a server,
 * and must call {@link #getContentType()} to be able to set the correct Content-Type header of the request.
 * </p>
 */
public class HttpRequestMultipartBody {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final String boundary;

    public HttpRequestMultipartBody() {
        this.boundary = new BigInteger(256, new SecureRandom()).toString();
    }

    /**
     * @return Content type string including the boundary string.
     */
    public String getContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    /**
     * Adds text part of a multipart body
     *
     * @param fieldName   A field name for the sake of identification on the service, e.g. to identify the
     *                    text part of a log entry.
     * @param value       Value, e.g. JSON representation of a log entry.
     * @param contentType Self-explanatory, e.g. application/json.
     */
    public void addTextPart(String fieldName, String value, String contentType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\r\n--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append(fieldName).append("\"");
        stringBuilder.append("\r\nContent-Type: ").append(contentType).append("\r\n\r\n");
        stringBuilder.append(value);
        byteArrayOutputStream.writeBytes(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds contents of a {@link File} to the multipart request body. The content type is probed, but may fall back to
     * application/octet-stream if probe fails.
     *
     * @param file A <code>non-null</code> file.
     * @throws RuntimeException if the file does not exist or cannot be read.
     */
    public void addFilePart(File file) {
        if (file == null){
            throw new RuntimeException("File part must not be null");
        }
        else if(!file.exists() || !file.canRead()){
            throw new RuntimeException("File " + file.getAbsolutePath() + " does not exist or cannot be read");
        }
        StringBuilder stringBuilder = new StringBuilder();
        // Default generic content type...
        String contentType = "application/octet-stream";
        try {
            // ... but try to determine something more specific
            String probedType = Files.probeContentType(file.toPath());
            if(probedType != null){
                contentType = probedType;
            }
        } catch (IOException e) {
            Logger.getLogger(HttpRequestMultipartBody.class.getName()).log(Level.WARNING, "Unable to determine content type of file " + file.getAbsolutePath(), e);
        }
        stringBuilder.append("\r\n--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append("files").append("\"; filename=\"").append(file.getName()).append("\"");
        stringBuilder.append("\r\nContent-Type: ").append(contentType).append("\r\n\r\n");

        byteArrayOutputStream.writeBytes(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        try {
            Files.copy(file.toPath(), byteArrayOutputStream);
        } catch (IOException e) {
            Logger.getLogger(HttpRequestMultipartBody.class.getName()).log(Level.WARNING, "Failed to copy content of file part", e);
        }
    }

    /**
     * @return The body of the multipart request.
     */
    public byte[] getBytes() {
        // Add last boundary
        byteArrayOutputStream.writeBytes(("\r\n--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return byteArrayOutputStream.toByteArray();
    }
}
