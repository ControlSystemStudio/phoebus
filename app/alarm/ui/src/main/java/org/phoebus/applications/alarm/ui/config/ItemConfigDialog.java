/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.applications.alarm.ui.config;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.nls.NLS;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for editing {@link AlarmTreeItem}.
 *
 * <p>Layout is defined in {@code ItemConfigDialog.fxml}.
 * Runtime wiring (model data, bindings, event handlers) is handled by
 * {@link ItemConfigDialogController}.
 *
 * <p>When pressing "OK", the dialog sends the updated configuration.
 */
@SuppressWarnings("nls")
public class ItemConfigDialog extends Dialog<Boolean> {

    public ItemConfigDialog(final AlarmClient model, final AlarmTreeItem<?> item) {
        super();
        // Allow multiple instances
        initModality(Modality.NONE);
        setTitle(Messages.configure + " " + item.getName());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setResources(NLS.getMessages(Messages.class));
            fxmlLoader.setLocation(this.getClass().getResource("ItemConfigDialog.fxml"));
            fxmlLoader.setControllerFactory(clazz -> {
                try {
                    return clazz.getConstructor(AlarmClient.class,
                                    AlarmTreeItem.class).newInstance(model, item);
                } catch (Exception e) {
                    Logger.getLogger(ItemConfigDialog.class.getName()).log(Level.SEVERE, "Failed to construct ItemConfigDialogController", e);
                }
                return null;
            });

            // Load returns the root node (ScrollPane) declared in the FXML
            final ScrollPane root = fxmlLoader.load();

            // ── OK-button validation filter ───────────────────────────────────────
            final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
            ok.addEventFilter(ActionEvent.ACTION,
                    event -> ((ItemConfigDialogController)fxmlLoader.getController()).validateAndStore());

            getDialogPane().setContent(root);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to load ItemConfigDialog.fxml", ex);
        }

        setResizable(true);

        setResultConverter(button -> button == ButtonType.OK);
    }
}
