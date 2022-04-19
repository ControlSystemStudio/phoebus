package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;
import org.phoebus.logbook.olog.ui.LogEntryTableViewController.TableViewListItem;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static javafx.application.Platform.runLater;

public class LogEntryCellDemo extends ApplicationWrapper {

    ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        launch(LogEntryCellDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("LogEntry Cell Demo");

        VBox root;

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("LogEntryCell.fxml"));
        loader.load();
        LogEntryCellController controller = loader.getController();
        root = loader.getRoot();

        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();

        // Every few seconds add a new log entry

        ex.schedule(() -> {
            LogEntry log = LogEntryBuilder.log().id(1L).description(
                    "Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.")
                    .createdDate(Instant.now())
                    .owner("nsls2-user")
                    .build();
            runLater(() -> {
                controller.setLogEntry(new TableViewListItem(log, true));
            });
        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {

            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));

            LogEntry log = LogEntryBuilder.log().description(
                    "Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.")
                    .createdDate(Instant.now())
                    .inLogbooks(logbooks)
                    .id(2L)
                    .withTags(tags)
                    .owner("nsls2-user")
                    .build();

            runLater(() -> {
                controller.setLogEntry(new TableViewListItem(log, true));
            });
        }, 2, TimeUnit.SECONDS);

        ex.schedule(() -> {
            Set<Tag> tags = new HashSet<Tag>();
            tags.add(TagImpl.of("tag1", "active"));
            tags.add(TagImpl.of("tag2", "active"));
            Set<Logbook> logbooks = new HashSet<Logbook>();
            logbooks.add(LogbookImpl.of("logbook1", "active"));
            logbooks.add(LogbookImpl.of("logbook2", "active"));


            File imageFile = null;
            File textFile = null;
            try {
                imageFile = new File(this.getClass().getClassLoader().getResource("image_1.png").toURI());
                textFile = new File(this.getClass().getClassLoader().getResource("file_phoebus.txt").toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            List<File> listOfFiles = Arrays.asList(imageFile, textFile);

            runLater(() -> {
                LogEntryBuilder lb = LogEntryBuilder.log()
                        .createdDate(Instant.now())
                        .title("A simple log Entry with some tags and logbooks")
                        .id(3L)
                        .description(
                                "Fast correctors for the vertical orbit have glitched to near saturation. Archiver shows there have been several episodes the past 24 hrs. Appears that FOFB in vertical plane might have momentary bad BPM reading.")
                        .withTags(new HashSet<Tag>(Arrays.asList(TagImpl.of("Orbit", "active"), TagImpl.of("Studies", "active"))))
                        .inLogbooks(new HashSet<Logbook>(Arrays.asList(LogbookImpl.of("Operations", "active"), LogbookImpl.of("Electrical", "active"))))
                        .owner("nsls2-user");
                listOfFiles.forEach(file -> {
                    try {
                        lb.attach(AttachmentImpl.of(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                controller.setLogEntry(new TableViewListItem(lb.build(), true));
            });

        }, 2, TimeUnit.SECONDS);

    }

}
