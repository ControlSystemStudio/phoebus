package org.phoebus.applications.email.actions;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.IOException;
import java.util.logging.Level;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.ui.javafx.ImageCache;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
public class SendEmailAction extends MenuItem
{
    public SendEmailAction()
    {
        super();
        setText("Send Email...");
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/mail-send-16.png"));
        setOnAction(event -> 
        {
            try {
                Parent root = FXMLLoader.load(EmailApp.class.getResource("ui/SimpleCreate.fxml"));
                Scene scene = new Scene(root, 600, 800);
                Stage stage = new Stage();
                stage.setTitle("FXML Welcome");
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create email dialog", e);
            }
        });
    }
}
