package org.phoebus.applications.email.actions;

import static org.phoebus.applications.email.EmailApp.logger;

import java.util.Optional;
import java.util.logging.Level;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.applications.email.ui.SimpleCreateController;
import org.phoebus.email.EmailPreferences;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A context menu entry for starting the email application
 *
 * @author Kunal Shroff
 *
 */
public class ContextCreateEmail implements ContextMenuEntry {

    private static final Class<?> supportedType = EmailEntry.class;

    @Override
    public String getName() {
        return EmailApp.DISPLAY_NAME;
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedType;
    }

    @Override
    public boolean isEnabled() {
        return EmailPreferences.isEmailSupported();
    }

    @Override
    public void call(Selection selection) {
        call(DockPane.getActiveDockPane(), selection);
    }

    @Override
    public void call(Node parent, Selection selection) {
        Optional<EmailEntry> adaptedSelection = AdapterService.adapt(selection, EmailEntry.class);
        if (EmailPreferences.isEmailSupported() && adaptedSelection.isPresent()) {
            EmailEntry emailEntry = adaptedSelection.get();
            try {
                final FXMLLoader loader = new FXMLLoader();
                loader.setLocation(EmailApp.class.getResource("ui/SimpleCreate.fxml"));
                Parent root = loader.load();
                final SimpleCreateController controller = loader.getController();

                if (emailEntry.getTitle() != null)
                    controller.setTitle(emailEntry.getTitle());
                if (emailEntry.getBody() != null)
                    controller.setBody(emailEntry.getBody());
                if (!emailEntry.getImages().isEmpty())
                    controller.setImages(emailEntry.getImages());
                if (!emailEntry.getFiles().isEmpty())
                    controller.setAttachmets(emailEntry.getFiles());

                final Stage stage = new Stage();
                stage.setTitle("Send Email");
                final Scene scene = new Scene(root, 600, 800);
                stage.setScene(scene);

                if (parent != null) {
                    controller.setSnapshotNode(parent.getScene().getRoot());
                    stage.setX(parent.getScene().getWindow().getX() + 100);
                    stage.setY(parent.getScene().getWindow().getY() + 50);
                }
                stage.show();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Email dialog failed", ex);
            }
        }

    }

}
