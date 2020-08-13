/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package org.phoebus.olog.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;

import org.phoebus.logbook.LogEntry;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.Date;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Log object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "log")
public class XmlLog {

    private Long id;
    private int version;
    private String owner;
    private String source;
    private String level;
    private String md5Entry;
    private String md5Recent;
    private Long tableId;
    private Date createdDate;
    private Date modifiedDate;
//    private String subject;
    private String description;
    private Collection<XmlProperty> properties = new ArrayList<XmlProperty>();
    private Collection<XmlLogbook> logbooks = new ArrayList<XmlLogbook>();
    private Collection<XmlTag> tags = new ArrayList<XmlTag>();
    private XmlAttachments attachments = new XmlAttachments();

    /** Creates a new instance of XmlLog */
    public XmlLog() {
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId log id
     */
    public XmlLog(Long logId) {
        this.id = logId;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param subject log subject
     */
//    public XmlLog(String subject) {
//        this.subject = subject;
//    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param subject log subject
     * @param owner log owner
     */
    public XmlLog(/*String subject, */String owner) {
//        this.subject = subject;
        this.owner = owner;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId log id
     * @param owner log owner
     */
    public XmlLog(Long logId, String owner) {
        this.id = logId;
        this.owner = owner;
    }

    public XmlLog(LogEntry log) {
        id = log.getId();
        version = log.getVersion();
        
        owner = log.getOwner();
        source = "";
        level = log.getLevel();
        md5Entry = "";
        md5Recent = "";
        createdDate = log.getCreatedDate() != null ? Date.from(log.getCreatedDate()) : null;
        modifiedDate = log.getModifiedDate() != null ? Date.from(log.getModifiedDate()) : null;
        description = log.getTitle() + System.lineSeparator() +log.getDescription();

        tags = log.getTags().stream().map(t -> {return new XmlTag(t);}).collect(Collectors.toList());
        logbooks = log.getLogbooks().stream().map(l -> {return new XmlLogbook(l);}).collect(Collectors.toList());
        attachments = new XmlAttachments();
        attachments.setAttachments(log.getAttachments().stream().map(a -> {return new XmlAttachment(a);}).collect(Collectors.toList()));
        properties  = log.getProperties().stream().map(p -> {return new XmlProperty(p);}).collect(Collectors.toList());
    }

    /**
     * Getter for log id.
     *
     * @return id
     */
    @XmlAttribute
    public Long getId() {
        return id;
    }

    /**
     * Setter for log id.
     *
     * @param logId
     */
    public void setId(Long logId) {
        this.id = logId;
    }

    /**
     * Getter for log version id.
     *
     * @return versionId
     */
    @XmlAttribute
    public int getVersion() {
        return version;
    }

    /**
     * Setter for log version id.
     *
     * @param version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Getter for log owner.
     *
     * @return owner
     */
    @XmlAttribute
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for log owner.
     *
     * @param owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for log level.
     *
     * @return level
     */
    @XmlAttribute
    public String getLevel() {
        return level;
    }

    /**
     * Setter for log owner.
     *
     * @param level
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Getter for log created date.
     *
     * @return createdDate
     */
    @XmlAttribute
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
     * Setter for log created date.
     *
     * @param createdDate
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Getter for log modified date.
     *
     * @return modifiedDate
     */
    @XmlAttribute
    public Date getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Setter for log modified date.
     *
     * @param modifiedDate
     */
    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * Getter for log source IP.
     *
     * @return source IP
     */
    @XmlAttribute
    public String getSource() {
        return source;
    }

    /**
     * Setter for log source IP.
     *
     * @param source IP
     */
    public void setSource(String source) {
        this.source = source;
    }

     /**
     * Getter for log description.
     *
     * @return description
     */
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * Setter for log description.
     *
     * @param description the value to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for MD5 entry.
     *
     * @return description
     */
    public String getMD5Entry() {
        return md5Entry;
    }

    /**
     * Setter for MD5 entry.
     *
     * @param md5entry the value to set
     */
    public void setMD5Entry(String md5entry) {
        this.md5Entry = md5entry;
    }
    
    /**
     * Getter for Table id.
     *
     * @return table id
     */
    public Long getTableId() {
        return tableId;
    }

    /**
     * Setter for Table id.
     *
     * @param tableId id to set
     */
    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }
    
    /**
     * Getter for log's XmlProperties.
     *
     * @return properties XmlProperties
     */
    @XmlElementWrapper(name = "properties")
    @XmlElement(name = "property")
    public Collection<XmlProperty> getXmlProperties() {
        return properties;
    }

    /**
     * Setter for log's XmlProperties.
     *
     * @param properties XmlProperties
     */
    public void setXmlProperties(Collection<XmlProperty> properties) {
        this.properties = properties;
    }

    /**
     * Adds an XmlProperty to the log.
     *
     * @param property single XmlProperty
     */
    public void addXmlProperty(XmlProperty property) {
        this.properties.add(property);
    }

    /**
     * Getter for log's XmlLogbooks.
     *
     * @return XmlLogbooks
     */
    @XmlElementWrapper(name = "logbooks")
    @XmlElement(name = "logbook")
    public Collection<XmlLogbook> getXmlLogbooks() {
        return logbooks;
    }

    /**
     * Setter for log's XmlLogbooks.
     *
     * @param logbooks XmlLogbooks
     */
    public void setXmlLogbooks(Collection<XmlLogbook> logbooks) {
        this.logbooks = logbooks;
    }

    /**
     * Adds an XmlLogbook to the log.
     *
     * @param logbook single XmlLogbook
     */
    public void addXmlLogbook(XmlLogbook logbook) {
        this.logbooks.add(logbook);
    }

    /**
     * Getter for the log's XmlTags.
     *
     * @return XmlTags for this log
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    public Collection<XmlTag> getXmlTags() {
        return tags;
    }

    /**
     * Setter for the log's XmlTags.
     *
     * @param tags XmlTags
     */
    public void setXmlTags(Collection<XmlTag> tags) {
        this.tags = tags;
    }

    /**
     * Adds an XmlTag to the collection.
     *
     * @param tag
     */
    public void addXmlTag(XmlTag tag) {
        this.tags.add(tag);
    }

    /**
     * Getter for the log's XmlAttachments.
     *
     * @return XmlAttachments for this log
     */
    @XmlElement(name = "attachments")
    public XmlAttachments getXmlAttachments() {
        return attachments;
    }

    /**
     * Setter for the log's XmlAttachments.
     *
     * @param attachments XmlAttachments
     */
    public void setXmlAttachments(XmlAttachments attachments) {
        this.attachments = attachments;
    }

    /**
     * Adds an XmlAttachment to the collection.
     *
     * @param attachment
     */
    public void addXmlAttachment(XmlAttachment attachment) {
        this.attachments.addXmlAttachment(attachment);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlLog to create the string representation for
     * @return string representation
     */
    public static String toLog(XmlLog data) {
        XmlLogbooks xl = new XmlLogbooks();
        xl.setLogbooks(data.logbooks);
        
        XmlTags xt = new XmlTags();
        xt.setTags(data.tags);
        
        return data.getId() + "-v." + data.getVersion() + " : " + /*data.getSubject() +*/ "(" + data.getOwner() + "):["
                + XmlLogbooks.toLog(xl)
                + XmlTags.toLog(xt)
                + XmlAttachments.toLog(data.attachments)
                + "]\n";
    }
}
