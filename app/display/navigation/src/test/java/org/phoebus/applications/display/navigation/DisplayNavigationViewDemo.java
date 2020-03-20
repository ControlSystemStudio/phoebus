package org.phoebus.applications.display.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

import org.phoebus.ui.javafx.ApplicationWrapper;

public class DisplayNavigationViewDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(DisplayNavigationViewDemo.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("DisplayNavigationView.fxml"));
        loader.load();

        DisplayNavigationViewController controller = loader.getController();

        URL url = getClass().getClassLoader().getResource("bob/root.bob");
        File rootFile = new File(url.getPath());

        controller.setRootFile(rootFile);

        Parent root = loader.getRoot();
        stage.setScene(new Scene(root, 400, 400));
        stage.show();

    }
}
