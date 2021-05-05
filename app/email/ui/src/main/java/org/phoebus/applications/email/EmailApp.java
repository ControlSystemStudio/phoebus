/**
 *
 */
package org.phoebus.applications.email;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.phoebus.applications.email.ui.EmailDialogController;
import org.phoebus.email.EmailPreferences;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Kunal Shroff
 */
public class EmailApp implements AppResourceDescriptor {

    /** Logger for email related messages */
    public static final Logger logger = Logger.getLogger(EmailApp.class.getPackageName());
    public static final String NAME = "email";
    public static final String DISPLAY_NAME = "Send Email";

    private Session session;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    /**
     * Create the {@link Session} at startup
     */
    @Override
    public void start()
    {
        final Properties props = new Properties();
        props.put("mail.smtp.host", EmailPreferences.mailhost);
        props.put("mail.smtp.port", EmailPreferences.mailport);

        final String username = EmailPreferences.username;
        final String password = EmailPreferences.password;

        if (!username.isEmpty() && !password.isEmpty()) {
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
    public AppInstance create()
    {
        try {
            
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EmailApp.class.getResource("ui/EmailDialog.fxml"));
            Parent root = loader.load();
            final EmailDialogController controller = loader.getController();
            
            Scene scene = new Scene(root, 600, 800);

            Stage stage = new Stage();
            stage.setTitle("Send Email");
            stage.setScene(scene);
            controller.setSnapshotNode(DockPane.getActiveDockPane());

            stage.show();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create email dialog", e);
        }
        return null;
    }

    /**
     * Handle resources like mailto:shroffk@....
     */
    @Override
    public AppInstance create(URI resource)
    {
        return create();
    }

    /**
     *
     * @return {@link Session} connection factory needed to create and send emails.
     */
    public Session getSession()
    {
        return session;
    }
}
