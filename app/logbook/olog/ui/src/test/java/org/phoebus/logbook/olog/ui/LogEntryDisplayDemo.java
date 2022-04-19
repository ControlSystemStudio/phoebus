package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.application.Platform.runLater;

public class LogEntryDisplayDemo extends ApplicationWrapper {

    ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        launch(LogEntryDisplayDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("LogEntry Display demo");

        BorderPane root;

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("LogEntryDisplay.fxml"));
        loader.setControllerFactory(clazz -> {
            try {
                if (clazz.isAssignableFrom(LogEntryTableViewController.class)) {
                    return clazz.getConstructor(LogClient.class)
                            .newInstance(getLogClient());
                } else if (clazz.isAssignableFrom(AdvancedSearchViewController.class)) {
                    return clazz.getConstructor(LogClient.class)
                            .newInstance(getLogClient());
                } else if (clazz.isAssignableFrom(LogPropertiesController.class)) {
                    return clazz.getConstructor().newInstance();
                } else if (clazz.isAssignableFrom(AttachmentsPreviewController.class)) {
                    return clazz.getConstructor().newInstance();
                } else if (clazz.isAssignableFrom(LogEntryCellController.class)) {
                    return clazz.getConstructor().newInstance();
                } else if (clazz.isAssignableFrom(LogEntryDisplayController.class)) {
                    return clazz.getConstructor().newInstance();
                } else if (clazz.isAssignableFrom(MergedLogEntryDisplayController.class)) {
                    return clazz.getConstructor(LogClient.class).newInstance(getLogClient());
                } else if (clazz.isAssignableFrom(SingleLogEntryDisplayController.class)) {
                    return clazz.getConstructor(LogClient.class).newInstance(getLogClient());
                } else {
                    throw new RuntimeException("No controller for class " + clazz.getName());
                }
            } catch (Exception e) {
                Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log calendar view", e);
            }
            return null;
        });
        loader.load();
        LogEntryDisplayController controller = loader.getController();
        root = loader.getRoot();

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();

        // Every few seconds add a new log entry

        ex.schedule(() -> {
            OlogLog ologLog = new OlogLog(1L);
            ologLog.setDescription("Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.");
            ologLog.setCreatedDate(Instant.now());
            runLater(() -> {
                controller.setLogEntry(ologLog);
            });
        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {
            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));

            LogEntry logEntry = controller.getLogEntry();

            runLater(() -> {
                OlogLog anotherLog = new OlogLog(2L);
                anotherLog.setDescription(controller.getLogEntry().getDescription());
                anotherLog.setCreatedDate(controller.getLogEntry().getCreatedDate());
                anotherLog.setTags(tags);
                anotherLog.setLogbooks(logbooks);
                controller.setLogEntry(anotherLog);
            });

        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {
            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));


            Map<String, String> tracAttributes = new HashMap<>();
            tracAttributes.put("id", "1234");
            tracAttributes.put("URL", "https://trac.epics.org/tickets/1234");
            Property track = PropertyImpl.of("Track", tracAttributes);

            Map<String, String> experimentAttributes = new HashMap<>();
            experimentAttributes.put("id", "1234");
            experimentAttributes.put("type", "XPD xray diffraction");
            experimentAttributes.put("scan-id", "6789");
            Property experimentProperty = PropertyImpl.of("Experiment", experimentAttributes);

            runLater(() -> {
                OlogLog ologLog = new OlogLog(3L);
                ologLog.setCreatedDate(Instant.now());
                ologLog.setTitle("A report on the orbit studies");
                ologLog.setDescription("Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.");
                ologLog.setTags(new HashSet(Arrays.asList(TagImpl.of("Orbit", "active"), TagImpl.of("Studies", "active"))));
                ologLog.setLogbooks(new HashSet(Arrays.asList(LogbookImpl.of("Operations", "active"))));
                ologLog.setProperties(Arrays.asList(track, experimentProperty));
                List<Attachment> attachments = new ArrayList<>();
                OlogAttachment attachment1 = new OlogAttachment("image_1.png");
                attachment1.setFileName("image_1.png");
                attachment1.setContentType("image");
                OlogAttachment attachment2 = new OlogAttachment("file_phoebus.txt");
                attachment2.setFileName("file_phoebus.txt");
                attachment2.setContentType("text");
                attachments.add(attachment1);
                attachments.add(attachment2);
                ologLog.setAttachments(attachments);
                controller.setLogEntry(ologLog);
            });

        }, 2, TimeUnit.SECONDS);

    }

    private LogClient getLogClient() {
        return new LogClient() {
            @Override
            public LogEntry set(LogEntry log) throws LogbookException {
                return null;
            }

            @Override
            public LogEntry getLog(Long logId) {
                return null;
            }

            @Override
            public InputStream getAttachment(Long logId, String attachmentName) throws LogbookException {
                return this.getClass().getClassLoader().getResourceAsStream(attachmentName);
            }

            @Override
            public Collection<Attachment> listAttachments(Long logId) {
                return null;
            }

            @Override
            public List<LogEntry> findLogs(Map<String, String> map) {
                return null;
            }

            @Override
            public Collection<LogEntry> listLogs() {
                return null;
            }
        };
    }
}
