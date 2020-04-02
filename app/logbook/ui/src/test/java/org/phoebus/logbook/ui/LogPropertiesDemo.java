package org.phoebus.logbook.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("LogProperties.fxml"));
        loader.load();
        LogPropertiesController controller = loader.getController();

        Map<String, String> tracAttributes = new HashMap<>();
        tracAttributes.put("id", "1234");
        tracAttributes.put("URL", "https://trac.epics.org/tickets/1234");
        Property track = PropertyImpl.of("Track",tracAttributes);

        Map<String, String> experimentAttributes = new HashMap<>();
        experimentAttributes.put("id", "1234");
        experimentAttributes.put("type", "XPD xray diffraction");
        experimentAttributes.put("scan-id", "6789");
        Property experimentProperty = PropertyImpl.of("Experiment", experimentAttributes);

        controller.setProperties(Arrays.asList(track, experimentProperty));

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}
