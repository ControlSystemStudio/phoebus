package org.phoebus.applications.saveandrestore.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.data.NodeAddedListener;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.framework.nls.NLS;

import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public abstract class BaseSaveAndRestoreController implements Initializable, NodeChangedListener, NodeAddedListener, ISaveAndRestoreController {
    protected Stage tagSearchWindow;

    @FXML
    protected void openTagSearchWindow() {
        try {
            if (tagSearchWindow == null) {
                final ResourceBundle bundle = NLS.getMessages(SaveAndRestoreApplication.class);
                SpringFxmlLoader loader = new SpringFxmlLoader();

                tagSearchWindow = new Stage();
                tagSearchWindow.setTitle(Messages.tagSearchWindowLabel);
                tagSearchWindow.initModality(Modality.WINDOW_MODAL);
                tagSearchWindow.setScene(new Scene((Parent) loader.load("ui/TagSearchWindow.fxml", bundle)));
                ((TagSearchController) loader.getLoader().getController()).setCallerController(this);
                tagSearchWindow.setOnCloseRequest(action -> tagSearchWindow = null);
                tagSearchWindow.show();
            } else {
                tagSearchWindow.requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeTagSearchWindow() {
        if (tagSearchWindow != null) {
            tagSearchWindow.close();
        }
    };
}
