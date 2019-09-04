/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeView;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link AlarmTreeView}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeUIDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final AlarmClient client = new AlarmClient("localhost:9092", "Accelerator");

        final AlarmTreeView tree_view = new AlarmTreeView(client);
        final Scene scene = new Scene(tree_view, 600, 800);
        stage.setTitle("Alarm Tree Demo");
        stage.setScene(scene);
        stage.show();

        client.start();

        stage.setOnCloseRequest(event -> client.shutdown());
    }

    public static void main(final String[] args)
    {
        launch(AlarmTreeUIDemo.class, args);
    }
}
