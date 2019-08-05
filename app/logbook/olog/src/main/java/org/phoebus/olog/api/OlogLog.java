package org.phoebus.olog.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Eric Berryman taken from shroffk
 * TODO this should simply be a wrapper around an XmlLog
 */
public class OlogLog implements LogEntry {
    private Long id;
    private int version;
    private String owner;
    private String source;
    private String level;
    private String md5Entry;
    private String md5Recent;
    private Long tableId;
    @JsonDeserialize(using = UnixTimestampDeserializer.class)
    private Instant createdDate;
    @JsonDeserialize(using = UnixTimestampDeserializer.class)
    private Instant modifiedDate;
    private String description;

    private Collection<Tag> tags;
    private Collection<Logbook> logbooks;
    private Collection<Attachment> attachments;
    private Collection<Property> properties;

    /** Creates a new instance of XmlLog */
    public OlogLog() {
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId log id
     */
    public OlogLog(Long logId) {
        this.id = logId;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param subject log subject
     * @param owner   log owner
     */
    public OlogLog(String owner) {
        this.owner = owner;
    }

    /**
     * Creates a new instance of XmlLog.
     *
     * @param logId log id
     * @param owner log owner
     */
    public OlogLog(Long logId, String owner) {
        this.id = logId;
        this.owner = owner;
    }

    public OlogLog(XmlLog xmlLog) {
        this.id = xmlLog.getId();
        this.version = xmlLog.getVersion();
        
        owner = xmlLog.getOwner();
        source = xmlLog.getSource();
        level = xmlLog.getLevel();
        md5Entry = xmlLog.getMD5Entry();
        md5Recent = xmlLog.getMD5Entry();
        createdDate = xmlLog.getCreatedDate().toInstant();
        modifiedDate = xmlLog.getModifiedDate().toInstant();
        description = xmlLog.getDescription();

        tags = xmlLog.getXmlTags().stream().map(t -> {return new OlogTag(t);}).collect(Collectors.toList());
        logbooks = xmlLog.getXmlLogbooks().stream().map(l -> {return new OlogLogbook(l);}).collect(Collectors.toList());
        attachments = xmlLog.getXmlAttachments().getAttachments().stream().map(a -> {return new OlogAttachment(a);}).collect(Collectors.toList());;
        properties  = xmlLog.getXmlProperties().stream().map(p -> {return new OlogProperty(p);}).collect(Collectors.toList());
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

    @Override
    public String getTitle() {
        // Not supported
        return null;
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
    @XmlElement
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
    @XmlElement
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
     * @param description the value to set
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
     * @param Table id to set
     */
    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    @Override
    @XmlElementWrapper(name = "properties")
    @XmlElement(type = XmlProperty.class, name = "property", nillable = true)
    public Collection<Property> getProperties() {
        return properties == null ? new ArrayList<Property>() : properties;
    }

    @Override
    public Property getProperty(String propertyName) {
        return null;
    }

    /**
     * Setter for log's XmlProperties.
     * 
     * @param properties XmlProperties
     */
    public void setXmlProperties(Collection<Property> properties) {
        this.properties = properties;
    }

    @Override
    @XmlElementWrapper(name = "logbooks")
    @XmlElement(type = XmlLogbook.class, name = "logbook")
    public Collection<Logbook> getLogbooks() {
        return logbooks == null ? new ArrayList<Logbook>() : logbooks;
    }

    /**
     * Setter for log's XmlLogbooks.
     *
     * @param logbooks XmlLogbooks
     */
    public void setLogbooks(Collection<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    /**
     * Getter for the log's XmlTags.
     *
     * @return XmlTags for this log
     */
    @XmlElementWrapper(name = "tags")
    @XmlElement(type = XmlTag.class, name = "tag")
    public Collection<Tag> getTags() {
        return tags == null ? new ArrayList<Tag>() : tags;
    }

    /**
     * Setter for the log's XmlTags.
     *
     * @param tags XmlTags
     */
    public void setTags(Collection<Tag> tags) {
        this.tags = tags;
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
        return attachments;
    }

    @Override
    public Collection<Attachment> getAttachments() {
        return attachments == null ? new ArrayList<Attachment>() : attachments;
    }

    /**
     * Setter for the log's XmlAttachments.
     *
     * @param attachments XmlAttachments
     */
    public void setXmlAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public Tag getTag(String tagName) {
        return null;
    }
}
