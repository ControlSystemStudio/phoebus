package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.logbook.olog.ui.write.LogPropertiesEditorController;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogPropertiesEditorDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(LogPropertiesEditorDemo.class, args);
    }
    public static Messages msg;

    @Override
    public void start(Stage primaryStage) throws IOException {

        Map<String, String> tracAttributes = new HashMap<>();
        tracAttributes.put("id", "1234");
        tracAttributes.put("URL", "https://trac.epics.org/tickets/1234");
        Property track = PropertyImpl.of("Track",tracAttributes);

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("write/LogPropertiesEditor.fxml"));
        loader.setControllerFactory(clazz -> {
            try{
                if(clazz.isAssignableFrom(LogPropertiesEditorController.class)) {
                    return clazz.getConstructor(Collection.class).newInstance(Arrays.asList(track));
                }
            } catch (Exception e) {
                Logger.getLogger(LogEntryEditorStage.class.getName()).log(Level.SEVERE, "Failed to construct controller for log editor UI", e);
            }
            return null;
        });

        loader.load();
        final LogPropertiesEditorController controller = loader.getController();
        Node tree = loader.getRoot();

        VBox vbox = new VBox();
        vbox.getChildren().add(tree);
        vbox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        Scene scene = new Scene(vbox, 600, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(v -> {
            controller.getProperties().stream().forEach(p -> {
                System.out.println(p.getName());
                p.getAttributes().entrySet().stream().forEach(e -> {
                    System.out.println("     " + e.getKey() + " : " + e.getValue());
                });
            });
        });
    }

    private LogClient getDummyLogClient(){
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

            @Override
            public Collection<Property> listProperties(){

                Map<String, String> experimentAttributes = new HashMap<>();
                experimentAttributes.put("id", "1234");
                experimentAttributes.put("type", "XPD xray diffraction");
                experimentAttributes.put("scan-id", "6789");
                Property experimentProperty = PropertyImpl.of("Experiment", experimentAttributes);

                return Arrays.asList(experimentProperty);
            }
        };
    }

}
