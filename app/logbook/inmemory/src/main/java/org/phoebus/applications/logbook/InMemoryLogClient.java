package org.phoebus.applications.logbook;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;

import com.google.common.io.Files;

/**
 * A logbook which maintains logentries in memory. It is mainly for testing and debugging purpose.
 */
public class InMemoryLogClient implements LogClient{
    private static Logger logger = Logger.getLogger(InMemoryLogClient.class.getName());
    private final AtomicInteger logIdCounter;
    private final Map<Long, LogEntry> logEntries;

    private final Collection<Logbook> logbooks = Arrays.asList(LogbookImpl.of("Controls"),
                                                               LogbookImpl.of("Commissioning"),
                                                               LogbookImpl.of("Scratch Pad"));
    private final Collection<Tag> tags = Arrays.asList(TagImpl.of("Operations"),
                                                       TagImpl.of("Alarm"),
                                                       TagImpl.of("Example"));
    private final List<String> levels = Arrays.asList("Urgent", "Suggestion", "Info", "Request", "Problem");

    public InMemoryLogClient() {
        logEntries = new HashMap<Long, LogEntry>();
        logIdCounter = new AtomicInteger();
    }

    @Override
    public Collection<String> listLevels() {
        return levels;
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
        return logEntries.values();
    }

    @Override
    public LogEntry getLog(Long logId) {
        return logEntries.get(logId);
    }

    String prefix = "phoebus_tmp_file";
    @Override
    public LogEntry set(LogEntry log) {
        long id = logIdCounter.incrementAndGet();

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
                tempFile.deleteOnExit();
                return AttachmentImpl.of(tempFile);
            } catch (IOException e) {
                logger.log(Level.WARNING, "failed to get in memory attachment", e);
                return null;
            }
        }).collect(Collectors.toSet());

        logBuilder.setAttach(attachmentsBuilder);

        LogEntry logEntry = logBuilder.build();
        logEntries.put(id, logEntry);

        return logEntry;
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        Stream<LogEntry> searchStream = logEntries.values().stream();
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
    public Collection<Attachment> listAttachments(Long logId) {
        return logEntries.get(logId).getAttachments();
    }

}
