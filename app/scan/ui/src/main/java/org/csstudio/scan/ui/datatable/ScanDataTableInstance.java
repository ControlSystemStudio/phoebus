/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;

/** Application instance for Scan Data Table
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataTableInstance implements AppInstance
{
    private final ScanDataTableApplication app;
    private final DockItemWithInput tab;

    ScanDataTableInstance(final ScanDataTableApplication app, final long scan_id)
    {
        this.app = app;

        final DataTable data_table = create(scan_id);
        final URI input = ScanURI.createURI(scan_id);
        tab = new DockItemWithInput(this, data_table, input, null, null);
        Platform.runLater(() -> tab.setLabel("Scan Data #" + scan_id));
        tab.addCloseCheck(() ->
        {
            data_table.dispose();
            return CompletableFuture.completedFuture(true);
        });
        DockPane.getActiveDockPane().addTab(tab);
    }

    private DataTable create(final long scan_id)
    {
        final ScanClient client = new ScanClient(Preferences.host, Preferences.port);
        return new DataTable(client, scan_id);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }
}
