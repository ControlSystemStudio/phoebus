/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.tree;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Code externalized from {@link EnableComponentAction}.
 */
public class ComponentActionHelper {

    /**
     * Updates a component or PV node to enable or disable alarms
     *
     * @param node   The visual component relative to which a confirmation dialog is positioned.
     * @param model  {@link AlarmClient} dispatching producer messages.
     * @param items  {@link List} of items subject for update, e.g. selected by user.
     * @param enable If <code>true</code>, enable alarms on selected nodes, otherwise disable.
     */
    public static void updateEnablement(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items, boolean enable) {
        final List<AlarmClientLeaf> pvs = new ArrayList<>();
        for (AlarmTreeItem<?> item : items) {
            findAffectedPVs(item, pvs, enable);
        }

        // If this affects exactly one PV, just do it.
        // Otherwise ask for confirmation
        if (pvs.size() != 1) {
            final Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle(enable ? Messages.enableAlarms : Messages.disableAlarms);
            if (pvs.isEmpty()) {
                dialog.setHeaderText(
                        enable
                                ? Messages.headerAlreadyEnabled
                                : Messages.headerAlreadyDisabled);
            } else {
                dialog.setHeaderText(MessageFormat.format(
                        enable
                                ? Messages.headerConfirmEnable
                                : Messages.headerConfirmDisable,
                        pvs.size()));
            }
            DialogHelper.positionDialog(dialog, node, -100, -50);
            if (dialog.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        JobManager.schedule(enable ? Messages.enableAlarms : Messages.disableAlarms, monitor ->
        {
            for (AlarmClientLeaf pv : pvs) {
                final AlarmClientLeaf copy = pv.createDetachedCopy();
                if (copy.setEnabled(enable))
                    try {
                        model.sendItemConfigurationUpdate(pv.getPathName(), copy);
                    } catch (Exception e) {
                        ExceptionDetailsErrorDialog.openError(Messages.error,
                                copy.isEnabled() ? Messages.enableAlarmFailed : Messages.disableAlarmFailed,
                                e);
                        throw e;
                    }
            }
        });
    }

    /**
     * @param item Node where to start recursing for PVs that would be affected
     * @param pvs  Array to update with PVs that would be affected
     */
    public static void findAffectedPVs(final AlarmTreeItem<?> item, final List<AlarmClientLeaf> pvs, boolean enable) {
        if (item instanceof AlarmClientLeaf) {
            final AlarmClientLeaf pv = (AlarmClientLeaf) item;
            // If pv has different enablement, and wasn't already added
            // because selection contains its parent as well as the PV itself...
            if (pv.isEnabled() != enable && !pvs.contains(pv)) {
                pvs.add(pv);
            }
        } else {
            for (AlarmTreeItem<?> sub : item.getChildren()) {
                findAffectedPVs(sub, pvs, enable);
            }
        }
    }
}
