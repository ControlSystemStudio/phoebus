package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.logbook.olog.ui.write.LogPropertiesEditorController;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LogPropertiesEditorDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(LogPropertiesEditorDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        Map<String, String> tracAttributes = new HashMap<>();
        tracAttributes.put("id", "1234");
        tracAttributes.put("URL", "https://trac.epics.org/tickets/1234");
        Property track = PropertyImpl.of("Track",tracAttributes);

        Map<String, String> experimentAttributes = new HashMap<>();
        experimentAttributes.put("id", "1234");
        experimentAttributes.put("type", "XPD xray diffraction");
        experimentAttributes.put("scan-id", "6789");
        Property experimentProperty = PropertyImpl.of("Experiment", experimentAttributes);

        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("write/LogPropertiesEditor.fxml"));
        loader.load();
        final LogPropertiesEditorController controller = loader.getController();
        Node tree = loader.getRoot();

        controller.setSelectedProperties(Arrays.asList(track));
        controller.setAvailableProperties(Arrays.asList(experimentProperty));

        VBox vbox = new VBox();
        vbox.getChildren().add(tree);
        vbox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        Scene scene = new Scene(vbox, 400, 200);
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

}
