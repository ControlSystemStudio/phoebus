/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.phoebus.olog.api;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.phoebus.logbook.Attachment;

/**
 * Attachment object that can be represented as XML/JSON in payload data.
 * TODO: pass attachments over XML / without webdav? make log entries with attachments atomic?
 * @author Eric Berryman
 */


@XmlType
@XmlRootElement(name = "attachment")
public class XmlAttachment {
    @XmlTransient
    protected String fileName;

    @XmlTransient
    protected String contentType;
    
    @XmlTransient
    protected Boolean thumbnail;
    
    @XmlTransient
    protected Long fileSize;

    /**
     * Creates a new instance of XmlAttachment
     */
    public XmlAttachment() {
        this.thumbnail = false;
    }

    public XmlAttachment(Attachment a) {
        fileName = a.getName();
        contentType = a.getContentType();
        thumbnail = a.getThumbnail();
        // TODO
        fileSize = 0L;
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
    public boolean getThumbnail() {
        return thumbnail;
    }

    /**
     * @param thumbnail name
     *            the contentType to set
     */
    public void setThumbnail(Boolean thumbnail) {
	this.thumbnail = thumbnail;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data the XmlAttach to log
     * @return string representation for log
     */
    public static String toLog(XmlAttachment data) {
        return data.getFileName() + "(" + data.getContentType() + ")";
    }
}
