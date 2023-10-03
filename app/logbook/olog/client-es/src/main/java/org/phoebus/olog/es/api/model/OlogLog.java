/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package org.phoebus.olog.es.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Log object that can be represented as XML/JSON in payload data.
 *
 * @author Kunal Shroff taken from Ralph Lange {@literal <Ralph.Lange@bessy.de>}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OlogLog implements LogEntry {

    private Long id;
    private int version;
    private String owner;
    private String source;
    private String level;
    private String title;
    @JsonDeserialize(using = UnixTimestampDeserializer.class)
    private Instant createdDate;
    @JsonDeserialize(using = UnixTimestampDeserializer.class)
    private Instant modifiedDate;
    private String description;

    private Collection<Tag> tags;
    private Collection<Logbook> logbooks;
    private Collection<Attachment> attachments;
    private Collection<Property> properties;

    /**
     * Creates a new instance of OlogLog
     */
    public OlogLog() {
    }

    /**
     * Creates a new instance of OlogLog.
     *
     * @param logId log id
     */
    public OlogLog(Long logId) {
        this.id = logId;
    }

    /**
     * Creates a new instance of OlogLog.
     *
     * @param owner log owner
     */
    public OlogLog(String owner) {
        this.owner = owner;
    }

    /**
     * Creates a new instance of OlogLog.
     *
     * @param logId log id
     * @param owner log owner
     */
    public OlogLog(Long logId, String owner) {
        this.id = logId;
        this.owner = owner;
    }

    /**
     * Getter for log id.
     *
     * @return id
     */
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
        return title;
    }

    /**
     * Getter for log owner.
     *
     * @return owner
     */
    @Override
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
    @Override
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
     * Setter for log title.
     *
     * @param title title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for log created date.
     *
     * @return createdDate
     */
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
    @JsonProperty("modifyDate")
    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }


    /**
     * Getter for log source IP.
     *
     * @return source IP
     */
    @Override
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
    @Override
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

    @Override
    public Collection<Property> getProperties() {
        return properties == null ? new ArrayList<Property>() : properties;
    }

    @Override
    public Property getProperty(String propertyName) {
        if (propertyName == null) {
            return null;
        }
        return getProperties().stream().filter(p -> propertyName.equals(p.getName())).findFirst().orElse(null);
    }

    /**
     * Setter for log's Properties.
     *
     * @param properties properties
     */
    public void setProperties(Collection<Property> properties) {
        this.properties = properties;
    }

    @Override
    public Collection<Logbook> getLogbooks() {
        return logbooks == null ? new ArrayList<Logbook>() : logbooks;
    }

    /**
     * Setter for log's OlogLogbooks.
     *
     * @param logbooks OlogLogbooks
     */
    public void setLogbooks(Collection<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    /**
     * Getter for the log's OlogTags.
     *
     * @return OlogTags for this log
     */
    @Override
    public Collection<Tag> getTags() {
        return tags == null ? new ArrayList<Tag>() : tags;
    }

    /**
     * Setter for the log's Tags.
     *
     * @param tags Tags
     */
    public void setTags(Collection<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public Collection<Attachment> getAttachments() {
        return attachments == null ? new ArrayList<Attachment>() : attachments;
    }

    /**
     * Setter for the log's attachments.
     *
     * @param attachments attachments
     */
    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public Tag getTag(String tagName) {
        return null;
    }

}
