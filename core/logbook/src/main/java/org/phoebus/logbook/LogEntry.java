package org.phoebus.logbook;

import java.time.Instant;
import java.util.Collection;

public interface LogEntry {

    public Long getId();

    public String getOwner();

    public String getTitle();

    public String getDescription();

    /**
     * This (optional) field holds the user specified log entry description in some markup format.
     * The actual markup scheme is not mandated to anything specific, but is rather an
     * implementation detail with the logbook clients. If the client does not use any markup,
     * this field should be identical to the <code>description</code> field.
     * @return
     */
    public default String getSource(){
        return null;
    }

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
     * return the {@link Property} with name propertyName if it exists
     * on this log else return null.
     * 
     * @param propertyName
     * @return {@link Property} with name propertyName else null if no such
     *         property exists on this log.
     */
    public Property getProperty(String propertyName);

}
