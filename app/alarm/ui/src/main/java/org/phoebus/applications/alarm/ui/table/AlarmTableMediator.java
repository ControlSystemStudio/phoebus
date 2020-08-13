/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;

/** Mediates between alarm client, table model, UI
 *
 *  <p>Passes information from {@link AlarmClient} to
 *  {@link AlarmTableModel}, updating the {@link AlarmTableUI}
 *  as necessary.
 *
 *  @author Kay Kasemir
 */
public class AlarmTableMediator implements AlarmClientListener
{
    private final AlarmTableUI ui;
    private final AlarmTableModel model = new AlarmTableModel();
    private final UpdateThrottle throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::throttledUpdate);

    public AlarmTableMediator(final AlarmClient client, final AlarmTableUI ui)
    {
        this.ui = ui;
    }

    // AlarmClientModelListener
    @Override
    public void serverStateChanged(final boolean alive)
    {
        Platform.runLater(() -> ui.setServerState(alive));
    }

    // AlarmClientModelListener
    @Override
    public void serverModeChanged(final boolean maintenance_mode)
    {
        Platform.runLater(() -> ui.setMaintenanceMode(maintenance_mode));
    }

    // AlarmClientModelListener
    @Override
    public void serverDisableNotifyChanged(final boolean disable_notify)
    {
        Platform.runLater(() -> ui.setDisableNotify(disable_notify));
    }

    // AlarmClientListener
    @Override
    public void itemAdded(final AlarmTreeItem<?> item)
    {
        // Ignore
    }

    // AlarmClientListener
    @Override
    public void itemUpdated(final AlarmTreeItem<?> item)
    {
        if (model.handleUpdate(item))
            throttle.trigger();
    }

    // AlarmClientListener
    @Override
    public void itemRemoved(final AlarmTreeItem<?> item)
    {
        if (model.remove(item))
            throttle.trigger();
    }

    private void throttledUpdate()
    {
        final List<AlarmInfoRow> active = new ArrayList<>(),
                                 acknowledged = new ArrayList<>();
        for (AlarmClientLeaf pv : model.getActiveAlarms())
            active.add(new AlarmInfoRow(pv));
        for (AlarmClientLeaf pv : model.getAcknowledgedAlarms())
            acknowledged.add(new AlarmInfoRow(pv));
        Platform.runLater(() -> ui.update(active, acknowledged));
    }
}
