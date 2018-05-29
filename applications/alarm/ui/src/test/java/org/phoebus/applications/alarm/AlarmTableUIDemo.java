/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.util.ArrayList;
import java.util.List;

import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.table.AlarmInfoRow;
import org.phoebus.applications.alarm.ui.table.AlarmTable;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of the {@link AlarmTable}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableUIDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final AlarmTable table = new AlarmTable();
        final Scene scene = new Scene(table, 1200, 300);
        stage.setScene(scene);
        stage.show();

        List<AlarmInfoRow> active = new ArrayList<>();
        List<AlarmInfoRow> acknowledged = new ArrayList<>();
        for (int i=0; i<20; ++i)
            active.add(new AlarmInfoRow("pv " + i, SeverityLevel.values()[i % 9]));
        for (int i=0; i<20; ++i)
            acknowledged.add(new AlarmInfoRow("ackpv " + i, SeverityLevel.values()[i % 9]));

        table.setAlarms(active, acknowledged);
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
