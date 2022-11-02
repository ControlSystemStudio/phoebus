package org.phoebus.channel.views.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;

import static org.phoebus.channelfinder.Property.Builder.property;

public class AddPropertyDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(AddPropertyDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("AddProperty.fxml"));
        loader.load();

        AddPropertyController controller = loader.getController();
        controller.setAvaibleOptions(Arrays.asList(property("prop1").owner("owner1").build(),
                                                   property("prop2").owner("owner2").build(),
                                                   property("prop3").owner("owner3").build()));

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}
