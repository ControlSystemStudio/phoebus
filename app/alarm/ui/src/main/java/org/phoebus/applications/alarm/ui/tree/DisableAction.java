package org.phoebus.applications.alarm.ui.tree;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.config.DisableUntilDialogController;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

import org.phoebus.applications.alarm.ui.Messages;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DisableAction extends Menu {

    private final AlarmClient alarmClient;

    public DisableAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items) {
        this.alarmClient = model;
        setText(Messages.disableMenu);
        setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disabled.png"));
        MenuItem disable = new DisableComponentAction(node, model, items);
        disable.setText(Messages.indefinitely);
        MenuItem disableUntil = new MenuItem(Messages.withEnableDate);
        disableUntil.setDisable(true);
        AtomicReference<TreeNodeInfo> treeNodeInfo = new AtomicReference<>();
        setOnShowing(e -> JobManager.schedule("Get Tree Node Info", monitor -> {
            treeNodeInfo.set(AlarmTreeHelper.getTreeNodeInfo(items));
            if(treeNodeInfo.get().disabledWithEnableDate() == 0 || treeNodeInfo.get().commonEnableDate().isPresent()) {
                Platform.runLater(() -> disableUntil.setDisable(false));
            }
        }));
        disableUntil.setOnAction(e -> {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setResources(NLS.getMessages(Messages.class));
            fxmlLoader.setLocation(DisableUntilDialogController.class.getResource("DisableUntilDialog.fxml"));

            final GridPane root;
            try {
                root = fxmlLoader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            DisableUntilDialogController dialogController = fxmlLoader.getController();

            if (treeNodeInfo.get().commonEnableDate().isPresent()) {
                dialogController.setDefaultDate(treeNodeInfo.get().commonEnableDate().get());
            }

            final Dialog<LocalDateTime> dlg = new Dialog<>();
            dlg.setTitle("Disable until");
            dlg.getDialogPane().setContent(root);
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dlg.setResultConverter(button -> {
                if (button.equals(ButtonType.OK)) {
                    return dialogController.determineEnableDate();
                }
                return null;
            });
            Optional<LocalDateTime> localDateTime = dlg.showAndWait();
            localDateTime.ifPresent(dateTime -> updateEnablement(dateTime, treeNodeInfo.get().leaves()));
        });

        getItems().addAll(disable, disableUntil);
    }

    /**
     * Updates a component to disable a hierarchy of PVs with an enable date.
     *
     * @param enableDate The {@link LocalDateTime} to set on all leaf nodes specified in <code>items</code>   .
     */
    private void updateEnablement(LocalDateTime enableDate, Set<AlarmClientLeaf> totalLeafItems) {
        if (totalLeafItems.isEmpty()) {
            return;
        }
        if (totalLeafItems.size() > 1) {
            final Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle(Messages.disableAlarms);
            dialog.setHeaderText(MessageFormat.format(Messages.headerConfirmDisableWithEnableDate, enableDate, totalLeafItems.size()));

            if (dialog.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        JobManager.schedule(Messages.disableAlarms, monitor ->
        {
            for (AlarmClientLeaf pv : totalLeafItems) {
                final AlarmClientLeaf copy = pv.createDetachedCopy();
                if (copy.setEnabledDate(enableDate)) {
                    try {
                        alarmClient.sendItemConfigurationUpdate(pv.getPathName(), copy);
                    } catch (Exception e) {
                        ExceptionDetailsErrorDialog.openError(Messages.error,
                                Messages.disableAlarmFailed,
                                e);
                        throw e;
                    }
                }
            }
        });
    }
}
