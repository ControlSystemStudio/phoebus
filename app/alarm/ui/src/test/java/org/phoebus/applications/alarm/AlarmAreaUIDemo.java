/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.ui.area.AlarmAreaView;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link AlarmAreaView}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmAreaUIDemo extends ApplicationWrapper
{
    @Override
	public void start(final Stage stage) throws Exception
	{
		final AlarmClient client = new AlarmClient("localhost:9092", "Accelerator");

        final AlarmAreaView area_view = new AlarmAreaView(client);
        final Scene scene = new Scene(area_view, 600, 800);
        stage.setTitle("Alarm Area Demo");
        stage.setScene(scene);
        stage.show();

        client.start();

        stage.setOnCloseRequest(event -> client.shutdown());
	}

	public static void main(String[] args)
	{
		launch(AlarmAreaUIDemo.class, args);
	}
}
