/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.olog.es.api;

import java.io.File;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.phoebus.logbook.Attachment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Attachment object that can be represented as XML/JSON in payload data. TODO:
 * pass attachments over XML / without webdav? make log entries with attachments
 * atomic?
 * 
 * @author Eric Berryman
 */

@XmlType
@XmlRootElement(name = "attachment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class XmlAttachment implements Attachment {

    @XmlTransient
    protected String fileName;

    @XmlTransient
    protected String contentType;

    @XmlTransient
    protected Boolean thumbnail;

    @XmlTransient
    protected Long fileSize;

    private File file;

    /**
     * Creates a new instance of XmlAttachment
     */
    public XmlAttachment() {
        this.thumbnail = false;
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

    /**
     * Creates a compact string representation for the log.
     *
     * @param data
     *            the XmlAttach to log
     * @return string representation for log
     */
    public static String toLog(XmlAttachment data) {
        return data.getFileName() + "(" + data.getContentType() + ")";
    }

    @Override
    public File getFile() {
        return file;
    }

    void setFile(File file) {
        this.file = file;
    }

    @Override
    @JsonProperty("filename")
    public String getName() {
        return fileName;
    }

}
