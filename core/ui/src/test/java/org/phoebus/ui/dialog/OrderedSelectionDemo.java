package org.phoebus.ui.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.util.Arrays;

public class OrderedSelectionDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(OrderedSelectionDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("OrderedSelection.fxml"));
        loader.load();

        OrderedSelectionController controller = loader.getController();
        controller.setAvailableOptions(Arrays.asList("first", "second", "third", "forth"));
        controller.setOrderedSelectedOptions(Arrays.asList("forth"));

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}
