package org.phoebus.logging;

import java.time.Instant;
import java.util.Collection;

public interface LogEntry {

    public Long getId();

    public String getOwner();

    public String getDescription();

    public String getLevel();

    public Instant getCreatedDate();

    public Instant getModifiedDate();

    public int getVersion();

    /**
     * Get a Collection of all the Tags associated with this log.
     * 
     * @return
     */
    public Collection<Tag> getTags();

    /**
     * Get a set of Names of all the tags associated with this log.
     * 
     * @return Set of all tag Names
     */
    public Collection<String> getTagNames();

    /**
     * Returns a Tag with the name tagName if it exists on this log else returns
     * null.
     * 
     * @param tagName
     * @return {@link Tag} with name tagName else null if no such tag attached
     *         to this log
     */
    public Tag getTag(String tagName);

    /**
     * Get all the logbooks associated with this log.
     * 
     * @return a Collection of all {@link Logbook}
     */
    public Collection<Logbook> getLogbooks();

    /**
     * Get a set of all the logbook names.
     * 
     * @return
     */
    public Collection<String> getLogbookNames();

    /**
     * Get all the attachments associated with this log.
     * 
     * @return
     */
    public Collection<Attachment> getAttachments();

    /**
     * Get all the {@link Property}s associated with this log.
     * 
     * @return
     */
    public Collection<Property> getProperties();

    /**
     * Get a set of names for all the properties associated with this log.
     * 
     * @return a set of all property names.
     */
    public Collection<String> getPropertyNames();

    /**
     * return the {@link Property} with name <tt>propertyName</tt> if it exists
     * on this log else return null.
     * 
     * @param propertyName
     * @return {@link Property} with name propertyName else null if no such
     *         property exists on this log.
     */
    public Property getProperty(String propertyName);

}
