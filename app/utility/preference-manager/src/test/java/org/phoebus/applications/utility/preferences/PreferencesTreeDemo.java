package org.phoebus.applications.utility.preferences;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;

public class PreferencesTreeDemo extends ApplicationWrapper {


    public static void main(String[] args)
    {
        launch(PreferencesTreeDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("PreferencesTree.fxml"));
        loader.load();
        PreferencesTreeController controller = loader.getController();

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }
}
