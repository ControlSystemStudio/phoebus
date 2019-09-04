/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.applications.alarm.ui.annunciator.AnnunciatorTable;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *  Demo of the AnnunciatorTable
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorTableDemo extends ApplicationWrapper
{
    final TalkClient client = new TalkClient(AlarmSystem.server, AlarmSystem.config_name);
    final AnnunciatorTable table = new AnnunciatorTable(client);

    @Override
    public void start(final Stage stage) throws Exception
    {

        final Scene scene = new Scene(table, 1200, 300);
        stage.setScene(scene);
        stage.setTitle("Alarm Annunciator Table Demo");
        stage.show();
        client.start();
        
        stage.setOnCloseRequest(event -> table.shutdown());
    }

    public static void main(final String[] args)
    {
        launch(AnnunciatorTableDemo.class, args);
    }
}
