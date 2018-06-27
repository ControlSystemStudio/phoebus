/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

/** Action that enables items in the alarm tree configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class EnableComponentAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param model {@link AlarmClient}
     *  @param items Items to enable
     */
    public EnableComponentAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items)
    {
        if (doEnable())
        {
            setText("Enable Alarms");
            setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/enabled.png"));
        }
        else
        {
            setText("Disable Alarms");
            setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disabled.png"));
        }

        setOnAction(event ->
        {
            final List<AlarmClientLeaf> pvs = new ArrayList<>();
            for (AlarmTreeItem<?> item : items)
                findAffectedPVs(item, pvs);

            // If this affects exactly one PV, just do it.
            // Otherwise ask for confirmation
            if (pvs.size() != 1)
            {
                final Alert dialog = new Alert(AlertType.CONFIRMATION);
                dialog.setTitle(getText());
                if (pvs.size() == 0)
                    dialog.setHeaderText(
                        doEnable()
                        ? "All PVs in the selected section are already enabled"
                        : "All PVs in the selected section are already disabled");
                else
                    dialog.setHeaderText(MessageFormat.format(
                        doEnable()
                        ? "Enable all PVs in the selected section of the alarm hierarchy?\n" +
                          "This would enable {0} PVs"
                        : "Disable all PVs in the selected section of the alarm hierarchy?\n" +
                          "This would disable {0} PVs",
                        pvs.size()));
                DialogHelper.positionDialog(dialog, node, -100, -50);
                if (dialog.showAndWait().get() != ButtonType.OK)
                    return;
            }

            JobManager.schedule(getText(), monitor ->
            {
                for (AlarmClientLeaf pv : pvs)
                {
                    final AlarmClientLeaf copy = pv.createDetachedCopy();
                    if (copy.setEnabled(doEnable()))
                        model.sendItemConfigurationUpdate(pv.getPathName(), copy);
                }
            });
        });
    }

    // Implementation can actually disable or enable. Which one is it going to be?
    protected boolean doEnable()
    {
        return true;
    }

    /** @param item Node where to start recursing for PVs that would be affected
     *  @param pvs Array to update with PVs that would be affected
     */
    private void findAffectedPVs(final AlarmTreeItem<?> item, final List<AlarmClientLeaf> pvs)
    {
        if (item instanceof AlarmClientLeaf)
        {
            final AlarmClientLeaf pv = (AlarmClientLeaf) item;
            // If pv has different enablement, and wasn't already added
            // because selection contains its parent as well as the PV itself...
            if (pv.isEnabled() != doEnable()  &&  !pvs.contains(pv))
                pvs.add(pv);
        }
        else
            for (AlarmTreeItem<?> sub : item.getChildren())
                findAffectedPVs(sub, pvs);
    }
}
