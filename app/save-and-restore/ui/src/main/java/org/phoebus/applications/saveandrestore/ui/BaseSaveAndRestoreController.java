package org.phoebus.applications.saveandrestore.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.NodeAddedListener;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.javafx.ImageCache;

import java.util.ResourceBundle;

public abstract class BaseSaveAndRestoreController implements Initializable, NodeChangedListener, NodeAddedListener, ISaveAndRestoreController {

    public static final Image folderIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/folder.png");
    public static final Image saveSetIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/saveset.png");
    public static final Image editSaveSetIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/edit_saveset.png");
    public static final Image deleteIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/delete.png");
    public static final Image renameIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/rename_col.png");
    public static final Image snapshotIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/snapshot.png");
    public static final Image snapshotGoldenIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/snapshot-golden.png");
    public static final Image compareSnapshotIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/compare.png");
    public static final Image snapshotTagsWithCommentIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tags.png");
    public static final Image csvImportIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/csv_import.png");
    public static final Image csvExportIcon = ImageCache.getImage(BaseSaveAndRestoreController.class, "/icons/csv_export.png");

    protected Stage searchWindow;

    @FXML
    protected void openSearchWindow() {
        try {
            if (searchWindow == null) {
                final ResourceBundle bundle = NLS.getMessages(SaveAndRestoreApplication.class);

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(BaseSaveAndRestoreController.class.getResource("SearchWindow.fxml"));
                loader.setResources(bundle);
                searchWindow = new Stage();
                searchWindow.getIcons().add(ImageCache.getImage(ImageCache.class, "/icons/logo.png"));
                searchWindow.setTitle(Messages.searchWindowLabel);
                searchWindow.initModality(Modality.WINDOW_MODAL);
                searchWindow.setScene(new Scene(loader.load()));
                ((SearchController) loader.getController()).setCallerController(this);
                searchWindow.setOnCloseRequest(action -> searchWindow = null);
                searchWindow.show();
            } else {
                searchWindow.requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeTagSearchWindow() {
        if (searchWindow != null) {
            searchWindow.close();
        }
    }

    ;
}
