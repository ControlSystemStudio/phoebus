/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.dataplot;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;

/** Application instance for Scan Data Plot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataPlotInstance implements AppInstance
{
    private static final String X_DEVICE = "x_dev", Y_DEVICE = "y_dev";
    private final ScanDataPlotApplication app;
    private final DataPlot data_plot;
    private final DockItemWithInput tab;

    ScanDataPlotInstance(final ScanDataPlotApplication app, final long scan_id)
    {
        this.app = app;

        data_plot = create(scan_id);
        final URI input = ScanURI.createURI(scan_id);
        tab = new DockItemWithInput(this, data_plot, input, null, null);
        Platform.runLater(() -> tab.setLabel("Scan Plot #" + scan_id));
        tab.addCloseCheck(() ->
        {
            data_plot.stop();
            return CompletableFuture.completedFuture(true);
        });
        DockPane.getActiveDockPane().addTab(tab);
    }

    private DataPlot create(final long scan_id)
    {
        final DataPlot plot = new DataPlot(this::updateScanId);
        plot.selectScan(scan_id);
        return plot;
    }

    /** @param scan_id New scan selected by user in DataPlot */
    private void updateScanId(final long scan_id)
    {
        // Skip the initial call from
        // Constructor -> create -> selectScan
        if (tab == null)
            return;
        final URI input = ScanURI.createURI(scan_id);
        tab.setInput(input);
        Platform.runLater(() -> tab.setLabel("Scan Plot #" + scan_id));
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    @Override
    public void save(final Memento memento)
    {
        memento.setString(X_DEVICE, data_plot.getXDevice());
        final List<String> devs = data_plot.getYDevices();
        memento.setString(Y_DEVICE, devs.get(0));
        for (int i=1; i<devs.size(); ++i)
            memento.setString(Y_DEVICE+i, devs.get(i));
    }

    @Override
    public void restore(final Memento memento)
    {
        memento.getString(X_DEVICE).ifPresent(data_plot::selectXDevice);
        memento.getString(Y_DEVICE).ifPresent(dev -> data_plot.selectYDevice(0, dev));
        for (int i=1; true; ++i)
        {
            final Optional<String> dev = memento.getString(Y_DEVICE+i);
            if (dev.isPresent())
                data_plot.addYDevice(dev.get());
            else
                break;
        }
    }
}
