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
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.nls.NLS;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for editing {@link AlarmTreeItem}.
 *
 * <p>Layout is defined in {@code LeafConfigDialog.fxml}.
 * Runtime wiring (model data, bindings, event handlers) is handled by
 * {@link LeafConfigDialogController}.
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
        final FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            fxmlLoader.setResources(NLS.getMessages(Messages.class));
            if (item instanceof AlarmClientLeaf){
                fxmlLoader.setLocation(this.getClass().getResource("LeafConfigDialog.fxml"));
            }
            else{
                fxmlLoader.setLocation(this.getClass().getResource("ComponentConfigDialog.fxml"));
            }
            fxmlLoader.setControllerFactory(clazz -> {
                try {
                    if(clazz.isAssignableFrom(LeafConfigDialogController.class)) {
                        return clazz.getConstructor(AlarmClient.class, AlarmTreeItem.class).newInstance(model, item);
                    }
                    if(clazz.isAssignableFrom(ComponentConfigDialogController.class)) {
                        return clazz.getConstructor(AlarmClient.class, AlarmTreeItem.class).newInstance(model, item);
                    }
                    else if(clazz.isAssignableFrom(TitleDetailTableController.class)) {
                        return new TitleDetailTableController();
                    }
                    else if(clazz.isAssignableFrom(TitleDetailDelayTableController.class)) {
                        return new TitleDetailDelayTableController();
                    }
                    else if(clazz.isAssignableFrom(OptionsTablesController.class)) {
                        return clazz.getConstructor(AlarmTreeItem.class).newInstance(item);
                    }
                    else if(clazz.isAssignableFrom(TitleDetailToolbarController.class)){
                        return new TitleDetailToolbarController();
                    }
                } catch (Exception e) {
                    Logger.getLogger(ItemConfigDialog.class.getName()).log(Level.SEVERE, "Failed to construct controller for " + clazz.getName(), e);
                }
                return null;
            });

            // Load returns the root node (ScrollPane) declared in the FXML
            final ScrollPane root = fxmlLoader.load();

            // ── OK-button validation filter ───────────────────────────────────────
            final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
            ok.addEventFilter(ActionEvent.ACTION,
                    event -> ((ConfigDialogController)fxmlLoader.getController()).validateAndStore());

            getDialogPane().setContent(root);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to load " + fxmlLoader.getLocation(), ex);
        }

        setResizable(true);

        setResultConverter(button -> button == ButtonType.OK);
    }
}
