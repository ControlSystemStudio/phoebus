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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
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
    private final MenuButton x_device_selector = new MenuButton("X Axis");;
    private final List<MenuButton> y_device_selectors = new ArrayList<>();
    private final RTValuePlot plot = new RTValuePlot(true);

    private final ScanClient scan_client = new ScanClient(Preferences.host, Preferences.port);
    private volatile String x_device = "";
    private List<String> y_devices = new CopyOnWriteArrayList<>(List.of(""));
    private volatile List<ScanPlotDataProvider> plot_data = Collections.emptyList();

    /** Information about all scans */
    private ScanInfoModel scan_info_model;

    /** Reader for data of selected scan; used to update plot_data */
    private ScanDataReader reader;

    /** Last data returned by reader.
     *  Kept so that updated *_device_* values
     *  can right away show data.
     */
    private volatile ScanData last_scan_data = null;

    /** Devices of the selected scan; used to update device selectors */
    private final AtomicReference<List<String>> scan_devices = new AtomicReference<>(Collections.emptyList());

    public DataPlot()
    {
        scan_selector = new MenuButton("Scan");

        toolbar.getChildren().setAll(ToolbarHelper.createSpring(), scan_selector, x_device_selector);
        y_device_selectors.add(new MenuButton("Value"));
        updateToolbar();

        plot.showToolbar(false);
        plot.showLegend(false);
        plot.getXAxis().setAutoscale(true);
        plot.getYAxes().get(0).setAutoscale(true);
        plot.getYAxes().get(0).useAxisName(false);

        VBox.setVgrow(plot, Priority.ALWAYS);
        getChildren().setAll(toolbar, plot);

        // Start getting data
        reader = new ScanDataReader(scan_client, this::updatePlotData);

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

    private void updateScans(final List<ScanInfo> infos)
    {
        final ToggleGroup group = new ToggleGroup();
        final List<MenuItem> names = new ArrayList<>(infos.size());
        for (ScanInfo info : infos)
        {
            final String label = MessageFormat.format(Messages.scan_name_id_fmt, info.getName(), info.getId());
            final RadioMenuItem item = new RadioMenuItem(label);
            item.setToggleGroup(group);
            if (reader.getScanId() == info.getId())
                item.setSelected(true);
            item.setOnAction(event -> selectScan(info.getId(), label));
            names.add(item);
        }
        Platform.runLater(() -> scan_selector.getItems().setAll(names));
    }

    /** Update drop-downs for devices based on current 'devices', 'x_device', 'y_devices' */
    private void updateToolbar()
    {
        // Select the current scan in list of scan menu items
        for (MenuItem scan_sel : scan_selector.getItems())
            if (scan_sel.getText().endsWith("#" + reader.getScanId()))
                    ((RadioMenuItem)scan_sel).setSelected(true);

        final List<String> devices = scan_devices.get();

        // Update devices for X Axis
        updateDeviceMenus(x_device_selector, devices, x_device, this::selectXDevice);

        int i = y_device_selectors.size();
        while (i > y_devices.size())
            y_device_selectors.remove(--i);
        while (i < y_devices.size())
            y_device_selectors.add(new MenuButton("Value " + (1 + i++)));

        // Update devices for Values
        for (i=0; i<y_device_selectors.size(); ++i)
        {
            final int index = i;
            updateDeviceMenus(y_device_selectors.get(i), devices, y_devices.get(i),
                              dev -> selectYDevice(index, dev));
        }

        final ObservableList<Node> items = toolbar.getChildren();
        items.remove(3,  items.size());
        items.addAll(y_device_selectors);
    }

    /** @param menu Menu to update
     *  @param devices Devices for which to create menu entries
     *  @param device Currently selected device
     *  @param action Action to perform when menu item is selected
     */
    private void updateDeviceMenus(final MenuButton menu, final List<String> devices, final String device, final Consumer<String> action)
    {
        final ToggleGroup group = new ToggleGroup();
        final List<MenuItem> items = new ArrayList<>(devices.size());
        for (String dev : devices)
        {
            final RadioMenuItem item = new RadioMenuItem(dev);
            item.setToggleGroup(group);
            if (dev.equals(device))
                item.setSelected(true);
            item.setOnAction(event -> action.accept(dev));
            items.add(item);
        }
        menu.getItems().setAll(items);
    }

    public void selectScan(final long scan_id)
    {
        selectScan(scan_id, MessageFormat.format(Messages.scan_name_id_fmt, "Scan", scan_id));
    }

    public void selectScan(final long scan_id, final String title)
    {
        // Data from last scan no longer valid
        last_scan_data = null;
        plot.setTitle(title);
        reader.setScanId(scan_id);
    }

    public void selectXDevice(final String device)
    {
        x_device = device;
        plot.getXAxis().setName(x_device);
        updatePlotDataProviders();
        updateToolbar();
        reader.trigger();
    }

    public void addYDevice(final String device)
    {
        y_devices.add(device);
        updatePlotDataProviders();
        updateToolbar();
        reader.trigger();
    }

    public void selectYDevice(final int index, final String device)
    {
        y_devices.set(index, device);
        updatePlotDataProviders();
        updateToolbar();
        reader.trigger();
    }

    private void updatePlotDataProviders()
    {
        // Remove all traces
        for (Trace<Double> trace : plot.getTraces())
            plot.removeTrace(trace);

        final ScanData data = last_scan_data;

        // Create plot data provider for each trace
        final List<ScanPlotDataProvider> new_plot_data = new ArrayList<>();
        final RGBFactory colors = new RGBFactory();
        for (String device : y_devices)
            if (! (x_device.isEmpty() || device.isEmpty()))
            {
                final ScanPlotDataProvider pd = new ScanPlotDataProvider(x_device, device);
                if (data != null)
                    pd.update(data);
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

    private void updatePlotData(final ScanData data)
    {
        // New device list?
        final List<String> new_devices = data.getDevices();
        final List<String> old_devices = scan_devices.getAndSet(new_devices);
        if (! new_devices.equals(old_devices))
            Platform.runLater(this::updateToolbar);

        final List<ScanPlotDataProvider> copy = plot_data;
        for (ScanPlotDataProvider pd : copy)
            pd.update(data);
        plot.requestUpdate();
        last_scan_data  = data;
    }
}
