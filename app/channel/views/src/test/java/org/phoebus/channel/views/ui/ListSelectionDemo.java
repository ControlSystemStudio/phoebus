package org.phoebus.channel.views.ui;

import java.io.IOException;
import java.util.Arrays;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

public class ListSelectionDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(ListSelectionDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("AddProperty.fxml"));
        loader.load();

        AddPropertyController controller = loader.getController();
        controller.setAvaibleOptions(Arrays.asList("prop1", "prop2", "prop3"));

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}
