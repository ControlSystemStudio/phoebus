/**
 * 
 */
package org.phoebus.applications.email;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

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

    private static final Logger log = Logger.getLogger(AppResourceDescriptor.class.getName());
    public static final String NAME = "email";
    public static final String DISPLAY_NAME = "Email App";

    private Session session;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /**
     * Create the {@link Session} at startup
     */
    @Override
    public void start() {
        Properties props = new Properties();
        Properties defaultProps = new Properties();
        try {
            defaultProps.load(AppResourceDescriptor.class.getResourceAsStream("/email.properties"));
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to read default preferences", e);
        }
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

        props.put("mail.smtp.host", prefs.get("mailhost", defaultProps.getProperty("mailhost")));
        props.put("mail.smtp.port", prefs.get("mailport", defaultProps.getProperty("mailport")));

        String username = prefs.get("username", defaultProps.getProperty("username"));
        String password = prefs.get("password", defaultProps.getProperty("password"));
        if (username != null && password != null) {
            PasswordAuthentication auth = new PasswordAuthentication(username, password);
            session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return auth;
                }
            });
        } else {
            session = Session.getDefaultInstance(props);
        }
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
            log.log(Level.WARNING, "Failed to create email dialog", e);
        }
        return null;
    }

    /**
     * Handle resources like mailto:shroffk@....
     */
    @Override
    public AppInstance create(String resource) {
        return create();
    }

    /**
     * 
     * @return {@link Session} connection factory needed to create and send emails.
     */
    public Session getSession() {
        return session;
    }
}
