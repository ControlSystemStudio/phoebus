package org.phoebus.applications.email.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SplitPane;
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
import javafx.stage.Stage;
import org.phoebus.applications.email.EmailApp;
import org.phoebus.email.EmailPreferences;
import org.phoebus.email.EmailService;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImagesTab;
import org.phoebus.util.time.TimestampFormats;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import static org.phoebus.applications.email.EmailApp.logger;

/**
 * Controller for dialog to create and send emails
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("nls")
public class EmailDialogController {

    private static final String LAST_TO = "last_to";
    private static final String LAST_FROM = "last_from";

    final Preferences prefs = PhoebusPreferenceService.userNodeForClass(EmailApp.class);

    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_HTML = "text/html";

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
    private final FilesTab att_files = new FilesTab();

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
            // Set To: header field of the header.
            String txt = txtTo.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtTo::requestFocus);
                return;
            }
            String to = txt;

            // Set From: header field of the header.
            txt = txtFrom.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtFrom::requestFocus);
                return;
            }
            String from = txt;

            // Set Subject: header field
            txt = txtSubject.getText().trim();
            if (txt.isEmpty())
            {
                Platform.runLater(txtSubject::requestFocus);
                return;
            }
            String subject = txt;

            Multipart body = new MimeMultipart();

            // Create the message part
            // This will depend on the body type

            BodyPart messageBodyPart;
            switch (choiceBox.getValue()) {
            case TEXT_HTML:
                messageBodyPart = new MimeBodyPart();
                String htmlText = htmlEditor.getHtmlText();
                messageBodyPart.setContent(htmlText, "text/html");
                body.addBodyPart(messageBodyPart);
                break;
            default:
                // Text
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(textArea.getText());
                body.addBodyPart(messageBodyPart);
                // Attachments
                // TODO Fix access to javax.annotations that clashes with JDK9 module, see #52
                final String date = TimestampFormats.SECONDS_FORMAT.format(Instant.now());
                int i = 0;
                for (Image image : att_images.getImages()) {
                    messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setDataHandler(new DataHandler(new ImageDataSource(image)));
                    messageBodyPart.setFileName("Image" + (++i) + "_" + date + ".png");
                    body.addBodyPart(messageBodyPart);
                }

                for (File file : att_files.getFiles())
                {
                    messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(file)));
                    messageBodyPart.setFileName(file.getName());
                    body.addBodyPart(messageBodyPart);
                }
                break;
            }

            EmailService.send(to, from, subject, body);
            logger.info("Sent message successfully....");

            prefs.put(LAST_TO, txtTo.getText());
            prefs.put(LAST_FROM, txtFrom.getText());

            Stage stage = (Stage) btnSend.getScene().getWindow();
            stage.close();

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void initialize() {

        txtTo.setText(prefs.get(LAST_TO, ""));
        if(EmailPreferences.from == null || EmailPreferences.from.isBlank())
            txtFrom.setText(prefs.get(LAST_FROM, ""));
        else
            txtFrom.setText(EmailPreferences.from);

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

        attachmentTabs.getTabs().addAll(att_images, att_files);

        // Set initial focus
        // Don't check subject/title:
        // It's OK to leave that empty,
        // and it's usually set by the calling code via setSubject,
        // i.e. it's empty right now but will soon be set.
        // User may change, but doesn't have to.
        // Body is most likely to require changes.
        final Node focus;
        if (txtTo.getText().isEmpty())
            focus = txtTo;
        else if (txtFrom.getText().isEmpty())
            focus = txtFrom;
        else
            focus = textArea;
        Platform.runLater(() -> focus.requestFocus());
    }

    /** @param node Node to use when taking a screenshot */
    public void setSnapshotNode(final Node node)
    {
        att_images.setSnapshotNode(node);
    }

    /** @param text Subject (subject) */
    public void setSubject(final String text)
    {
        txtSubject.setText(text);
    }

    /** @param text Body (content) */
    public void setBody(final String text)
    {
        textArea.setText(text);
        htmlEditor.setHtmlText(text.replace("\n", "<br>"));
    }

    /** @param images Initial list of images to attach */
    public void setImages(final List<Image> images)
    {
        att_images.setImages(images);
    }

    /** @param files Initial list of files to attach */
    public void setAttachmets(final List<File> files)
    {
        att_files.setFiles(files);
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
