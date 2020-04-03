package org.phoebus.logbook.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("LogProperties.fxml"));
        loader.load();
        final LogPropertiesController controller = loader.getController();
        Node tree = loader.getRoot();

        CheckBox checkBox = new CheckBox();
        BooleanProperty editable = new SimpleBooleanProperty();
        checkBox.selectedProperty().bindBidirectional(editable);

        controller.setProperties(Arrays.asList(track, experimentProperty));
        editable.addListener((observable, oldValue, newValue) -> {
            controller.setEditable(newValue);
        });

        VBox vbox = new VBox();
        vbox.getChildren().add(checkBox);
        vbox.getChildren().add(tree);
        primaryStage.setScene(new Scene(vbox, 400, 400));
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
