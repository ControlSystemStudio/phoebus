/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.util.Collections;
import java.util.List;

import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** {@link ScansTable} demo
 *  @author Kay Kasemir
 */
public class ScansTableDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        ScanInfoModel model = ScanInfoModel.getInstance();
        final ScansTable scans = new ScansTable(model.getScanClient());

        final Scene scene = new Scene(scans, 600, 300);
        stage.setScene(scene);
        stage.show();

        final ScanInfoModelListener listener = new ScanInfoModelListener()
        {
            @Override
            public void scanServerUpdate(final ScanServerInfo server_info)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void scanUpdate(final List<ScanInfo> infos)
            {
                Platform.runLater(() -> scans.update(infos));
            }

            @Override
            public void connectionError()
            {
                Platform.runLater(() -> scans.update(Collections.emptyList()));
            }
        };
        model.addListener(listener);

        stage.setOnHiding(event ->
        {
            model.removeListener(listener);
            model.release();
        });
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
