package org.phoebus.applications.email;

import org.phoebus.applications.email.ui.SimpleCreateController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EmailDialogDemo extends Application
{

    @Override
    public void start(final Stage stage) throws Exception
    {
        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EmailApp.class.getResource("ui/SimpleCreate.fxml"));
        Parent root = loader.load();
        SimpleCreateController controller = loader.getController();

        controller.setSnapshotNode(root);

        Scene scene = new Scene(root, 600, 800);

        stage.setScene(scene);

        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}