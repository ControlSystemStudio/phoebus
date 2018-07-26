package org.phoebus.applications.email;

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
        Parent root = FXMLLoader.load(EmailApp.class.getResource("ui/SimpleCreate.fxml"));
        Scene scene = new Scene(root, 600, 800);

        stage.setScene(scene);

        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}