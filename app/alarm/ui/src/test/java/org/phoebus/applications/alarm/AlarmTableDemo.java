/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.ui.table.AlarmTableMediator;
import org.phoebus.applications.alarm.ui.table.AlarmTableUI;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of the {@link AlarmTableUI}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final AlarmClient client = new AlarmClient(AlarmSystem.server, AlarmSystem.config_name);
        final AlarmTableUI table = new AlarmTableUI(client);
        final Scene scene = new Scene(table, 1200, 300);
        stage.setScene(scene);
        stage.setTitle("Alarm Table Demo");
        stage.show();

        final AlarmTableMediator mediator = new AlarmTableMediator(client, table);
        client.addListener(mediator);
        client.start();
    }

    public static void main(final String[] args)
    {
        launch(AlarmTableDemo.class, args);
    }
}
