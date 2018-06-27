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

import org.phoebus.logging.Attachment;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.Logbook;
import org.phoebus.logging.Property;
import org.phoebus.logging.Tag;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.Date;
import java.util.Map;
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
public class XmlLog implements LogEntry {

    private Long id;
    private int version;
    private String owner;
    private String source;
    private String level;
    private String md5Entry;
    private String md5Recent;
    private Long tableId;
    private Instant createdDate;
    private Instant modifiedDate;
    private String description;

    private Map<String, Tag> tags;
    private Map<String, Logbook> logbooks;
    private Map<String, Attachment> attachments;
    private Map<String, Property> properties;

    /** Creates a new instance of XmlLog */
    public XmlLog() {
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId
     *            log id
     */
    public XmlLog(Long logId) {
        this.id = logId;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param subject
     *            log subject
     * @param owner
     *            log owner
     */
    public XmlLog(String owner) {
        this.owner = owner;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId
     *            log id
     * @param owner
     *            log owner
     */
    public XmlLog(Long logId, String owner) {
        this.id = logId;
        this.owner = owner;
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
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Setter for log created date.
     *
     * @param createdDate
     */
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Getter for log modified date.
     *
     * @return modifiedDate
     */
    @XmlAttribute
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Setter for log modified date.
     *
     * @param modifiedDate
     */
    public void setModifiedDate(Instant modifiedDate) {
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
     * @param source
     *            IP
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
     * @param description
     *            the value to set
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
     * @param description
     *            the value to set
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
     * @param Table
     *            id to set
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
    public Collection<Property> getXmlProperties() {
        return properties.values();
    }

    @Override
    public Collection<Property> getProperties() {
        return properties.values();
    }

    @Override
    public Property getProperty(String propertyName) {
        return properties.get(propertyName);
    }
    /**
     * Setter for log's XmlProperties.
     *
     * @param properties XmlProperties
     */
    public void setXmlProperties(Collection<Property> properties) {
        this.properties = properties.stream().collect(Collectors.toMap(Property::getName, (Property p) -> {return p;}));
    }

    /**
     * Adds an XmlProperty to the log.
     *
     * @param property
     *            single XmlProperty
     */
    public void addXmlProperty(Property property) {
        this.properties.put(property.getName(), property);
    }

    /**
     * Getter for log's XmlLogbooks.
     *
     * @return XmlLogbooks
     */
    @XmlElementWrapper(name = "logbooks")
    @XmlElement(name = "logbook")
    public Collection<Logbook> getXmlLogbooks() {
        return logbooks.values();
    }

    @Override
    public Collection<Logbook> getLogbooks() {
        return logbooks.values();
    }

    /**
     * Setter for log's XmlLogbooks.
     *
     * @param logbooks XmlLogbooks
     */
    public void setXmlLogbooks(Collection<Logbook> logbooks) {
        this.logbooks = logbooks.stream().collect(Collectors.toMap(Logbook::getName, (Logbook l) -> {return l;}));
    }

    /**
     * Adds an XmlLogbook to the log.
     *
     * @param logbook single XmlLogbook
     */
    public void addXmlLogbook(Logbook logbook) {
        this.logbooks.put(logbook.getName(), logbook);
    }

    /**
     * Getter for the log's XmlTags.
     *
     * @return XmlTags for this log
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    public Collection<Tag> getTags() {
        return tags.values();
    }

    @Override
    public Tag getTag(String tagName) {
        return tags.get(tagName);
    }

    /**
     * Setter for the log's XmlTags.
     *
     * @param tags XmlTags
     */
    public void setXmlTags(Collection<Tag> tags) {
        this.tags = tags.stream().collect(Collectors.toMap(Tag::getName, (Tag t) -> {return t;}));
    }

    /**
     * Adds an XmlTag to the collection.
     *
     * @param tag
     */
    public void addXmlTag(XmlTag tag) {
        this.tags.put(tag.getName(), tag);
    }

    /**
     * Getter for the log's XmlAttachments.
     *
     * @return XmlAttachments for this log
     */
    @XmlElement(name = "attachments")
    public XmlAttachments getXmlAttachments() {
        XmlAttachments attachments = new XmlAttachments();
        attachments.setAttachments(attachments.getAttachments());
        return  attachments;
    }

    @Override
    public Collection<Attachment> getAttachments() {
        return attachments.values();
    }

    /**
     * Setter for the log's XmlAttachments.
     *
     * @param attachments XmlAttachments
     */
    public void setXmlAttachments(XmlAttachments attachments) {
        this.attachments = attachments.getAttachments().stream().collect(Collectors.toMap(Attachment::getFileName, (Attachment a) -> {return a;}));
    }

    /**
     * Adds an XmlAttachment to the collection.
     *
     * @param attachment
     */
    public void addXmlAttachment(XmlAttachment attachment) {
        this.attachments.put(attachment.getFileName(), attachment);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlLog to create the string representation for
     * @return string representation
     */
    public static String toLog(LogEntry data) {
        XmlLogbooks xl = new XmlLogbooks();
        xl.setLogbooks(data.getLogbooks().stream().map(logbook -> {return new XmlLogbook(logbook);}).collect(Collectors.toSet()));

        XmlTags xt = new XmlTags();
        xt.setTags(data.getTags().stream().map(tag -> {return new XmlTag(tag);}).collect(Collectors.toSet()));

        return data.getId() + "-v." + data.getVersion() + " : "
                + "(" + data.getOwner() + "):[" + XmlLogbooks.toLog(xl) + XmlTags.toLog(xt)
                + "]\n";
    }

}
