package org.phoebus.logbook.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.phoebus.logging.AttachmentImpl;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logging.Logbook;
import org.phoebus.logging.LogbookImpl;
import org.phoebus.logging.Tag;
import org.phoebus.logging.TagImpl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LogEntryTableDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // TODO Auto-generated method stub

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("LogEntryTable.fxml"));
        loader.load();
        LogEntryTableController controller = loader.getController();
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

        String path = "C:\\Users\\Kunal Shroff\\Pictures\\screenshot-git\\att";
        File folder = new File(path);
        List<File> listOfFiles = Arrays.asList(folder.listFiles());

        for (int i = 0; i < 10; i++) {
            LogEntryBuilder lb = LogEntryBuilder.log()
                           .title("log "+ i)
                           .description("First line for log " + i)
                           .createdDate(Instant.now())
                           .inLogbooks(logbooks)
                           .withTags(tags);
            for (int j = 0; j < i; j++) {
                lb.appendDescription("Some additional log text");
            }
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
}
