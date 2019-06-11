/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.table.AlarmInfoRow;
import org.phoebus.applications.alarm.ui.table.AlarmTableUI;
import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of the {@link AlarmTableUI}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableUIDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final AlarmTableUI table = new AlarmTableUI(null);
        final Scene scene = new Scene(table, 1200, 300);
        stage.setScene(scene);
        stage.setTitle("Alarm Table UI Demo");
        stage.show();


        final List<AlarmInfoRow> active = new ArrayList<>();
        final List<AlarmInfoRow> acknowledged = new ArrayList<>();
        for (int i=0; i<20; ++i)
        {
            final AlarmClientLeaf item = new AlarmClientLeaf(null, String.format("pv %02d", i));
            final SeverityLevel severity = SeverityLevel.values()[i % 9];
            item.setState(new ClientState(severity, "", "", Instant.now(), severity, ""));
            active.add(new AlarmInfoRow(item));
        }
        for (int i=0; i<20; ++i)
        {
            final AlarmClientLeaf item = new AlarmClientLeaf(null, String.format("pv %02d", i));
            final SeverityLevel severity = SeverityLevel.values()[i % 9];
            item.setState(new ClientState(severity, "", "", Instant.now(), severity, ""));
            acknowledged.add(new AlarmInfoRow(item));
        }

        table.update(active, acknowledged);

        final ScheduledExecutorService timer =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Timer"));

        final Runnable update = () ->
        {
            Platform.runLater(() ->
            {
                final ObjectProperty<SeverityLevel> severity = active.get(0).severity;
                if (severity.get() == SeverityLevel.MAJOR)
                    severity.set(SeverityLevel.OK);
                else
                    severity.set(SeverityLevel.MAJOR);
            });
        };
        timer.scheduleAtFixedRate(update, 100, 100, TimeUnit.MILLISECONDS);

        final Runnable add_remove = () ->
        {
            Platform.runLater(() ->
            {
                if (active.size() == 20)
                {
                    final AlarmClientLeaf item = new AlarmClientLeaf(null, "pv 20");
                    final SeverityLevel severity = SeverityLevel.INVALID;
                    item.setState(new ClientState(severity, "", "", Instant.now(), severity, ""));
                    active.add(new AlarmInfoRow(item));
                }
                else
                    active.remove(active.size()-1);
                table.update(active, acknowledged);
            });
        };
        timer.scheduleAtFixedRate(add_remove, 2000, 200, TimeUnit.MILLISECONDS);
    }

    public static void main(final String[] args)
    {
        launch(AlarmTableUIDemo.class, args);
    }
}
