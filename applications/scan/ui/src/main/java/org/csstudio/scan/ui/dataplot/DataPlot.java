package org.csstudio.scan.ui.dataplot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.ui.ScanDataReader;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class DataPlot extends GridPane
{
    private final ScanClient scan_client = new ScanClient(Preferences.host, Preferences.port);
    private volatile String x_device = null;
    private List<String> y_devices = new CopyOnWriteArrayList<>();
    private ScanDataReader reader;
    private RTValuePlot plot;
    private volatile List<ScanPlotDataProvider> plot_data = Collections.emptyList();


    public DataPlot()
    {
        plot = new RTValuePlot(true);
        plot.showToolbar(false);
        plot.showLegend(false);
        plot.getYAxes().get(0).useAxisName(false);

        GridPane.setHgrow(plot, Priority.ALWAYS);
        GridPane.setVgrow(plot, Priority.ALWAYS);
        add(plot, 0, 0);

        // Start getting data
        reader = new ScanDataReader(scan_client, this::updatePlot);
    }

    public void selectScan(final long scan_id)
    {
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
                plot.addTrace(device, null, pd, colors.next(), TraceType.SINGLE_LINE_DIRECT, 3, PointType.CIRCLES, 5, 0);
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

    final ReentrantLock lock = new ReentrantLock();

    private void updatePlot(final ScanData data)
    {
        final List<ScanPlotDataProvider> copy = plot_data;
        for (ScanPlotDataProvider pd : copy)
            pd.update(data);
        plot.requestUpdate();
    }
}
