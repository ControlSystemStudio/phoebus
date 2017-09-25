/**
 * 
 */
package org.phoebus.applications.email;

import java.io.IOException;
import java.util.Properties;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Kunal Shroff
 *
 */
public class EmailApp implements AppResourceDescriptor {

    public static final String NAME = "email";
    public static final String DISPLAY_NAME = "Email App";

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Create the session
     */
    @Override
    public void start() {
    }

    @Override
    public AppInstance create() {
        try {
            Parent root = FXMLLoader.load(this.getClass().getResource("ui/SimpleCreate.fxml"));
            Scene scene = new Scene(root, 600, 800);

            Stage stage = new Stage();
            stage.setTitle("FXML Welcome");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Handle resources like mailto:shroffk@....
     */
    @Override
    public AppInstance create(String resource) {
        return null;
    }

}
