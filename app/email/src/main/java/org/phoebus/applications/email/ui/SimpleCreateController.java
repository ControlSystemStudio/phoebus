package org.phoebus.applications.email.ui;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.framework.workbench.ApplicationService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.HTMLEditorSkin;
import javafx.scene.web.HTMLEditorSkin.Command;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for dialog to create and send emails
 *
 * @author Kunal Shroff
 *
 */
public class SimpleCreateController {

    private static final Logger log = Logger.getLogger(SimpleCreateController.class.getCanonicalName());
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_HTML = "text/html";

    private final FileChooser fileChooser = new FileChooser();

    private ObservableList<String> supportedMimeTypes = FXCollections.observableArrayList(TEXT_PLAIN, TEXT_HTML);

    @FXML
    VBox mainVBox;
    @FXML
    TextField txtTo;
    @FXML
    TextField txtFrom;
    @FXML
    TextField txtSubject;

    @FXML
    ChoiceBox<String> choiceBox;

    @FXML
    VBox simpleTextVBox;
    @FXML
    TextArea textArea;
    @FXML
    ListView<String> listView;
    @FXML
    Button btnAtt;

    @FXML
    VBox htmlTextVBox;
    @FXML
    HTMLEditor htmlEditor;

    @FXML
    Button btnSend;
    @FXML
    Button btnCancel;

    @FXML
    public void send(Event event) {
        // TODO move to a job and move to Email service

        try {
            // Create a default MimeMessage object.
            EmailApp app = ApplicationService.findApplication(EmailApp.NAME);
            Message message = new MimeMessage(app.getSession());

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(txtFrom.getText()));

            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(txtTo.getText()));

            // Set Subject: header field
            message.setSubject(txtSubject.getText());

            Multipart multipart = new MimeMultipart();

            // Create the message part
            // This will depend on the body type

            BodyPart messageBodyPart;
            switch (choiceBox.getValue()) {
            case TEXT_HTML:
                messageBodyPart = new MimeBodyPart();
                String htmlText = htmlEditor.getHtmlText();
                messageBodyPart.setContent(htmlText, "text/html");
                multipart.addBodyPart(messageBodyPart);
                break;
            default:
                // Text
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(textArea.getText());
                multipart.addBodyPart(messageBodyPart);
                // Attachments
// TODO Fix access to javax.annotations that clashes with JDK9 module, see #52
                for (String file : listView.getItems()) {
                    messageBodyPart = new MimeBodyPart();
                    String filename = file;
                    DataSource source = new FileDataSource(filename);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(filename);
                    multipart.addBodyPart(messageBodyPart);
                }
                break;
            }

            // Send the complete message parts
            message.setContent(multipart);
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
    public void initialize() {
        choiceBox.setItems(supportedMimeTypes);
        choiceBox.setValue("text/plain");
        recomputeTextArea();
        choiceBox.setOnAction(value -> {
            recomputeTextArea();
        });
        
        htmlEditor.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                event.consume();

                final HTMLEditorSkin skin = (HTMLEditorSkin) htmlEditor.getSkin();

                    try
                    {
                        // Invoke the private executeCommand(Command.INSERT_NEW_LINE.getCommand(), null);
                        final Method method = skin.getClass().getDeclaredMethod("executeCommand", String.class, String.class);
                        method.setAccessible(true);
                        method.invoke(skin, Command.INSERT_NEW_LINE.getCommand(), null);
                    }
                    catch (Throwable ex)
                    {
                        throw new RuntimeException("Cannot hack around ENTER", ex);
                    }
            }
        });
    }

    private void recomputeTextArea() {
        simpleTextVBox.setVisible(choiceBox.getValue().equals(TEXT_PLAIN));
        simpleTextVBox.setManaged(choiceBox.getValue().equals(TEXT_PLAIN));
        htmlTextVBox.setVisible(choiceBox.getValue().equals(TEXT_HTML));
        htmlTextVBox.setManaged(choiceBox.getValue().equals(TEXT_HTML));
        mainVBox.requestLayout();
    }

    @FXML
    public void cancel(Event event) {
        // Do nothing right now
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void attachments(Event event) {
        Stage stage = (Stage) btnAtt.getScene().getWindow();
        List<File> list = new ArrayList<File>();
        list = fileChooser.showOpenMultipleDialog(stage);
        listView.setItems(FXCollections
                .observableArrayList(list.stream().map(File::getAbsolutePath).collect(Collectors.toList())));
    }
}
