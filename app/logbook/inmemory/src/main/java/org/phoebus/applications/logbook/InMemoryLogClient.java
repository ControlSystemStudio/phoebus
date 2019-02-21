package org.phoebus.applications.logbook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;

import com.google.common.io.Files;

public class InMemoryLogClient implements LogClient{
    private final AtomicInteger logIdCounter;
    private final Map<Long, LogEntry> LogEntries;

    private final Collection<Logbook> logbooks = Arrays.asList(LogbookImpl.of("Controls"),
                                                               LogbookImpl.of("Commissioning"),
                                                               LogbookImpl.of("Scratch Pad"));
    private final Collection<Tag> tags = Arrays.asList(TagImpl.of("Operations"),
                                                       TagImpl.of("Alarm"),
                                                       TagImpl.of("Example"));

    public InMemoryLogClient() {
        LogEntries = new HashMap<Long, LogEntry>();
        logIdCounter = new AtomicInteger();
    }

    @Override
    public Collection<Logbook> listLogbooks() {
        return logbooks;
    }

    @Override
    public Collection<Tag> listTags() {
        return tags;
    }

    @Override
    public Collection<LogEntry> listLogs() {
        return LogEntries.values();
    }

    @Override
    public LogEntry getLog(Long logId) {
        return LogEntries.get(logId);
    }

    @Override
    public Collection<Property> listProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> listAttributes(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Collection<Attachment> listAttachments(Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAttachment(Long logId, Attachment attachment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAttachment(Long logId, String attachmentName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property getProperty(String property) {
        // TODO Auto-generated method stub
        return null;
    }
    String prefix = "phoebus_tmp_file";
    @Override
    public LogEntry set(LogEntry log) {
        long id = (long) logIdCounter.incrementAndGet();

        LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log(log);
        logBuilder.id(id);
        logBuilder.createdDate(Instant.now());

        Set<Attachment> attachmentsBuilder = log.getAttachments().stream().map(attachment -> {

            try {
                File file = attachment.getFile();
                int i = file.getName().lastIndexOf(".");
                String ext = null;
                if (i >= 0) {
                    ext = file.getName().substring(i);
                }
                File tempFile = File.createTempFile(prefix, ext);
                Files.copy(file, tempFile);
                return AttachmentImpl.of(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toSet());

        logBuilder.setAttach(attachmentsBuilder);
        return LogEntries.put(id, logBuilder.build());
    }

    @Override
    public Collection<LogEntry> set(Collection<LogEntry> logs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag set(Tag tag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag set(Tag tag, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook set(Logbook Logbook) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook set(Logbook logbook, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property set(Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry update(LogEntry log) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<LogEntry> update(Collection<LogEntry> logs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property update(Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag update(Tag tag, Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag update(Tag tag, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook update(Logbook logbook, Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook update(Logbook logbook, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry update(Property property, Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Attachment add(File local, Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry findLogById(Long logId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogsBySearch(String pattern) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogsByTag(String pattern) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogsByLogbook(String logbook) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName, String attributeName, String attributeValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        Stream<LogEntry> searchStream = LogEntries.values().stream();
        if(map.containsKey("start")) {
            searchStream = searchStream.filter(log -> {
                return log.getCreatedDate().isAfter(Instant.ofEpochSecond(Long.valueOf(map.get("start"))));
            });
        }
        if(map.containsKey("end")) {
            searchStream = searchStream.filter(log -> {
                return log.getCreatedDate().isBefore(Instant.ofEpochSecond(Long.valueOf(map.get("end"))));
            });
        }
        if (map.containsKey("search")) {
            final String searchString = map.get("search").replaceAll("\\*", "");
            if (!searchString.isEmpty()) {
                searchStream = searchStream.filter(log -> {
                    return log.getDescription().contains(searchString)||log.getTitle().contains(searchString);
                });
            }
        }
        return searchStream.collect(Collectors.toList());
    }

    @Override
    public void deleteTag(String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteLogbook(String logbook) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteProperty(String property) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(LogEntry log) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Long logId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Collection<LogEntry> logs) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Tag tag, Long logId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Tag tag, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Logbook logbook, Long logId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Logbook logbook, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Property property, Long logId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Property property, Collection<Long> logIds) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(String fileName, Long logId) {
        // TODO Auto-generated method stub
        
    }

}
