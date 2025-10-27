package org.phoebus.applications.waterfallplotwidget;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.TickUnitSupplier;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.ColormapSelector;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import io.fair_acc.dataset.spi.DoubleGridDataSet;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class WaterfallPlotController {

    @FXML
    public StackPane xyChartStackPane;

    public final ColormapSelector.ColormapComboBox colorGradient = new ColormapSelector.ColormapComboBox();

    private XYChart xyChart;

    private final Pair<Integer, TimespanUnit> DEFAULT_TIMESPAN = new Pair(10, TimespanUnit.MINUTES);
    private Pair<Integer, TimespanUnit> timespan = DEFAULT_TIMESPAN;

    private DefaultNumericAxis zAxis = new DefaultNumericAxis("Z-Axis");
    private DefaultNumericAxis xAxis = new DefaultNumericAxis("X-Axis");
    private DefaultNumericAxis yAxis = new DefaultNumericAxis("Y-Axis");
    public ContourDataSetRenderer contourDataSetRenderer;
    private boolean zAxisMinHasBeenAutomaticallySet = false;
    private boolean zAxisMaxHasBeenAutomaticallySet = false;

    @FXML
    public void initialize() {
        xAxis.setTimeAxis(true);
        xAxis.setMin(Instant.now().getEpochSecond());
        xAxis.setMax(Instant.now().getEpochSecond() + 100);
        xAxis.setAutoRanging(true);
        xAxis.invertAxis(false);
        xAxis.setUnit("Time");

        yAxis.setAutoRanging(true);
        zAxis.setAutoRanging(false);

        newXYChart(xAxis, yAxis, zAxis);
    }

    private synchronized void newXYChart(Axis xAxis, Axis yAxis, Axis zAxis) {
        xyChart = new XYChart(xAxis, yAxis, zAxis);

        xyChart.getAxes().clear();
        xyChart.getAxes().addAll(xAxis, yAxis, zAxis);

        contourDataSetRenderer = new ContourDataSetRenderer();
        contourDataSetRenderer.setColorGradient(ColorGradient.RAINBOW);
        contourDataSetRenderer.setShowInLegend(true);
        contourDataSetRenderer.setUseGlobalColorIndex(true);
        contourDataSetRenderer.setPointReduction(false);
        contourDataSetRenderer.getAxes().setAll(xAxis, yAxis, zAxis);

        colorGradient.setValue(contourDataSetRenderer.getColorGradient());
        colorGradient.valueProperty().bindBidirectional(contourDataSetRenderer.colorGradientProperty());

        xyChart.getRenderers().clear();
        xyChart.getRenderers().add(contourDataSetRenderer);
        xyChart.setLegendVisible(false);

        xyChart.setTitle(plotTitle);
        // Workaround around the fact that the fonts get updated on
        // mouse clicks: add event handler to set the font back again:
        xyChart.getTitleLabel().fontProperty().addListener((property, oldValue, newValue) -> {
            if (titleFont.isPresent()) {
                Font font = titleFont.get();
                if (newValue != font) {
                    xyChart.getTitleLabel().setFont(font);
                }
            }
        });

        dataSet = createEmptyPVDataSet();
        xyChart.getDatasets().add(dataSet);

        xyChart.setMouseTransparent(true); // Removes "hover"-effect along the top edge of the widget.

        this.xyChartStackPane.getChildren().clear();
        this.xyChartStackPane.getChildren().add(xyChart);
    }

    private synchronized DoubleGridDataSet createEmptyPVDataSet() {
        DoubleGridDataSet dataSet = new DoubleGridDataSet("Data Set", 3);
        int N_SAMPLES = 100;
        final double[][] xyValues = new double[2][N_SAMPLES];
        final double[] zValues = new double[N_SAMPLES * N_SAMPLES];
        for (int n = 0; n < N_SAMPLES; n++) {
            xyValues[0][n] = n;
            xyValues[1][n] = n;
        }

        for (int n = 0; n < N_SAMPLES * N_SAMPLES; n++) {
            zValues[n] = Double.NaN;
        }
        dataSet.set(false, xyValues, zValues);

        return dataSet;
    }

    private double observedMin = Double.NaN;
    private double observedMax = Double.NaN;

    private synchronized double getAutomaticMin() {
        if (!Double.isNaN(observedMin) && !Double.isNaN(observedMax) && observedMin < observedMax) {
            return observedMin - (observedMax - observedMin) * 0.20;
        } else {
            return Double.NaN;
        }
    }

    private synchronized double getAutomaticMax() {
        if (!Double.isNaN(observedMin) && !Double.isNaN(observedMax) && observedMin < observedMax) {
            return observedMax + (observedMax - observedMin) * 0.20;
        } else {
            return Double.NaN;
        }
    }

    public AtomicBoolean timeOnXAxis = new AtomicBoolean(true);

    public static long timespanInSeconds(Pair<Integer, TimespanUnit> timespan) {
        long multiplier;
        if (timespan.getValue().equals(TimespanUnit.SECONDS)) {
            multiplier = 1;
        } else if (timespan.getValue().equals(TimespanUnit.MINUTES)) {
            multiplier = 60;
        } else if (timespan.getValue().equals(TimespanUnit.HOURS)) {
            multiplier = 60 * 60;
        } else if (timespan.getValue().equals(TimespanUnit.DAYS)) {
            multiplier = 60 * 60 * 24;
        } else if (timespan.getValue().equals(TimespanUnit.WEEKS)) {
            multiplier = 60 * 60 * 24 * 7;
        } else {
            throw new RuntimeException("Case for TimespanUnit missing: " + timespan.getValue());
        }
        return timespan.getKey() * multiplier;
    }

    private DoubleGridDataSet dataSet;

    private void addAxisFontListeners(Axis axis) {
        // Workaround around the fact that the fonts get updated on
        // mouse clicks: add event handler to set the font back again:
        axis.getAxisLabel().fontProperty().addListener((property, oldValue, newValue) -> {
            if (axisLabelFont.isPresent()) {
                Font font = axisLabelFont.get();
                if (newValue != font) {
                    axis.getAxisLabel().setFont(font);
                }
            }
        });
        // Workaround around the fact that the fonts get updated on
        // mouse clicks: add event handler to set the font back again:
        axis.getTickLabelStyle().fontProperty().addListener((property, oldValue, newValue) -> {

            if (tickLabelFont.isPresent()) {
                Font font = tickLabelFont.get();
                if (newValue != font) {
                    axis.getTickLabelStyle().setFont(font);
                }
            }

        });
    }

    public synchronized void setNewPVName(boolean isWaveform, boolean usePVNumberAsLabelOnAxis, List<String> pvNames) {
        observedMin = Double.NaN;
        observedMax = Double.NaN;
        DefaultNumericAxis dataSourceAxis;
        if (isWaveform) {
            dataSourceAxis = new DefaultNumericAxis();
            addAxisFontListeners(dataSourceAxis);
            dataSourceAxis.autoRangingProperty().setValue(true);
            var formatter = dataSourceAxis.getAxisLabelFormatter();
            TickUnitSupplier tickUnitSupplier = referenceTickUnit -> Math.ceil(referenceTickUnit);
            formatter.setTickUnitSupplier(tickUnitSupplier);
        } else {
            List<String> categories = new LinkedList<>();
            int i = 0;
            for (String pvName : pvNames) {
                categories.add("");
                if (usePVNumberAsLabelOnAxis) {
                    categories.add(Integer.toString(i++));
                } else {
                    categories.add(pvName);
                }
            }
            categories.add("");
            CategoryAxis categoryAxis = new CategoryAxis();
            addAxisFontListeners(categoryAxis);
            var formatter = categoryAxis.getAxisLabelFormatter();
            TickUnitSupplier tickUnitSupplier = referenceTickUnit -> 1.0;
            categoryAxis.setCategories(FXCollections.observableList(categories));
            formatter.setTickUnitSupplier(tickUnitSupplier);
            categoryAxis.autoRangingProperty().setValue(false);
            categoryAxis.setMaxMajorTickLabelCount(categories.size() * 2 - 1); // Remove the "unknown" category.
            categoryAxis.setMin(0.0);
            categoryAxis.setMax(categories.size() - 1);

            dataSourceAxis = categoryAxis;
        }

        DefaultNumericAxis timeAxis = new DefaultNumericAxis();
        addAxisFontListeners(timeAxis);
        timeAxis.setTimeAxis(true);
        timeAxis.setName("Time");
        timeAxis.setUnit("UTC");

        zAxis = new DefaultNumericAxis();
        addAxisFontListeners(zAxis);
        zAxis.setAutoRanging(false);
        if (timeOnXAxis.get()) {
            timeAxis.invertAxis(false);
            xAxis = timeAxis;
            yAxis = dataSourceAxis;
        } else {
            timeAxis.invertAxis(true);
            xAxis = dataSourceAxis;
            yAxis = timeAxis;
        }
        newXYChart(xAxis, yAxis, zAxis);
    }

    public synchronized void setMajorTickLength(double newTickLength) {
        xAxis.setTickLength(newTickLength);
        yAxis.setTickLength(newTickLength);
        zAxis.setTickLength(newTickLength);
    }

    public synchronized void setMajorTickWidth(double newTickWidth) {
        xAxis.getMajorTickStyle().setStrokeWidth(newTickWidth);
        yAxis.getMajorTickStyle().setStrokeWidth(newTickWidth);
        zAxis.getMajorTickStyle().setStrokeWidth(newTickWidth);
    }

    public synchronized void setMinorTickLength(double newTickLength) {
        xAxis.setMinorTickLength(newTickLength);
        yAxis.setMinorTickLength(newTickLength);
        zAxis.setMinorTickLength(newTickLength);
    }

    public synchronized void setMinorTickWidth(double newTickWidth) {
        xAxis.getMinorTickStyle().setStrokeWidth(newTickWidth);
        yAxis.getMinorTickStyle().setStrokeWidth(newTickWidth);
        zAxis.getMinorTickStyle().setStrokeWidth(newTickWidth);
    }

    public synchronized void setTimeOnXAxis(boolean newValue) {
        timeOnXAxis.set(newValue);
        xAxis.setTimeAxis(newValue);
        yAxis.setTimeAxis(!newValue);
    }

    Optional<Instant>[] previousT1 = new Optional[]{Optional.empty()}; // Wrapped in an array since variables in a Runnable must be final.
    Optional<Instant>[] previousT2 = new Optional[]{Optional.empty()}; // Wrapped in an array since variables in a Runnable must be final.
    long[] previousTimespanInSeconds = new long[]{timespanInSeconds(timespan)};

    public synchronized void redraw(WaterfallPlotRuntime.PVData pvData) {
        boolean timeOnXAxisValue = timeOnXAxis.get();
        long noOfSamplesToPlot;
        if (timeOnXAxisValue) {
            noOfSamplesToPlot = (long) xyChart.getPlotArea().getWidth();
        } else {
            noOfSamplesToPlot = (long) xyChart.getPlotArea().getHeight();
        }

        if (noOfSamplesToPlot <= 1) {
            return;
        } else {
            noOfSamplesToPlot -= 0;
        }

        long currentTimespanInSeconds = timespanInSeconds(timespan);
        Duration stepsize = Duration.ofSeconds(currentTimespanInSeconds).dividedBy(noOfSamplesToPlot);

        Instant t1;
        Instant t2;
        if (previousTimespanInSeconds[0] != currentTimespanInSeconds || previousT1[0].isEmpty() || previousT2[0].isEmpty()) {
            t2 = Instant.now();
            previousTimespanInSeconds[0] = currentTimespanInSeconds;
        } else {
            var stepsFromPreviousT2ToCurrentT2 = Duration.between(previousT2[0].get(), Instant.now()).dividedBy(stepsize);
            var deltaT = stepsize.multipliedBy(stepsFromPreviousT2ToCurrentT2);
            t2 = previousT2[0].get().plus(deltaT);
        }
        t1 = t2.minus(stepsize.multipliedBy(noOfSamplesToPlot));
        previousT1[0] = Optional.of(t1);
        previousT2[0] = Optional.of(t2);

        LinkedList<Double> timeValuesLinkedList = new LinkedList<>();
        LinkedList<ArrayList<Double>> zValuesLinkedList = new LinkedList<>();
        int waveformLength = 1;

        double minFromPV = Double.NaN;
        double maxFromPV = Double.NaN;
        if (pvData instanceof WaterfallPlotRuntime.WaveformPVData waveformPVData) {
            garbageCollectInstantToValue(waveformPVData.instantToValue(), t1);

            minFromPV = waveformPVData.minFromPV().get();
            maxFromPV = waveformPVData.maxFromPV().get();

            ConcurrentSkipListMap<Instant, ArrayList<Double>> instantToWaveform = waveformPVData.instantToValue();
            Instant startKey = instantToWaveform.ceilingKey(Instant.MIN);

            Optional<Instant> previousInstant = Optional.empty();
            for (Instant t = t1.plus(stepsize); t.compareTo(t2) <= 0; t = t.plus(stepsize)) {
                timeValuesLinkedList.add(((double) t.toEpochMilli()) / 1000.0);

                if (startKey == null || t.isBefore(startKey)) {
                    zValuesLinkedList.add(null); // null means absence of data for this point in time
                }
                else {
                    var instant = instantToWaveform.floorKey(t);

                    ArrayList<Double> waveform = instantToWaveform.get(instant);
                    waveformLength = Math.max(waveformLength, waveform.size());
                    zValuesLinkedList.add(waveform);

                    if (previousInstant.isPresent()) {
                        // Optimization: Remove data points that are not plotted.
                        for (var key : instantToWaveform.subMap(previousInstant.get(), false, instant, false).keySet()) {
                            instantToWaveform.remove(key);
                        }
                    }

                    previousInstant = Optional.of(instant);
                }
            }
        } else if (pvData instanceof WaterfallPlotRuntime.ScalarPVsData scalarPVsData) {
            ArrayList<Pair<String, ConcurrentSkipListMap<Instant, Double>>> pvNameToInstantToValue = scalarPVsData.pvNameToInstantToValue();
            pvNameToInstantToValue.forEach(pvNameAndInstantToValueAtomicReference -> garbageCollectInstantToValue(pvNameAndInstantToValueAtomicReference.getValue(), t1));

            minFromPV = scalarPVsData.minFromPV().get();
            maxFromPV = scalarPVsData.maxFromPV().get();

            waveformLength = 2 * pvNameToInstantToValue.size() + 1;

            HashMap<String, Optional<Instant>> pvNameToPreviousInstant = new HashMap<>();
            for (var pvNameAndInstantToValue : pvNameToInstantToValue) {
                String pvName = pvNameAndInstantToValue.getKey();
                pvNameToPreviousInstant.put(pvName, Optional.empty());
            }

            for (Instant t = t1.plus(stepsize); t.compareTo(t2) <= 0; t = t.plus(stepsize)) {
                timeValuesLinkedList.add(((double) t.toEpochMilli()) / 1000.0);
                ArrayList<Double> zValues = new ArrayList<>();

                for (var pvNameAndInstantToValue : pvNameToInstantToValue) {
                    String pvName = pvNameAndInstantToValue.getKey();

                    ConcurrentSkipListMap<Instant, Double> instantToValue = pvNameAndInstantToValue.getValue();

                    var instant = instantToValue.floorKey(t);
                    if (instant == null) {
                        zValues.add(Double.NaN);
                        zValues.add(Double.NaN);
                    } else {
                        zValues.add(instantToValue.get(instant));
                        zValues.add(instantToValue.get(instant));

                        Optional<Instant> previousInstant = pvNameToPreviousInstant.get(pvName);
                        if (previousInstant.isPresent()) {
                            // Optimization: Remove data points that are not plotted.
                            for (var key : instantToValue.subMap(previousInstant.get(), false, instant, false).keySet()) {
                                instantToValue.remove(key);
                            }
                        }

                        pvNameToPreviousInstant.put(pvName, Optional.of(instant));
                    }
                }

                // Append the last value one more time in order to
                // fix the plotting when there is only 1 scalar PV:
                var lastValue = zValues.get(zValues.size()-1);
                zValues.add(lastValue);

                zValuesLinkedList.add(zValues);
            }
        }

        final double[][] xyValuesFinal = {{}, {}};
        double[] finalZValues = new double[waveformLength * zValuesLinkedList.size()];

        if (timeOnXAxisValue) {
            {
                double[] xValues = new double[timeValuesLinkedList.size()];
                for (int j = 0; j < xValues.length; j++) {
                    xValues[j] = timeValuesLinkedList.get(j);
                }
                xyValuesFinal[0] = xValues;
            }

            {
                double[] yValues = new double[waveformLength];
                for (int d = 0; d < waveformLength; d++) {
                    // This is needed so that the values align correctly with the y-axis:
                    yValues[d] = (double) d * ((double) (waveformLength + 1) / ((double) (waveformLength)));
                }
                xyValuesFinal[1] = yValues;
            }

            for (int n = 0; n < zValuesLinkedList.size(); n++) {
                ArrayList<Double> waveformValues = zValuesLinkedList.get(n);

                for (int m = 0; m < waveformLength; m++) {
                    double value;
                    if (waveformValues != null && m < waveformValues.size()) {
                        value = waveformValues.get(m);
                    } else {
                        value = Double.NaN;
                    }

                    if (Double.isNaN(observedMin) || observedMin > value) {
                        observedMin = value;
                    }
                    if (Double.isNaN(observedMax) || observedMax < value) {
                        observedMax = value;
                    }

                    finalZValues[n + m * zValuesLinkedList.size()] = value;
                }
            }
        } else {
            {
                double[] yValues = new double[timeValuesLinkedList.size()];
                for (int j = 0; j < yValues.length; j++) {
                    yValues[j] = timeValuesLinkedList.get(j);
                }
                xyValuesFinal[1] = yValues;
            }

            {
                double[] xValues = new double[waveformLength];
                for (int d = 0; d < waveformLength; d++) {
                    // This is needed so that the values align correctly with the x-axis:
                    xValues[d] = (double) d * ((double) (waveformLength + 1) / ((double) (waveformLength)));
                }
                xyValuesFinal[0] = xValues;
            }

            for (int n = 0; n < zValuesLinkedList.size(); n++) {
                ArrayList<Double> waveformValues = zValuesLinkedList.get(n);

                for (int m = 0; m < waveformLength; m++) {
                    double value;
                    if (waveformValues != null && m < waveformValues.size()) {
                        value = waveformValues.get(m);
                    } else {
                        value = Double.NaN;
                    }

                    if (Double.isNaN(observedMin) || observedMin > value) {
                        observedMin = value;
                    }
                    if (Double.isNaN(observedMax) || observedMax < value) {
                        observedMax = value;
                    }

                    finalZValues[m + n * waveformLength] = value;
                }
            }
        }

        dataSet.set(false, xyValuesFinal, finalZValues);
        dataSet.recomputeLimits();
        if (zAxisMinMax.equals(WaterfallPlotWidget.ZAxisMinMax.SetAutomaticallyBasedOnReceivedValues)) {
            if (!Double.isNaN(observedMin) && !Double.isNaN(observedMax) && observedMin < observedMax) {
                double currentMin = zAxis.getMin();
                double currentMax = zAxis.getMax();
                if (!zAxisMinHasBeenAutomaticallySet || observedMin < currentMin + Math.abs(currentMin)*0.1) {
                    zAxis.setMin(getAutomaticMin());
                    zAxisMinHasBeenAutomaticallySet = true;
                }
                if (!zAxisMaxHasBeenAutomaticallySet || observedMax > currentMax - Math.abs(currentMax)*0.1) {
                    zAxis.setMax(getAutomaticMax());
                    zAxisMaxHasBeenAutomaticallySet = true;
                }
            }
        }
        else if (zAxisMinMax.equals(WaterfallPlotWidget.ZAxisMinMax.FromPVLimits)) {
            if (!Double.isNaN(minFromPV) && !Double.isNaN(maxFromPV) && minFromPV < maxFromPV) {
                double computedMin = minFromPV - (maxFromPV - minFromPV) * 0.20;
                double computedMax = maxFromPV + (maxFromPV - minFromPV) * 0.20;
                zAxis.setMin(computedMin);
                zAxis.setMax(computedMax);
            }
        }
    }

    private synchronized static <T> void garbageCollectInstantToValue(ConcurrentSkipListMap<Instant, T> instantToWaveform, Instant t1) {
        // Garbage collect old values that are no longer needed:
        Instant instantOfOldestRelevantKey = instantToWaveform.floorKey(t1);
        if (instantOfOldestRelevantKey != null) {
            for (var key : instantToWaveform.subMap(Instant.MIN, instantOfOldestRelevantKey).keySet()) {
                instantToWaveform.remove(key);
            }
        }
    }

    private WaterfallPlotWidget.ZAxisMinMax zAxisMinMax = WaterfallPlotWidget.ZAxisMinMax.FromPVLimits;

    public synchronized void setZAxisMinMax(WaterfallPlotWidget.ZAxisMinMax zAxisUseAutomaticMinMax) {
        this.zAxisMinMax = zAxisUseAutomaticMinMax;
    }

    private String plotTitle = "";

    public synchronized void setTitle(String newValue) {
        plotTitle = newValue;
        xyChart.setTitle(newValue);
    }

    private Optional<Font> axisLabelFont = Optional.empty();
    private Optional<Font> tickLabelFont = Optional.empty();
    private Optional<Font> titleFont = Optional.empty();

    public synchronized void setTitleFont(Font font) {
        titleFont = Optional.of(font);
        xyChart.getTitleLabel().setFont(font);
    }

    public synchronized void setAxisLabelFont(Font font) {
        axisLabelFont = Optional.of(font);
        xAxis.getAxisLabel().setFont(font);
        yAxis.getAxisLabel().setFont(font);
        zAxis.getAxisLabel().setFont(font);
    }

    public synchronized void setTickLabelFont(Font font) {
        tickLabelFont = Optional.of(font);
        xAxis.getTickLabelStyle().setFont(font);
        yAxis.getTickLabelStyle().setFont(font);
        zAxis.getTickLabelStyle().setFont(font);
    }

    public synchronized void setZAxisName(String zAxisName) {
        if (zAxisName.isEmpty()) {
            zAxis.setName("");
        } else {
            zAxis.setName(zAxisName);
        }
    }

    public synchronized void setZAxisUnit(String zAxisUnit) {
        if (zAxisUnit.isEmpty()) {
            zAxis.setUnit(null);
        } else {
            zAxis.setUnit(zAxisUnit);
        }
    }

    public synchronized Axis getZAxis() {
        return zAxis;
    }

    public synchronized void setPVAxisName(String pvAxisName) {
        if (timeOnXAxis.get()) {
            yAxis.setName(pvAxisName);
            xAxis.setName("Time");
            xAxis.setUnit("UTC");
        }
        else {
            xAxis.setName(pvAxisName);
            yAxis.setName("Time");
            yAxis.setUnit("UTC");
        }
    }

    public synchronized void setPVAxisUnit(String pvAxisUnit) {
        if (timeOnXAxis.get()) {
            yAxis.setUnit(pvAxisUnit.isEmpty() ? null : pvAxisUnit);
            xAxis.setName("Time");
            xAxis.setUnit("UTC");
        }
        else {
            xAxis.setUnit(pvAxisUnit.isEmpty() ? null : pvAxisUnit);
            yAxis.setName("Time");
            yAxis.setUnit("UTC");
        }
    }

    public synchronized void setTimespan(String newTimespan) {
        Optional<Pair<Integer, TimespanUnit>> parsedTimespan = parseTimespanString(newTimespan);
        if (parsedTimespan.isPresent()) {
            timespan = parsedTimespan.get();
        }
    }

    enum TimespanUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        WEEKS
    }

    public static Optional<Pair<Integer, TimespanUnit>> parseTimespanString(String timespanString) {
        int parsedNumber = 0;
        int i = 0;
        for (; i < timespanString.length(); i++) {
            char c = timespanString.charAt(i);
            if (Character.isDigit(c)) {
                parsedNumber = parsedNumber * 10 + Character.getNumericValue(c);
            } else {
                break;
            }
        }
        if (parsedNumber != 0) {
            String stringContainingUnit = timespanString.substring(i).trim().toLowerCase();

            if (stringContainingUnit.equals("") || stringContainingUnit.equals("s") || stringContainingUnit.equals("sec") || stringContainingUnit.equals("secs") || stringContainingUnit.equals("second") || stringContainingUnit.equals("seconds")) {
                return Optional.of(new Pair(parsedNumber, TimespanUnit.SECONDS));
            } else if (stringContainingUnit.equals("m") || stringContainingUnit.equals("min") || stringContainingUnit.equals("mins") || stringContainingUnit.equals("minute") || stringContainingUnit.equals("minutes")) {
                return Optional.of(new Pair(parsedNumber, TimespanUnit.MINUTES));
            } else if (stringContainingUnit.equals("h") || stringContainingUnit.equals("hour") || stringContainingUnit.equals("hours")) {
                return Optional.of(new Pair<>(parsedNumber, TimespanUnit.HOURS));
            } else if (stringContainingUnit.equals("d") || stringContainingUnit.equals("day") || stringContainingUnit.equals("days")) {
                return Optional.of(new Pair<>(parsedNumber, TimespanUnit.DAYS));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}

