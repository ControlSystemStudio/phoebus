/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.tree;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.TimestampFormats;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Set;


/**
 * Code externalized from {@link EnableAction}.
 */
public final class ComponentActionHelper {

    private ComponentActionHelper() {}

    /**
     * Updates a hierarchy of PVs to a new {@link EnabledState}.
     *
     * @param newEnabledState The {@link EnabledState} to set on all leaf nodes specified in <code>items</code>   .
     */
    public static void updateEnabledState(
            final Node node,
            final AlarmClient model,
            final EnabledState newEnabledState,
            final Set<AlarmClientLeaf> totalLeafItems
    ) {
        if (totalLeafItems.stream().allMatch(leaf -> leaf.getEnabled().equals(newEnabledState))) {
            final Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            DialogHelper.positionDialog(dialog, node, 0, 0);
            dialog.setHeaderText(newEnabledState.enabled ? Messages.headerAlreadyEnabled : Messages.headerAlreadyDisabled);
            dialog.show();
            return;
        }

        // check the enable datetime is in the future right before updating - we could guard against more in the
        // disable dialog, but the user could still for example select the next minute and then let two minutes pass
        // So if we didn't check here we'd need to constantly be checking in the dialog box, not really worth it
        if (newEnabledState.enabled_date != null && newEnabledState.enabled_date.isBefore(LocalDateTime.now())) {
            final Alert dialog = new Alert(Alert.AlertType.ERROR);
            DialogHelper.positionDialog(dialog, node, 0, 0);
            dialog.setHeaderText(MessageFormat.format(Messages.selectedDateInThePast, TimestampFormats.SECONDS_FORMAT.format(newEnabledState.enabled_date)));
            dialog.show();
            return;
        }

        // If more than one alarm affected, show a dialog box
        if (totalLeafItems.size() > 1) {
            final Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            DialogHelper.positionDialog(dialog, node, 0, 0);
            dialog.setTitle(newEnabledState.enabled ? Messages.enableAlarms : Messages.disableAlarms);
            if (newEnabledState.enabled_date != null) {
                dialog.setHeaderText(MessageFormat.format(
                        Messages.headerConfirmDisableWithEnableDate,
                        totalLeafItems.size(),
                        TimestampFormats.DATETIME_FORMAT.format(newEnabledState.enabled_date)
                ));
            } else if (!newEnabledState.enabled) {
                dialog.setHeaderText(MessageFormat.format(Messages.headerConfirmDisable, totalLeafItems.size()));
            } else {
                dialog.setHeaderText(MessageFormat.format(Messages.headerConfirmEnable, totalLeafItems.size()));
            }

            if (dialog.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        JobManager.schedule(newEnabledState.enabled ? Messages.enableAlarms : Messages.disableAlarms, monitor ->
        {
            for (final AlarmClientLeaf pv : totalLeafItems) {
                final AlarmClientLeaf copy = pv.createDetachedCopy();
                if (copy.setEnabled(newEnabledState)) {
                    try {
                        model.sendItemConfigurationUpdate(pv.getPathName(), copy);
                    } catch (final Exception ex) {
                        ExceptionDetailsErrorDialog.openError(Messages.error,
                                newEnabledState.enabled ? Messages.enableAlarmFailed : Messages.disableAlarmFailed,
                                ex);
                        throw ex;
                    }
                }
            }
        });
    }
}
