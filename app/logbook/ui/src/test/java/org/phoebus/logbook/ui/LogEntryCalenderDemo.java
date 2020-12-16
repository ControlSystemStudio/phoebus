package org.phoebus.logbook.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.logbook.ui.write.LogEntryEditorStage;
import org.phoebus.ui.javafx.ApplicationWrapper;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LogEntryCalenderDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(LogEntryCalenderDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("LogEntryCalenderView.fxml"));
        loader.setControllerFactory(clazz -> {
            try {
                if(clazz.isAssignableFrom(LogEntryCalenderViewController.class)){
                    return clazz.getConstructor(LogClient.class)
                            .newInstance(getLogClient());
                }
                else if(clazz.isAssignableFrom(AdvancedSearchViewController.class)){
                    return clazz.getConstructor(LogClient.class)
                            .newInstance(getLogClient());
                }
            } catch (Exception e) {
                Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log calendar view", e);
            }
            return null;
        });
        loader.load();
        LogEntryCalenderViewController controller = loader.getController();
        Parent root = loader.getRoot();

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();

        List<LogEntry> logs = new ArrayList<LogEntry>();

        Set<Tag> tags = new HashSet<Tag>();
        tags.add(TagImpl.of("tag1", "active"));
        tags.add(TagImpl.of("tag2", "active"));

        Set<Logbook> logbooks = new HashSet<Logbook>();
        logbooks.add(LogbookImpl.of("logbook1", "active"));
        logbooks.add(LogbookImpl.of("logbook2", "active"));

        String path = "C:\\Users\\Kunal Shroff\\Pictures\\screenshot-git\\log-att";
        File folder = new File(path);
        List<File> listOfFiles = Arrays.asList(folder.listFiles());

        for (int i = 0; i < 10; i++) {
            LogEntryBuilder lb = LogEntryBuilder.log()
                           .owner("Owner")
                           .title("log "+ i)
                           .description("First line for log " + i)
                           .createdDate(Instant.now().minusSeconds(i*60*60))
                           .inLogbooks(logbooks)
                           .withTags(tags);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sb.append("Some additional log text");
            }
            lb.appendDescription(sb.toString());
            listOfFiles.forEach(file -> {
                try {
                    lb.attach(AttachmentImpl.of(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            });
            logs.add(lb.build());
        }

        controller.setLogs(logs);
    }

    private LogClient getLogClient(){
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
