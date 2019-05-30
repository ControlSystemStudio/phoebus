/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link DataTable}
 *  @author Kay Kasemir
 */
public class DataTableDemo extends ApplicationWrapper
{
    private static final int SCAN_ID = 66;

    @Override
    public void start(final Stage stage) throws Exception
    {
        final ScanClient client = new ScanClient(Preferences.host, Preferences.port);
        final Scene scene = new Scene(new DataTable(client, SCAN_ID), 600, 500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws Exception
    {
        launch(DataTableDemo.class, args);
    }
}
