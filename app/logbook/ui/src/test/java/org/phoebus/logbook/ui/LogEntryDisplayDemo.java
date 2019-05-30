package org.phoebus.logbook.ui;

import static javafx.application.Platform.runLater;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LogEntryDisplayDemo extends ApplicationWrapper {

    ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        launch(LogEntryDisplayDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("LogEntry Display demo");

        VBox root = new VBox();

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("LogEntryDisplay.fxml"));
        loader.setRoot(root);
        loader.setController(new LogEntryController());
        loader.load();
        LogEntryController controller = loader.getController();
        root = loader.getRoot();

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();

        // Every few seconds add a new log entry

        ex.schedule(() -> {
            LogEntry log = LogEntryBuilder.log().description(
                    "Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.")
                    .createdDate(Instant.now()).build();
            runLater(() -> {
                controller.setLogEntry(log);
            });
        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {
            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));

            runLater(() -> {
                controller.setLogEntry(
                        LogEntryBuilder.log(controller.getLogEntry()).inLogbooks(logbooks).withTags(tags).build());
            });

        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {
            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));

            String path = "C:\\Users\\Kunal Shroff\\Pictures\\screenshot-git\\log-att";
            File folder = new File(path);
            List<File> listOfFiles = Arrays.asList(folder.listFiles());

            runLater(() -> {
                LogEntryBuilder lb = LogEntryBuilder.log()
                        .createdDate(Instant.now())
                        .description(
                        "Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.")
                        .withTags(new HashSet<Tag>(Arrays.asList(TagImpl.of("Orbit", "active"), TagImpl.of("Studies", "active"))))
                        .inLogbooks(new HashSet<Logbook>(Arrays.asList(LogbookImpl.of("Operations", "active"))));
                listOfFiles.forEach(file -> {
                    try {
                        lb.attach(AttachmentImpl.of(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                controller.setLogEntry(lb.build());
            });

        }, 2, TimeUnit.SECONDS);

    }

}
