/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.dataplot;

import static org.csstudio.scan.ScanSystem.logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.ui.Messages;
import org.csstudio.scan.ui.ScanDataReader;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.application.Platform;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Plot of scan log
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataPlot extends VBox
{
    private final HBox toolbar = new HBox(5);
    private final MenuButton scan_selector;
    private final RTValuePlot plot = new RTValuePlot(true);

    private final ScanClient scan_client = new ScanClient(Preferences.host, Preferences.port);
    private volatile String x_device = null;
    private List<String> y_devices = new CopyOnWriteArrayList<>();
    private ScanDataReader reader;
    private volatile List<ScanPlotDataProvider> plot_data = Collections.emptyList();

    private ScanInfoModel scan_info_model;

    public DataPlot()
    {
        scan_selector = new MenuButton("Scan");
        toolbar.getChildren().setAll(ToolbarHelper.createSpring(), scan_selector);

        plot.showToolbar(false);
        plot.showLegend(false);
        plot.getYAxes().get(0).useAxisName(false);

        VBox.setVgrow(plot, Priority.ALWAYS);
        getChildren().setAll(toolbar, plot);

        // Start getting data
        reader = new ScanDataReader(scan_client, this::updatePlot);

        try
        {
            scan_info_model = ScanInfoModel.getInstance();
            scan_info_model.addListener(new ScanInfoModelListener()
            {
                @Override
                public void scanUpdate(List<ScanInfo> infos)
                {
                    updateScans(infos);
                }

                @Override
                public void connectionError()
                {
                    updateScans(null);
                }
            });
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot monitor scans", ex);
        }
    }

    public void selectScan(final long scan_id)
    {
        selectScan(scan_id, MessageFormat.format(Messages.scan_name_id_fmt, "Scan", scan_id));
    }

    public void selectScan(final long scan_id, final String title)
    {
        plot.setTitle(title);
        reader.setScanId(scan_id);
        reader.trigger();
    }


    public void selectXDevice(final String device)
    {
        x_device = device;
        plot.getXAxis().setName(x_device);
        updatePlotDataProviders();
        reader.trigger();
    }

    public void addYDevice(final String device)
    {
        y_devices.add(device);
        updatePlotDataProviders();
        reader.trigger();
    }

    private void updatePlotDataProviders()
    {
        // Remove all traces
        for (Trace<Double> trace : plot.getTraces())
            plot.removeTrace(trace);

        // Create plot data provider for each trace
        final List<ScanPlotDataProvider> new_plot_data = new ArrayList<>();
        final RGBFactory colors = new RGBFactory();
        for (String device : y_devices)
            if (x_device != null  &&  device != null)
            {
                final ScanPlotDataProvider pd = new ScanPlotDataProvider(x_device, device);
                plot.addTrace(device, null, pd, colors.next(), TraceType.SINGLE_LINE_DIRECT, 3, PointType.CIRCLES, 10, 0);
                new_plot_data.add(pd);
            }

        plot_data = new_plot_data;
    }

    public void stop()
    {
        // Stop reader
        reader.shutdown();
        reader = null;
    }

    private void updateScans(final List<ScanInfo> infos)
    {
        final List<MenuItem> names = new ArrayList<>(infos.size());
        for (ScanInfo info : infos)
        {
            final String label = MessageFormat.format(Messages.scan_name_id_fmt, info.getName(), info.getId());
            final MenuItem item = new MenuItem(label);
            item.setOnAction(event -> selectScan(info.getId(), label));
            names.add(item);
        }
        Platform.runLater(() -> scan_selector.getItems().setAll(names));
    }

    private void updatePlot(final ScanData data)
    {
        final List<ScanPlotDataProvider> copy = plot_data;
        for (ScanPlotDataProvider pd : copy)
            pd.update(data);
        plot.requestUpdate();
    }
}
