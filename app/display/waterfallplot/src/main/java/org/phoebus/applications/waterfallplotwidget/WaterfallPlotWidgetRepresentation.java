package org.phoebus.applications.waterfallplotwidget;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.RuntimeUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WaterfallPlotWidgetRepresentation extends JFXBaseRepresentation<Pane, WaterfallPlotWidget> {

    private StackPane xyChartStackPane;
    private XYChart xyChart;
    private WaterfallPlotController waterfallPlotController;

    @Override
    protected Pane createJFXNode() throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(WaterfallPlotController.class.getResource("WaterfallPlotMain.fxml"));
        Node waterfallPlotMain = loader.load();
        waterfallPlotController = loader.getController();
        xyChartStackPane = waterfallPlotController.xyChartStackPane;
        xyChart = (XYChart) xyChartStackPane.getChildren().get(0);
        registerPropertyListeners();
        runPropertyListeners();

        if (!toolkit.isEditMode()) {
            List<String> pvNames = model_widget.propInputPVs().getValue().stream().map(widgetProperty -> widgetProperty.getValue()).collect(Collectors.toUnmodifiableList());
            boolean isWaveform = model_widget.propInputIsWaveformPV().getValue();
            boolean usePVNumberAsLabelOnAxis = model_widget.propUsePVNumberAsLabelOnAxis().getValue();
            waterfallPlotController.setNewPVName(isWaveform, usePVNumberAsLabelOnAxis, pvNames);
            String zAxisName = model_widget.propZAxisName().getValue();
            waterfallPlotController.setZAxisName(zAxisName);
            String zAxisUnit = model_widget.propZAxisUnit().getValue();
            waterfallPlotController.setZAxisUnit(zAxisUnit);
            WaterfallPlotWidget.ZAxisMinMax zAxisMinMax = model_widget.propZAxisMinMax().getValue();
            waterfallPlotController.setZAxisMinMax(zAxisMinMax);
            if (zAxisMinMax.equals(WaterfallPlotWidget.ZAxisMinMax.SetManually)) {
                double zAxisMin = model_widget.propZAxisMin().getValue();
                double zAxisMax = model_widget.propZAxisMax().getValue();
                waterfallPlotController.getZAxis().setMin(zAxisMin);
                waterfallPlotController.getZAxis().setMax(zAxisMax);
            }
            String pvAxisName = model_widget.propPVAxisName().getValue();
            waterfallPlotController.setPVAxisName(pvAxisName);
            String pvAxisUnit = model_widget.propPVAxisUnit().getValue();
            waterfallPlotController.setPVAxisUnit(pvAxisUnit);
            String timespan = model_widget.propTimespan().getValue();
            waterfallPlotController.setTimespan(timespan);
        }

        {
            double majorTickLength = model_widget.propMajorTickLength().getValue();
            waterfallPlotController.setMajorTickLength(majorTickLength);
        }

        {
            double majorTickWidth = model_widget.propMajorTickWidth().getValue();
            waterfallPlotController.setMajorTickWidth(majorTickWidth);
        }

        {
            double minorTickLength = model_widget.propMinorTickLength().getValue();
            waterfallPlotController.setMinorTickLength(minorTickLength);
        }

        {
            double minorTickWidth = model_widget.propMinorTickWidth().getValue();
            waterfallPlotController.setMinorTickWidth(minorTickWidth);
        }

        {
            ColorGradient colorGradient = colorGradientEnumToColorGradient(model_widget.propColorGradient().getValue());
            waterfallPlotController.contourDataSetRenderer.setColorGradient(colorGradient);
        }

        // Make background of waterfall plot transparent:
        xyChartStackPane.setBackground(Background.EMPTY);

        xyChart.getPlotArea().setOnMousePressed(mouseEvent -> {
            xyChartStackPane.fireEvent(mouseEvent);
        });

        setFonts();
        return xyChartStackPane;
    }

    CompletableFuture<Runnable> futureReturningStopScheduledRedrawing = new CompletableFuture<>();
    public WaterfallPlotWidgetRepresentation() {
        Runnable redrawWaterfallPlot = () -> {
            WaterfallPlotRuntime waterfallPlotRuntime = (WaterfallPlotRuntime) RuntimeUtil.getRuntime(model_widget);
            if (waterfallPlotRuntime == null) {
                return;
            }

            WaterfallPlotRuntime.PVData pvData = waterfallPlotRuntime.getPVData();
            try {
                waterfallPlotController.redraw(pvData);
            } catch (Exception e) {
                // Catch exceptions in order to retry redrawing again if an exception occurs.
            }
        };

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(redrawWaterfallPlot, 1, 1, TimeUnit.SECONDS);
        futureReturningStopScheduledRedrawing.complete(() -> scheduler.shutdown());
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    private final UntypedWidgetPropertyListener waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener = (property, oldValue, newValue) -> {

        WaterfallPlotRuntime waterfallPlotRuntime = (WaterfallPlotRuntime) RuntimeUtil.getRuntime(model_widget);
        if (waterfallPlotRuntime != null) {
            waterfallPlotRuntime.stop();
            waterfallPlotRuntime.initialize(model_widget);
            waterfallPlotRuntime.start();
        }

        boolean isWaveform = model_widget.propInputIsWaveformPV().getValue();
        boolean usePVNumberAsLabelOnAxis = model_widget.propUsePVNumberAsLabelOnAxis().getValue();
        List<String> pvNames = model_widget.propInputPVs().getValue().stream().map(widgetProperty -> widgetProperty.getValue()).collect(Collectors.toUnmodifiableList());
        waterfallPlotController.setNewPVName(isWaveform, usePVNumberAsLabelOnAxis, pvNames);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<Double> majorTickLengthPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setMajorTickLength(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<Double> majorTickWidthPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setMajorTickWidth(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<Double> minorTickLengthPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setMinorTickLength(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<Double> minorTickWidthPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setMinorTickWidth(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<WidgetFont> titleFontPropertyListener = ((property, oldValue, newValue) -> {
        setFonts();
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<WidgetFont> axisLabelFontPropertyListener = ((property, oldValue, newValue) -> {
        setFonts();
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<WidgetFont> tickLabelFontPropertyListener = ((property, oldValue, newValue) -> {
        setFonts();
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<String> zAxisNamePropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setZAxisName(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<String> zAxisUnitPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setZAxisUnit(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<String> pvAxisNamePropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setPVAxisName(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<String> pvAxisUnitPropertyListener = ((property, oldValue, newValue) -> {
        waterfallPlotController.setPVAxisUnit(newValue);
        xyChart.invalidate();
    });

    private final WidgetPropertyListener<String> waterfallPlotNamePropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.setTitle(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<WaterfallPlotWidget.ZAxisMinMax> zAxisAutomaticMinMaxPropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.setZAxisMinMax(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<Double> zAxisMinPropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.getZAxis().setMin(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<Double> zAxisMaxPropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.getZAxis().setMax(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<Integer> heightPropertyListener = (property, oldValue, newValue) -> {
        xyChartStackPane.setPrefHeight(newValue);
        xyChartStackPane.setMinHeight(newValue);
        xyChartStackPane.setMaxHeight(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<Integer> widthPropertyListener = (property, oldValue, newValue) -> {
        xyChartStackPane.setPrefWidth(newValue);
        xyChartStackPane.setMinWidth(newValue);
        xyChartStackPane.setMaxWidth(newValue);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<WaterfallPlotWidget.TimeAxis> timeOnXAxisPropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.setTimeOnXAxis(newValue.equals(WaterfallPlotWidget.TimeAxis.XAxis));
        String pvAxisName = model_widget.propPVAxisName().getValue();
        String pvAxisUnit = model_widget.propPVAxisUnit().getValue();
        waterfallPlotController.setPVAxisName(pvAxisName);
        waterfallPlotController.setPVAxisUnit(pvAxisUnit);
        xyChart.invalidate();
    };

    private final WidgetPropertyListener<String> timespanPropertyListener = (property, oldValue, newValue) -> {
        waterfallPlotController.setTimespan(newValue);
        xyChart.invalidate();
    };

    private ColorGradient colorGradientEnumToColorGradient(WaterfallPlotWidget.ColorGradientEnum colorGradientEnum) {
        if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.RAINBOW)) {
            return ColorGradient.RAINBOW;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.RAINBOW_OPAQUE)) {
            return ColorGradient.RAINBOW_OPAQUE;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.JET)) {
            return ColorGradient.JET;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.TOPO_EXT)) {
            return ColorGradient.TOPO_EXT;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.WHITE_BLACK)) {
            return ColorGradient.WHITE_BLACK;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.BLACK_WHITE)) {
            return ColorGradient.BLACK_WHITE;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.HOT)) {
            return ColorGradient.HOT;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.SUNRISE)) {
            return ColorGradient.SUNRISE;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.VIRIDIS)) {
            return ColorGradient.VIRIDIS;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.BLUE_RED)) {
            return ColorGradient.BLUERED;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.PINK)) {
            return ColorGradient.PINK;
        }
        else if (colorGradientEnum.equals(WaterfallPlotWidget.ColorGradientEnum.RAINBOW_EQ)) {
            return ColorGradient.RAINBOW_EQ;
        }
        else {
            throw new RuntimeException("Missing case");
        }
    }

    private final WidgetPropertyListener<WaterfallPlotWidget.ColorGradientEnum> colorGradientPropertyListener = (property, oldValue, newValue) -> {
        ColorGradient colorGradient = colorGradientEnumToColorGradient(newValue);
        waterfallPlotController.contourDataSetRenderer.setColorGradient(colorGradient);
        xyChart.invalidate();
    };

    private void setFonts() {
        {
            WidgetFont titleWidgetFont = model_widget.propTitleFont().getValue();
            Font titleFont = JFXUtil.convert(titleWidgetFont);
            waterfallPlotController.setTitleFont(titleFont);
        }

        {
            WidgetFont axisLabelWidgetFont = model_widget.propAxisLabelFont().getValue();
            Font axisLabelFont = JFXUtil.convert(axisLabelWidgetFont);
            waterfallPlotController.setAxisLabelFont(axisLabelFont);
        }

        {
            WidgetFont tickLabelWidgetFont = model_widget.propTickLabelFont().getValue();
            Font tickLabelFont = JFXUtil.convert(tickLabelWidgetFont);
            waterfallPlotController.setTickLabelFont(tickLabelFont);
        }
    }

    private void runPropertyListeners() {
        titleFontPropertyListener.propertyChanged(null, null, model_widget.propTitleFont().getValue());
        axisLabelFontPropertyListener.propertyChanged(null, null, model_widget.propAxisLabelFont().getValue());
        tickLabelFontPropertyListener.propertyChanged(null, null, model_widget.propTickLabelFont().getValue());
        waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener.propertyChanged(null, null, null);
        waterfallPlotNamePropertyListener.propertyChanged(null, null, model_widget.propName().getValue());
        widthPropertyListener.propertyChanged(null, null, model_widget.propWidth().getValue());
        heightPropertyListener.propertyChanged(null, null, model_widget.propHeight().getValue());
        timeOnXAxisPropertyListener.propertyChanged(null, null, model_widget.propTimeAxis().getValue());
        colorGradientPropertyListener.propertyChanged(null, null, model_widget.propColorGradient().getValue());
        zAxisNamePropertyListener.propertyChanged(null, null, model_widget.propZAxisName().getValue());
        zAxisUnitPropertyListener.propertyChanged(null, null, model_widget.propZAxisUnit().getValue());
        zAxisAutomaticMinMaxPropertyListener.propertyChanged(null, null, model_widget.propZAxisMinMax().getValue());
        zAxisMinPropertyListener.propertyChanged(null, null, model_widget.propZAxisMin().getValue());
        zAxisMaxPropertyListener.propertyChanged(null, null, model_widget.propZAxisMax().getValue());
        pvAxisNamePropertyListener.propertyChanged(null, null, model_widget.propPVAxisName().getValue());
        pvAxisUnitPropertyListener.propertyChanged(null, null, model_widget.propPVAxisUnit().getValue());
        timespanPropertyListener.propertyChanged(null, null, model_widget.propTimespan().getValue());
        majorTickLengthPropertyListener.propertyChanged(null, null, model_widget.propMajorTickLength().getValue());
        majorTickWidthPropertyListener.propertyChanged(null, null, model_widget.propMajorTickWidth().getValue());
        minorTickLengthPropertyListener.propertyChanged(null, null, model_widget.propMinorTickLength().getValue());
        minorTickWidthPropertyListener.propertyChanged(null, null, model_widget.propMinorTickWidth().getValue());
    }

    private void registerPropertyListeners() {
        model_widget.propTitleFont().addPropertyListener(titleFontPropertyListener);
        model_widget.propAxisLabelFont().addPropertyListener(axisLabelFontPropertyListener);
        model_widget.propTickLabelFont().addPropertyListener(tickLabelFontPropertyListener);
        model_widget.propInputPVs().addUntypedPropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propInputIsWaveformPV().addUntypedPropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propUsePVNumberAsLabelOnAxis().addUntypedPropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propName().addPropertyListener(waterfallPlotNamePropertyListener);
        model_widget.propWidth().addPropertyListener(widthPropertyListener);
        model_widget.propHeight().addPropertyListener(heightPropertyListener);
        model_widget.propTimeAxis().addPropertyListener(timeOnXAxisPropertyListener);
        model_widget.propColorGradient().addPropertyListener(colorGradientPropertyListener);
        model_widget.propZAxisName().addPropertyListener(zAxisNamePropertyListener);
        model_widget.propZAxisUnit().addPropertyListener(zAxisUnitPropertyListener);
        model_widget.propZAxisMinMax().addPropertyListener(zAxisAutomaticMinMaxPropertyListener);
        model_widget.propZAxisMin().addPropertyListener(zAxisMinPropertyListener);
        model_widget.propZAxisMax().addPropertyListener(zAxisMaxPropertyListener);
        model_widget.propPVAxisName().addPropertyListener(pvAxisNamePropertyListener);
        model_widget.propPVAxisUnit().addPropertyListener(pvAxisUnitPropertyListener);
        model_widget.propTimespan().addPropertyListener(timespanPropertyListener);
        model_widget.propMajorTickLength().addPropertyListener(majorTickLengthPropertyListener);
        model_widget.propMajorTickWidth().addPropertyListener(majorTickWidthPropertyListener);
        model_widget.propMinorTickLength().addPropertyListener(minorTickLengthPropertyListener);
        model_widget.propMinorTickWidth().addPropertyListener(minorTickWidthPropertyListener);
    }

    private void unregisterPropertyListeners() {
        model_widget.propTitleFont().removePropertyListener(titleFontPropertyListener);
        model_widget.propInputPVs().removePropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propInputIsWaveformPV().removePropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propUsePVNumberAsLabelOnAxis().removePropertyListener(waveformAndUsePVNumberAsLabelOnAxisAndPVNamesPropertyListener);
        model_widget.propName().removePropertyListener(waterfallPlotNamePropertyListener);
        model_widget.propWidth().removePropertyListener(widthPropertyListener);
        model_widget.propHeight().removePropertyListener(heightPropertyListener);
        model_widget.propTimeAxis().removePropertyListener(timeOnXAxisPropertyListener);
        model_widget.propColorGradient().removePropertyListener(colorGradientPropertyListener);
        model_widget.propZAxisName().removePropertyListener(zAxisNamePropertyListener);
        model_widget.propZAxisUnit().removePropertyListener(zAxisUnitPropertyListener);
        model_widget.propZAxisMinMax().removePropertyListener(zAxisAutomaticMinMaxPropertyListener);
        model_widget.propZAxisMin().removePropertyListener(zAxisMinPropertyListener);
        model_widget.propZAxisMax().removePropertyListener(zAxisMaxPropertyListener);
        model_widget.propPVAxisName().removePropertyListener(pvAxisNamePropertyListener);
        model_widget.propPVAxisUnit().removePropertyListener(pvAxisUnitPropertyListener);
        model_widget.propTimespan().removePropertyListener(timespanPropertyListener);
        model_widget.propMajorTickLength().removePropertyListener(majorTickLengthPropertyListener);
        model_widget.propMajorTickWidth().removePropertyListener(majorTickWidthPropertyListener);
        model_widget.propMinorTickLength().removePropertyListener(minorTickLengthPropertyListener);
        model_widget.propMinorTickWidth().removePropertyListener(minorTickWidthPropertyListener);
    }

    @Override
    public void dispose()
    {
        try {
            Runnable stopScheduledRedrawing = futureReturningStopScheduledRedrawing.get();
            stopScheduledRedrawing.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            unregisterPropertyListeners();
            super.dispose();
        }
    }
}
