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
import java.util.stream.Collectors;

public class DisableAction extends Menu {

    private AlarmClient alarmClient;

    public DisableAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items) {
        this.alarmClient = model;
        setText(Messages.disableMenu);
        setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disabled.png"));
        MenuItem disable = new DisableComponentAction(node, model, items);
        MenuItem disableUntil = new MenuItem(Messages.disabledUntil);
        disableUntil.setDisable(true);
        Set<AlarmClientLeaf> totalLeafItems = new HashSet<>();
        Set<AlarmClientLeaf> leafItemsWithEnableDate = new HashSet<>();
        setOnShowing(e -> {

            new Thread(() -> {
                if (checkEnableDates(items, totalLeafItems, leafItemsWithEnableDate)) {
                    Platform.runLater(() -> disableUntil.setDisable(false));
                }

            }).start();


        });
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


            if (!leafItemsWithEnableDate.isEmpty()){
                LocalDateTime defaultDate = leafItemsWithEnableDate.iterator().next().getEnabledDate();
                dialogController.setDefaultDate(defaultDate);
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
            if (localDateTime.isPresent()) {
                updateEnablement(localDateTime.get(), totalLeafItems);
                System.out.println(localDateTime.get());
            }

        });

        getItems().addAll(disable, disableUntil);
    }

    /**
     * Divides items the user clicked on in leaf items and non leaf items
     * Returns true when all leaf items of the same structure either have no enable dates or all the same
     * Returns false if the enable dates differ
     *
     * @param items           Root item
     * @param totalLeafItems          {@link Set} that will hold all leaf nodes
     * @param leafItemsWithEnableDate {@link Set} that will hold all leaf nodes with non-null enable date
     *
     */

    public static boolean checkEnableDates(final List<AlarmTreeItem<?>> items, Set<AlarmClientLeaf> totalLeafItems, Set<AlarmClientLeaf> leafItemsWithEnableDate) {
        Set<AlarmTreeItem<?>> nonLeafItems =
                items.stream().filter(i -> !(i instanceof AlarmClientLeaf)).collect(Collectors.toSet());
        Set<AlarmTreeItem<?>> leafItems =
                items.stream().filter(i -> (i instanceof AlarmClientLeaf)).collect(Collectors.toSet());
        nonLeafItems.forEach(i -> findAffectedPVs(i, totalLeafItems, leafItemsWithEnableDate));
        leafItems.forEach(i -> findAffectedPVs(i, totalLeafItems, leafItemsWithEnableDate));
        if (leafItemsWithEnableDate.isEmpty()) {
            return true;
        } else if (totalLeafItems.size() != leafItemsWithEnableDate.size()) {
            return false;
        } else {
            LocalDateTime firstDate = leafItemsWithEnableDate.iterator().next().getEnabledDate();
            for (AlarmClientLeaf alarmClientLeaf : totalLeafItems) {
                LocalDateTime currDate = alarmClientLeaf.getEnabledDate();
                if (!firstDate.equals(currDate)) {
                    return false;
                }
            }
            return true;
        }
    }


    /**
     * Recursively counts alarm tree items in a subtree to find total number and
     * number of disabled with enable date.
     *
     * @param item           Root item
     * @param total          {@link Set} that will hold all leaf nodes
     * @param withEnableDate {@link Set} that will hold all leaf nodes with non-null enable date
     *
     */
    public static void findAffectedPVs(final AlarmTreeItem<?> item, final Set<AlarmClientLeaf> total, final Set<AlarmClientLeaf> withEnableDate) {
        if (item instanceof AlarmClientLeaf) {
            final AlarmClientLeaf pv = (AlarmClientLeaf) item;
            total.add(pv);
            if (pv.getEnabledDate() != null) {
                withEnableDate.add(pv);
            }
        } else {
            for (AlarmTreeItem<?> sub : item.getChildren()) {
                findAffectedPVs(sub, total, withEnableDate);
            }
        }
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
