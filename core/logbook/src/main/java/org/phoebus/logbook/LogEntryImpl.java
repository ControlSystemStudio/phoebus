package org.phoebus.logbook;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LogEntryImpl implements LogEntry {

    private final Long id;
    private final String owner;
    private final String title;
    private final String description;
    private final String level;
    private final Instant createdDate;
    private final Instant modifiedDate;
    private final int version;

    private final Map<String, Tag> tags;
    private final Map<String, Logbook> logbooks;
    private final Map<String, Attachment> attachments;
    private final Map<String, Property> properties;

    private LogEntryImpl(Long id, String owner, String title, String description, String level, Instant createdDate,
            Instant modifiedDate, int version, Set<Tag> tags, Set<Logbook> logbooks,
            Set<Attachment> attachments, Set<Property> properties) {
        super();
        this.id = id;
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.level = level;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.version = version;

        this.tags = tags.stream().collect(Collectors.toMap(Tag::getName, (Tag t) -> {return t;}));
        this.logbooks = logbooks.stream().collect(Collectors.toMap(Logbook::getName, (Logbook l) -> {return l;}));
        this.attachments = attachments.stream().collect(Collectors.toMap((a)-> {return a.getFile().getName();}, (a) -> {return a;}));
        this.properties = properties.stream().collect(Collectors.toMap(Property::getName, (Property p) -> {return p;}));
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLevel() {
        return level;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Get a Collection of all the Tags associated with this log.
     * 
     * @return
     */
    public Collection<Tag> getTags() {
        return tags.values();
    }

    /**
     * Get a set of Names of all the tags associated with this log.
     * 
     * @return Set of all tag Names
     */
    public Collection<String> getTagNames() {
        return tags.keySet();
    }

    /**
     * Returns a Tag with the name tagName if it exists on this log else returns
     * null.
     * 
     * @param tagName
     * @return {@link Tag} with name tagName else null if no such tag attached
     *         to this log
     */
    public Tag getTag(String tagName) {
        return tags.get(tagName);
    }

    /**
     * Get all the logbooks associated with this log.
     * 
     * @return a Collection of all {@link Logbook}
     */
    public Collection<Logbook> getLogbooks() {
        return logbooks.values();
    }

    /**
     * Get a set of all the logbook names.
     * 
     * @return
     */
    public Collection<String> getLogbookNames() {
        return logbooks.keySet();
    }

    /**
     * Get all the attachments associated with this log.
     * 
     * @return
     */
    public Collection<Attachment> getAttachments() {
        return attachments.values();
    }

    /**
     * Get all the {@link Property}s associated with this log.
     * 
     * @return
     */
    public Collection<Property> getProperties() {
        return properties.values();
    }

    /**
     * Get a set of names for all the properties associated with this log.
     * 
     * @return a set of all property names.
     */
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    /**
     * return the {@link Property} with name <tt>propertyName</tt> if it exists
     * on this log else return null.
     * 
     * @param propertyName
     * @return {@link Property} with name propertyName else null if no such
     *         property exists on this log.
     */
    public Property getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String toString() {
        return "Log#" + id + ":v." + version + " [ description=" + description + "]";
    }

    /**
     * A Builder for creating an instance of {@link LogEntryImpl}
     * 
     * @author Kunal Shroff
     *
     */
    public static class LogEntryBuilder {
        private Long id;
        private String owner;
        private String title;
        private StringBuilder description;
        private String level;
        private Instant createdDate;
        private Instant modifiedDate;
        private int version;
        private Set<Tag> tags = new HashSet<Tag>();
        private Set<Logbook> logbooks = new HashSet<Logbook>();
        private Set<Property> properties = new HashSet<Property>();
        private Set<Attachment> attachments = new HashSet<Attachment>();

        public static LogEntryBuilder log(LogEntry log) {
            LogEntryBuilder logentryBuilder = new LogEntryBuilder();
            logentryBuilder.id = log.getId();
            logentryBuilder.owner = log.getOwner();
            logentryBuilder.title = log.getTitle();
            logentryBuilder.description = new StringBuilder(log.getDescription());
            logentryBuilder.level = log.getLevel();
            logentryBuilder.createdDate = log.getCreatedDate();
            logentryBuilder.modifiedDate = log.getModifiedDate();
            logentryBuilder.version = log.getVersion();
            logentryBuilder.tags.addAll(log.getTags());
            logentryBuilder.logbooks.addAll(log.getLogbooks());
            logentryBuilder.attachments.addAll(log.getAttachments());
            logentryBuilder.properties.addAll(log.getProperties());
            return logentryBuilder;
        }

        public static LogEntryBuilder log() {
            LogEntryBuilder logBuilder = new LogEntryBuilder();
            return logBuilder;
        }

        public LogEntryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LogEntryBuilder description(String description) {
            if (description != null)
                this.description = new StringBuilder(description);
            else if (description == null)
                this.description = null;
            return this;
        }

        public LogEntryBuilder appendDescription(String description) {
            if (this.description == null)
                this.description = new StringBuilder(description);
            else if (this.description != null)
                this.description.append("\n").append(description);
            return this;
        }

        public LogEntryBuilder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public LogEntryBuilder title(String title) {
            this.title = title;
            return this;
        }

        public LogEntryBuilder createdDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }
        
        public LogEntryBuilder modifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public LogEntryBuilder level(String level) {
            this.level = level;
            return this;
        }

        public LogEntryBuilder withTags(Set<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public LogEntryBuilder withProperties(Set<Property> properties) {
            this.properties = properties;
            return this;
        }

        public LogEntryBuilder inLogbooks(Set<Logbook> logbooks) {
            this.logbooks = logbooks;
            return this;
        }

        public LogEntryBuilder appendTag(Tag tag) {
            this.tags.add(tag);
            return this;
        }

        public LogEntryBuilder appendProperty(Property property) {
            this.properties.add(property);
            return this;
        }

        public LogEntryBuilder appendToLogbook(Logbook logbook) {
            this.logbooks.add(logbook);
            return this;
        }

        public LogEntryBuilder attach(Attachment attachment) {
            attachments.add(attachment);
            return this;
        }
        
        public LogEntryBuilder setAttach(Set<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public LogEntryBuilder property(Property property) {
            properties.add(property);
            return this;
        }

        public LogEntry build() {
            return new LogEntryImpl(id, owner, title, description.toString(), level, createdDate, modifiedDate, version, tags, logbooks,
                    attachments, properties);
        }
    }

}
