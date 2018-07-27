package org.phoebus.applications.email.ui;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.activation.DataHandler;
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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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
    SplitPane simpleTextVBox;
    @FXML
    TextArea textArea;

    private final ImagesTab att_images = new ImagesTab();

    @FXML
    TabPane attachmentTabs;

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

            // Set To: header field of the header.
            String txt = txtTo.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtTo::requestFocus);
                return;
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(txt));

            // Set From: header field of the header.
            txt = txtFrom.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtFrom::requestFocus);
                return;
            }
            message.setFrom(new InternetAddress(txt));

            // Set Subject: header field
            txt = txtSubject.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtSubject::requestFocus);
                return;
            }
            message.setSubject(txt);

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
                for (Image image : att_images.getImages()) {
                    messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setDataHandler(new DataHandler(new ImageDataSource(image)));
                    messageBodyPart.setFileName("Image");
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
        txtFrom.setPromptText("Enter your email address");
        txtTo.setPromptText("Enter receipient's email address(es)");
        txtTo.setTooltip(new Tooltip("Enter receipient's email address(es), comma-separated"));
        txtSubject.setPromptText("Enter Subject");

        choiceBox.setItems(supportedMimeTypes);
        choiceBox.setValue("text/plain");
        recomputeTextArea();
        choiceBox.setOnAction(value -> {
            recomputeTextArea();
        });

        // Patch for bug in HTMLEditor that doesn't allow entering new lines on Linux
        // https://stackoverflow.com/questions/11269632/javafx-hmtleditor-doesnt-react-on-return-key
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

        simpleTextVBox.setDividerPositions(0.6, 0.9);

        final Tab att_files = new Tab("Files");
        att_files.setClosable(false);
        attachmentTabs.getTabs().addAll(att_images, att_files);
    }

    /** @param node Node to use when taking a screenshot */
    public void setSnapshotNode(final Node node)
    {
        att_images.setSnapshotNode(node);
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
}
