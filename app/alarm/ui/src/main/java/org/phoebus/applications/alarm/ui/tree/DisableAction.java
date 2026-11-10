package org.phoebus.applications.alarm.ui.tree;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.config.DisableDialogController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import org.phoebus.applications.alarm.ui.Messages;

import java.io.IOException;
import java.util.*;

public class DisableAction extends MenuItem {

    public DisableAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items) {
        super();

        setText(Messages.disableMenu);
        setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disabled.png"));
        setOnAction(e -> {
            final TreeNodeInfo treeNodeInfo = AlarmTreeHelper.getTreeNodeInfo(items);

            final Dialog<EnabledState> dlg = new Dialog<>();
            dlg.setTitle(Messages.disableAlarms);
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            DialogHelper.positionDialog(dlg, node, 0, 0);

            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setResources(NLS.getMessages(Messages.class));
            fxmlLoader.setLocation(DisableDialogController.class.getResource("DisableDialog.fxml"));
            fxmlLoader.setControllerFactory(_ -> new DisableDialogController(
                    treeNodeInfo.commonDisableState().orElseGet(() -> new EnabledState(true)),
                    dlg.getDialogPane().lookupButton(ButtonType.OK)
            ));

            final VBox root;
            try {
                root = fxmlLoader.load();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }

            dlg.getDialogPane().setContent(root);
            dlg.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return fxmlLoader.<DisableDialogController>getController().getEnabledState();
                }
                return null;
            });

            final Optional<EnabledState> newEnabledState = dlg.showAndWait();
            newEnabledState.ifPresent(enabledState -> ComponentActionHelper.updateEnabledState(node, model, enabledState, treeNodeInfo.leaves()));
        });
    }
}
