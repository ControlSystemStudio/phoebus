package org.phoebus.logging;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Eric Berryman taken from shroffk
 * 
 */
public interface LogClient {

    /**
     * Get a list of all the logbooks currently existings
     * 
     * @return string collection of logbooks
     */
    public Collection<Logbook> listLogbooks();

    /**
     * Get a list of all the tags currently existing
     * 
     * @return string collection of tags
     */
    public Collection<Tag> listTags();

    /**
     * Get a list of all the Properties currently existing
     * 
     * @return
     */
    public Collection<Property> listProperties();

    /**
     * List all the active attributes associated with the property
     * <tt>propertyName</tt> property must exist, name != null
     * 
     * @param propertyName
     * @return
     */
    public Collection<String> listAttributes(String propertyName);

    /**
     * Return all the logs. ***Warning can return a lot of data***
     * 
     * @return Collection of all LogEntry entires
     */
    public Collection<LogEntry> listLogs();

    /**
     * Returns a LogEntry that exactly matches the logId <tt>logId</tt>
     * 
     * @param logId LogEntry id
     * @return LogEntry object
     */
    public LogEntry getLog(Long logId);

    /**
     * Returns a collection of attachments that matches the logId <tt>logId</tt>
     * 
     * @param logId LogEntry id
     * @return attachments collection object
     */
    public Collection<Attachment> listAttachments(Long logId);

    /**
     * 
     * @param logId
     * @param attachment
     * @return {@link InputStream} to the attachment file
     */
    public InputStream getAttachment(Long logId, Attachment attachment);

    /**
     * 
     * @param logId
     * @param attachment
     * @return {@link InputStream} to the attachment file
     */
    public InputStream getAttachment(Long logId, String attachmentName);

    /**
     * return the complete property <tt>property</tt>
     * 
     * @param property
     * @return
     */
    public Property getProperty(String property);

    /**
     * Set a single LogEntry <tt>log</tt>, if the LogEntry already exists it is replaced.
     * Destructive operation
     * 
     * TODO: check validity of LogEntry entry represented by builder
     * 
     * TODO: (shroffk) should there be anything returned? XXX: creating logs
     * with same subject allowed?
     * 
     * @param log
     *            the LogEntry to be added

     */
    public LogEntry set(LogEntry log);

    /**
     * Set a set of logs Destructive operation.
     * 
     * TODO: (shroffk) should anything be returned? and should be returned from
     * the service?
     * 
     * @param logs collection of logs to be added
     */
    @Deprecated
    public Collection<LogEntry> set(Collection<LogEntry> logs);

    /**
     * Set a Tag <tt>tag</tt> with no associated logs to the database.
     * 
     * TODO: validity check,
     * 
     * @param tag

     */
    public Tag set(Tag tag);

    /**
     * Set tag <tt>tag</tt> on the set of logs <tt>logIds</tt> and remove it
     * from all others
     * 
     * TODO: all logIds should exist/ service should do proper transactions.
     * 
     * @param tag
     * @param logIds

     */
    public Tag set(Tag tag, Collection<Long> logIds);

    /**
     * Set a new logbook <tt>logbook</tt> with no associated logs.
     * 
     * @param Logbook

     */
    public Logbook set(Logbook Logbook);

    /**
     * Set Logbook <tt>logbook</tt> to the logs <tt>logIds</tt> and remove it
     * from all other logs TODO: all logids should exist, no nulls, check
     * transaction
     * 
     * @param logbook
     *            logbook builder
     * @param logIds
     *            LogEntry ids

     */
    public Logbook set(Logbook logbook, Collection<Long> logIds);

    /**
     * Create or replace property <tt>property</tt>
     * 
     * TODO: test creation of a new property, test changing this property, test
     * old LogEntry entries still have old property structure
     * 
     * @param property
     * @return

     */
    public Property set(Property property);

    /**
     * Update a LogEntry entry <tt>LogEntry </tt>
     * 
     * @param log
     * @return the updated LogEntry entry

     */
    public LogEntry update(LogEntry log);

    /**
     * Update a set of logs
     * 
     * @param logs
     *            set of logs to be added

     */
    public Collection<LogEntry> update(Collection<LogEntry> logs);

    /**
     * Update an existing property,
     * 
     * TODO: check non destructive nature, old attributes should not be touched.
     * old entries should have old property.
     * 
     * @param property
     * @return
     */
    public Property update(Property property);

    /**
     * Update Tag <tt>tag </tt> by adding it to LogEntry with name <tt>logName</tt>
     * 
     * TODO: logid valid.
     * 
     * @param tag
     *            tag builder
     * @param logId
     *            LogEntry id the tag to be added

     */
    public Tag update(Tag tag, Long logId);

    /**
     * Update the Tag <tt>tag</tt> by adding it to the set of the logs with ids
     * <tt>logIds</tt>
     * 
     * TODO: Transactional nature,
     * 
     * @param tag
     *            tag builder
     * @param logIds
     *            collection of LogEntry ids

     */
    public Tag update(Tag tag, Collection<Long> logIds);

    /**
     * Add Logbook <tt>logbook</tt> to the LogEntry <tt>logId</tt>
     * 
     * @param logbook
     *            logbook builder
     * @param logId
     *            LogEntry id

     */
    public Logbook update(Logbook logbook, Long logId);

    /**
     * 
     * TODO: transaction check
     * 
     * @param logIds
     * @param logbook

     */
    public Logbook update(Logbook logbook, Collection<Long> logIds);

    /**
     * Update Property <tt>property</tt> by adding it to LogEntry with id
     * <tt>logId</tt>
     * 
     * TODO : service invalid payload, need attribute and value
     * 
     * @param property
     *            property builder
     * @param logId
     *            LogEntry id the property to be added

     */
    public LogEntry update(Property property, Long logId);

    /**
     * @param logId
     * @param local

     */
    public Attachment add(File local, Long logId);

    /**
     * 
     * @param logId
     * @return

     */
    @Deprecated
    public LogEntry findLogById(Long logId);

    /**
     * 
     * @param pattern
     * @return collection of LogEntry objects

     */
    public List<LogEntry> findLogsBySearch(String pattern);

    /**
     * 
     * @param pattern
     * @return collection of LogEntry objects

     */
    public List<LogEntry> findLogsByTag(String pattern);

    /**
     * This function is a subset of queryLogs - should it be removed??
     * <p>
     * TODO: add the usage of patterns and implement on top of the general query
     * using the map
     * 
     * @param logbook
     *            logbook name
     * @return collection of LogEntry objects

     */
    public List<LogEntry> findLogsByLogbook(String logbook);

    /**
     * This function is a subset of queryLogs should it be removed??
     * <p>
     * search for logs with property <tt>property</tt> and optionally value
     * matching pattern<tt>propertyValue</tt>
     * 
     * @param property
     * @return

     */
    public List<LogEntry> findLogsByProperty(String propertyName, String attributeName, String attributeValue);

    /**
     * 
     * @param propertyName
     * @return
     */
    public List<LogEntry> findLogsByProperty(String propertyName);

    /**
     * Query for logs based on the criteria specified in the map
     * 
     * @param map
     * @return collection of LogEntry objects
     */
    public List<LogEntry> findLogs(Map<String, String> map);

    /**
     * Remove {tag} from all logs
     * 
     * @param tag
     */
    public void deleteTag(String tag);

    /**
     * 
     * @param logbook
     * @throws LogFinderException
     */
    public void deleteLogbook(String logbook);

    /**
     * Delete the property with name <tt>property</tt>
     * 
     * @param property

     */
    public void deleteProperty(String property);

    /**
     * Remove the LogEntry identified by <tt>log</tt>
     * 
     * @param LogEntry LogEntry to be removed

     */
    public void delete(LogEntry log);

    /**
     * Remove the LogEntry identified by <tt>log</tt>
     * 
     * @param logId
     *            LogEntry id LogEntry id to be removed

     */
    public void delete(Long logId);

    /**
     * Remove the LogEntry collection identified by <tt>log</tt>
     * 
     * @param logs
     *            logs to be removed

     */
    public void delete(Collection<LogEntry> logs);

    /**
     * Remove tag <tt>tag</tt> from the LogEntry with the id <tt>logId</tt>
     * 
     * @param tag
     * @param logId
     */
    public void delete(Tag tag, Long logId);

    /**
     * Remove the tag <tt>tag </tt> from all the logs <tt>logNames</tt>
     * 
     * @param tag
     * @param logIds

     */
    public void delete(Tag tag, Collection<Long> logIds);

    /**
     * Remove logbook <tt>logbook</tt> from the LogEntry with name <tt>logName</tt>
     * 
     * @param logbook
     *            logbook builder
     * @param logId
     *            LogEntry id
     */
    public void delete(Logbook logbook, Long logId);

    /**
     * Remove the logbook <tt>logbook</tt> from the set of logs <tt>logIds</tt>
     * 
     * @param logbook
     * @param logIds
     */
    public void delete(Logbook logbook, Collection<Long> logIds);

    /**
     * Remove property <tt>property</tt> from the LogEntry with id <tt>logId</tt>
     * TODO: Should this be it's own service?
     * 
     * @param property
     *            property builder
     * @param logId
     *            LogEntry id
     */
    public void delete(Property property, Long logId);

    /**
     * Remove the property <tt>property</tt> from the set of logs
     * <tt>logIds</tt>
     * 
     * @param property
     * @param logIds
     */
    public void delete(Property property, Collection<Long> logIds);

    /**
     * Remove file attachment from LogEntry <tt>logId<tt>
     * 
     * TODO: sardine delete hangs up, using jersey for delete
     * 
     * @param String
     *            fileName
     * @param Long
     *            logId
     */
    public void delete(String fileName, Long logId);

}
