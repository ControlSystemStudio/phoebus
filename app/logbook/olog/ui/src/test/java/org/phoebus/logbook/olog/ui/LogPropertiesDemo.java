package org.phoebus.logbook.olog.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LogPropertiesDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(LogPropertiesDemo.class, args);
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

        Map<String, String> databrowser = new HashMap<>();
        databrowser.put("name", "vaccum db");
        databrowser.put("file", "sss");
        Property resourceProperty = PropertyImpl.of("Resource", databrowser);

        Map<String, String> empty = new HashMap<>();
        empty.put("name", null);
        empty.put("file", "");
        Property emptyProperty = PropertyImpl.of("empty", empty);

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);

        loader.setLocation(this.getClass().getResource("LogProperties.fxml"));
        loader.load();
        final LogPropertiesController controller = loader.getController();
        Node tree = loader.getRoot();
        
        CheckBox checkBox = new CheckBox();
        BooleanProperty editable = new SimpleBooleanProperty();
        checkBox.selectedProperty().bindBidirectional(editable);

        controller.setProperties(Arrays.asList(track, experimentProperty, resourceProperty, emptyProperty));
        editable.addListener((observable, oldValue, newValue) -> {
            controller.setEditable(newValue);
        });

        VBox vbox = new VBox();
        vbox.getChildren().add(checkBox);
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
