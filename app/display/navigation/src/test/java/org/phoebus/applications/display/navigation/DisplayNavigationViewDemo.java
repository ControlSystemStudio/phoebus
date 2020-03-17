package org.phoebus.applications.display.navigation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class DisplayNavigationViewDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("DisplayNavigationView.fxml"));
        loader.load();

        DisplayNavigationViewController controller = loader.getController();

        URL url = this.getClass().getResource("test_root.opi");
        File rootFile = new File(url.getPath());

        controller.setRootFile(rootFile);

        Parent root = loader.getRoot();
        stage.setScene(new Scene(root, 400, 400));
        stage.show();

    }
}
