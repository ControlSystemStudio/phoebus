/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.olog.es.api.model;

import java.io.File;
import java.util.UUID;

import org.phoebus.logbook.Attachment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Attachment object that can be represented as XML/JSON in payload data. TODO:
 * pass attachments over XML / without webdav? make log entries with attachments
 * atomic?
 * 
 * @author Kunal Shroff
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class OlogAttachment implements Attachment {

    protected String fileName;

    protected String contentType;

    protected Boolean thumbnail;

    protected Long fileSize;

    private File file;

    /**
     * The unique id of the attachment. Client code need not set this, in which case the log
     * service will. If set, it must be unique among all attachments.
     */
    protected String id;

    /**
     * Creates a new instance of XmlAttachment
     */
    public OlogAttachment() {
       this(UUID.randomUUID().toString());
    }

    public OlogAttachment(String id) {
        this.thumbnail = false;
        this.id = id;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize
     *            the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the contentType
     */
    @JsonProperty("fileMetadataDescription")
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType
     *            the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the thumbnail name
     */
    public Boolean getThumbnail() {
        return thumbnail;
    }

    /**
     * @param thumbnail
     *            name the contentType to set
     */
    public void setThumbnail(Boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setId(String id){
        this.id = id;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    @JsonProperty("filename")
    public String getName() {
        return fileName;
    }


    @Override
    public String getId(){
        return id;
    }

}
