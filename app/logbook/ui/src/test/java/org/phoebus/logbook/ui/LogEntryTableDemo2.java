package org.phoebus.logbook.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LogEntryTableDemo2 extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(LogEntryTableDemo2.class, args);
    }

    private LogEntryTableControl control;

    @Override
    public void start(Stage primaryStage) throws Exception {
        control = new LogEntryTableControl();
        primaryStage.setScene(new Scene(control, 400, 400));
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
                           .createdDate(Instant.now())
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
        control.setLogs(logs);
    }
}
