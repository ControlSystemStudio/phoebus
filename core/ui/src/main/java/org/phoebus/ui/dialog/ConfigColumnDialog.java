package org.phoebus.ui.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.Messages;
import org.phoebus.ui.javafx.ColumnConfigHandler;

/**
 * Customized Dialog to configure the visibility of table columns.
 */

public class ConfigColumnDialog extends Dialog<Void> {

    public ConfigColumnDialog(ColumnConfigHandler colConfHandler){
        super();
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(NLS.getMessages(Messages.class));
        fxmlLoader.setLocation(getClass().getResource("ConfigColumnDialog.fxml"));

        fxmlLoader.setControllerFactory(clazz -> {
            try {
                if(clazz.isAssignableFrom(ConfigColumnDialogController.class)) {
                    return clazz.getConstructor(ColumnConfigHandler.class).newInstance(colConfHandler);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        final BorderPane root;

        try {
            root = fxmlLoader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setTitle(Messages.ColumnConfiguration);
        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }
}
