package org.phoebus.logbook.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ListSelectionDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(ListSelectionDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        loader.load();
        ListSelectionController controller = loader.getController();
        controller.setAvailable(Arrays.asList("a", "b", "c"));
        controller.setOnApply((List<String> t) -> {
            System.out.println("On close the final selection was: ");
            t.forEach(System.out::print);
            return true;
        });
        controller.setOnCancel((List<String> t) -> {
            primaryStage.close();
            return true;
        });

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}
