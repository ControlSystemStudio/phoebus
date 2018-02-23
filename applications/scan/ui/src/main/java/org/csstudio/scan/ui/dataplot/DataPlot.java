package org.csstudio.scan.ui.dataplot;

import java.util.ArrayList;
import java.util.List;
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
    private long scan_id;
    private String x_device;
    private List<String> y_devices = new ArrayList<>();
    private ScanDataReader reader;
    private RTValuePlot plot;
    private final List<ScanPlotDataProvider> plot_data = new ArrayList<>();


    public DataPlot()
    {
        // TODO Remove demo values
        scan_id = 70;
        x_device = "xpos";
        y_devices.add("ypos");

        plot = new RTValuePlot(true);
        plot.showToolbar(false);
        plot.showLegend(false);
        plot.getYAxes().get(0).useAxisName(false);

        GridPane.setHgrow(plot, Priority.ALWAYS);
        GridPane.setVgrow(plot, Priority.ALWAYS);
        add(plot, 0, 0);

        start();
    }

    private void start()
    {
        // Create plot data providers for each trace
        final RGBFactory colors = new RGBFactory();
        for (String device : y_devices)
            if (x_device != null  &&  device != null)
            {
                final ScanPlotDataProvider pdp = new ScanPlotDataProvider(x_device, device);
                plot_data.add(pdp);
                plot.addTrace(device, null, pdp, colors.next(), TraceType.SINGLE_LINE_DIRECT, 3, PointType.CIRCLES, 5, 0);
            }

        // Start getting data
        reader = new ScanDataReader(scan_client, scan_id, this::updatePlot);
    }

    public void stop()
    {
        // Stop reader
        reader.shutdown();
        reader = null;

        // Remove all traces
        for (Trace<Double> trace : plot.getTraces())
            plot.removeTrace(trace);
        plot_data.clear();
    }

    final ReentrantLock lock = new ReentrantLock();

    private void updatePlot(final ScanData data)
    {
        plot.getXAxis().setName(x_device);
        for (ScanPlotDataProvider pd : plot_data)
            pd.update(data);
        plot.requestUpdate();
    }
}
