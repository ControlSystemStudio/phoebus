package org.phoebus.applications.email.ui;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Controller for dialog to create and send emails
 * 
 * @author Kunal Shroff
 *
 */
public class SimpleCreateController {

    private static final Logger log = Logger.getLogger(SimpleCreateController.class.getCanonicalName());

    @FXML
    TextField txtTo;
    @FXML
    TextField txtFrom;
    @FXML
    TextField txtSubject;

    @FXML
    TextArea textArea;

    @FXML
    Button btnSend;
    @FXML
    Button btnCancel;

    @FXML
    public void send(Event event) {
        // TODO move to a job and move to Email service
        Properties props = new Properties();
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        props.put("mail.smtp.host", prefs.get("mailhost", "localhost"));
        props.put("mail.smtp.port", "25");

        Session session = Session.getDefaultInstance(props);
        try {
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(txtFrom.getText()));

            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(txtTo.getText()));

            // Set Subject: header field
            message.setSubject(txtSubject.getText());

            String text = textArea.getText();
            // Now set the actual message
            message.setText(text);

            // Send message
            Transport.send(message);
            log.info("Sent message successfully....");

            Stage stage = (Stage) btnSend.getScene().getWindow();
            stage.close();

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cancel(Event event) {
        // Do nothing right now
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}
