package org.csstudio.trends.databrowser3.ui.waveformview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.trends.databrowser3.Messages;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.HistoryDataSetRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Displays the current waveform and fading history for one or more PVs.
 * Each PV has an independent Y-axis; the toolbar can stagger their ranges.
 */
public class WaveformHistoryChart extends BorderPane {

    /**
     * A named PV and its waveforms ordered oldest to newest.
     */
    public record WaveformSeries(String name, List<double[]> waveforms) {
    }

    private static final double HISTORY_FADING = 0.9;
    private static final double ICON_SCALE = 0.8;
    private static final double STAGGER_GAP = 0.1;
    private static final Font AXIS_TITLE_FONT = Font.font("System", FontWeight.BOLD, 12);

    private final DefaultNumericAxis xAxis;
    private final DefaultNumericAxis mainYAxis;
    private final XYChart chart;
    private final ToolBar toolbar;

    private final List<DoubleDataSet> waveforms = new ArrayList<>();
    private final List<HistoryDataSetRenderer> renderers = new ArrayList<>();
    private final List<DefaultNumericAxis> extraYAxes = new ArrayList<>();
    private final List<double[]> valueRanges = new ArrayList<>();
    private final WaveformCursor waveformCursor = new WaveformCursor();

    private int historyLength;
    private boolean axisColorUpdatePending;

    public WaveformHistoryChart(int historyLength) {
        xAxis = new DefaultNumericAxis("Sample");
        mainYAxis = new DefaultNumericAxis("Value");
        xAxis.setAutoRanging(true);
        mainYAxis.setAutoRanging(true);
        xAxis.setUnit(null);
        mainYAxis.setUnit(null);

        chart = new XYChart(xAxis, mainYAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);

        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(waveformCursor);

        // ChartFX dataset line colors are resolved by JavaFX CSS after the
        // chart is attached to a Scene.
        chart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                scheduleAxisColorUpdate();
            }
        });

        setHistoryLength(historyLength);
        toolbar = createToolbar();
        setTop(toolbar);
        setCenter(chart);
    }

    private ToolBar createToolbar() {
        chart.toolBarSideProperty().bind(new SimpleObjectProperty<>(Side.TOP));
        chart.getMenuPane().setTop(null);

        var sourceToolbar = chart.getToolBar();
        List<Node> controls = new ArrayList<>(sourceToolbar.getChildren());
        sourceToolbar.getChildren().clear();

        HBox buttonBox = new HBox(4);
        for (Node control : controls) {
            if (control instanceof HBox innerBox) {
                buttonBox.getChildren().addAll(new ArrayList<>(innerBox.getChildren()));
                innerBox.getChildren().clear();
            } else {
                buttonBox.getChildren().add(control);
            }
        }

        while (!buttonBox.getChildren().isEmpty()
                && buttonBox.getChildren().get(0) instanceof Separator) {
            buttonBox.getChildren().remove(0);
        }

        // Same icon as the original RTPlot Stagger button.
        Button stagger = new Button();
        stagger.setTooltip(new Tooltip(Messages.WaveformHistoryStaggerTT));
        stagger.setGraphic(new ImageView(Activator.getIcon("stagger")));
        stagger.setOnAction(event -> staggerWaveforms());
        // Insert ahead of ChartFX's existing zoom/pan controls.
        buttonBox.getChildren().add(0, stagger);

        // Kay-style crosshair: a toggle that reads the current sample of
        // every selected waveform at the same X position.
        ToggleButton crosshair = new ToggleButton();
        crosshair.setTooltip(new Tooltip(Messages.WaveformHistoryCursorTT));
        crosshair.setGraphic(new ImageView(Activator.getIcon("crosshair")));
        crosshair.setOnAction(event -> waveformCursor.setActive(crosshair.isSelected()));
        buttonBox.getChildren().add(1, crosshair);

        for (Node control : buttonBox.getChildren()) {
            // Preserve the original Stagger and Crosshair icon contrast.
            compactControl(control, (control == crosshair || control == stagger) ? 0.0 : 0.45);
            sizeButton(control, 34, 26);
        }

        ToolBar result = new ToolBar(buttonBox);
        result.setPadding(new Insets(2, 4, 2, 4));
        result.setMinHeight(Region.USE_PREF_SIZE);
        result.setMaxHeight(Region.USE_PREF_SIZE);
        result.setStyle("-fx-font-size: 10px;"
                + "-fx-background-color: transparent;"
                + "-fx-border-width: 0;");
        return result;
    }

    /**
     * Place Inspect Waveforms controls on the same toolbar as the chart tools.
     * Call on the JavaFX application thread.
     */
    public void addToolbarControls(Node... controls) {
        toolbar.getItems().addAll(controls);
    }

    private static void compactControl(Node node, double brightness) {
        if (node instanceof ButtonBase button) {
            Node graphic = button.getGraphic();
            if (graphic != null) {
                button.setGraphic(null);
                graphic.setScaleX(graphic.getScaleX() * ICON_SCALE);
                graphic.setScaleY(graphic.getScaleY() * ICON_SCALE);
                ColorAdjust adjust = new ColorAdjust();
                adjust.setBrightness(brightness);
                graphic.setEffect(adjust);
                button.setGraphic(new Group(graphic));
            }
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                compactControl(child, brightness);
            }
        }
    }

    /**
     * Change the number of previous waveforms retained (on FX thread).
     */
    public void setHistoryLength(int historyLength) {
        final int length = Math.max(0, historyLength);
        if (this.historyLength == length) {
            return;
        }
        this.historyLength = length;
        resetHistoryRenderers();
        chart.invalidate();
    }

    /**
     * Recreate renderers without changing the current Y-axis arrangement.
     */
    private void resetHistoryRenderers() {
        // Old cursor markers/readouts refer to the previous datasets.
        waveformCursor.hideCursor();
        renderers.clear();
        for (int pv = 0; pv < waveforms.size(); pv++) {
            HistoryDataSetRenderer renderer = new HistoryDataSetRenderer(historyLength);
            renderer.setIntensityFading(HISTORY_FADING);
            renderer.setGlobalIndexOffset(pv);
            renderer.getDatasets().add(waveforms.get(pv));
            renderer.getAxes().addAll(xAxis, axisForPv(pv));
            renderers.add(renderer);
        }
        chart.getRenderers().setAll(renderers);
        scheduleAxisColorUpdate();
    }

    /**
     * Schedule one CSS color refresh per JavaFX pulse, even if renderers
     * have been recreated repeatedly before that pulse executes.
     */
    private void scheduleAxisColorUpdate() {
        if (axisColorUpdatePending) {
            return;
        }
        axisColorUpdatePending = true;
        Platform.runLater(() -> {
            axisColorUpdatePending = false;
            updateAxisColorsFromRenderers();
        });
    }

    /**
     * Match each Y-axis title to the CSS-resolved line color of its PV.
     * Using DataSetNode avoids maintaining a separate color palette.
     */
    private void updateAxisColorsFromRenderers() {
        if (chart.getScene() == null) {
            return;
        }

        chart.applyCss();
        for (int pv = 0; pv < renderers.size(); pv++) {
            HistoryDataSetRenderer renderer = renderers.get(pv);
            if (renderer.getDatasetNodes().isEmpty()) {
                continue;
            }
            var lineColor = renderer.getDatasetNodes().get(0).getLineColor();
            if (lineColor != null) {
                axisForPv(pv).getAxisLabel().setFill(lineColor);
            }
        }
    }

    private DefaultNumericAxis axisForPv(int pv) {
        // Every PV has its own Y-axis, even before Stagger is pressed.
        return pv == 0 ? mainYAxis : extraYAxes.get(pv - 1);
    }

    /**
     * Replace all displayed PVs and their histories (on FX thread).
     */
    public void setWaveformSeries(List<WaveformSeries> series) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "setWaveformSeries must run on the JavaFX application thread");
        }

        // Changing the PV selection resets any manually staggered ranges.
        // Navigating samples within the same selection preserves them.
        if (!hasSamePvSelection(series)) {
            clearStaggering();
        }

        // Detach old renderers before dropping any axes no longer needed.
        chart.getRenderers().clear();
        waveforms.clear();
        ensureYAxes(series.size());
        valueRanges.clear();
        for (WaveformSeries item : series) {
            waveforms.add(new DoubleDataSet(item.name(), 100));
            valueRanges.add(rangeOf(item.waveforms()));
        }
        // Label each Y-axis with the corresponding PV name.
        for (int pv = 0; pv < series.size(); pv++) {
            DefaultNumericAxis axis = axisForPv(pv);
            axis.setName(series.get(pv).name());
            axis.setUnit(null);
            axis.getAxisLabel().setFont(AXIS_TITLE_FONT);
        }
        resetHistoryRenderers();

        // Each renderer owns one independent history buffer, so each PV can
        // be loaded separately without inserting blank history frames.
        for (int pv = 0; pv < series.size(); pv++) {
            List<double[]> values = series.get(pv).waveforms();
            DoubleDataSet dataset = waveforms.get(pv);
            HistoryDataSetRenderer renderer = renderers.get(pv);
            int from = Math.max(0, values.size() - historyLength - 1);
            for (int idx = from; idx < values.size(); idx++) {
                if (idx > from) {
                    renderer.shiftHistory();
                }
                replaceWaveform(dataset, values.get(idx));
            }
        }
        chart.invalidate();
    }

    private boolean hasSamePvSelection(List<WaveformSeries> series) {
        if (series.size() != waveforms.size()) {
            return false;
        }
        for (int pv = 0; pv < series.size(); pv++) {
            if (!waveforms.get(pv).getName().equals(series.get(pv).name())) {
                return false;
            }
        }
        return true;
    }

    /**
     * One-shot Stagger action based on RTPlot/PlotProcessor.stagger().
     * Each PV's values occupy one of N nonoverlapping vertical bands with
     * 10% internal padding. Raw sample values are never changed.
     */
    public void staggerWaveforms() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("staggerWaveforms must run on the FX thread");
        }
        int count = waveforms.size();
        if (count < 2) {
            return;
        }

        for (int pv = 0; pv < count; pv++) {
            DefaultNumericAxis axis = axisForPv(pv);
            double low = valueRanges.get(pv)[0];
            double high = valueRanges.get(pv)[1];
            if (!Double.isFinite(low) || !Double.isFinite(high) || low > high) {
                continue;
            }
            if (low == high) {
                double half = Math.abs(low / 2.0);
                if (half == 0) half = 1.0;
                low -= half;
                high += half;
            }
            double span = high - low;
            low -= STAGGER_GAP * span;
            high += STAGGER_GAP * span;
            span = high - low;

            // Match Kay's rule: first PV on top, last PV on bottom.
            low -= (count - pv - 1) * span;
            high += pv * span;

            axis.setAutoRanging(false);
            axis.setMin(low);
            axis.setMax(high);

            HistoryDataSetRenderer renderer = renderers.get(pv);
            renderer.getAxes().setAll(xAxis, axis);

            // Refresh the renderer's cached xAxis and yAxis references.
            renderer.updateAxes();
        }
        chart.invalidate();
    }

    private void ensureYAxes(int count) {
        int required = Math.max(0, count - 1);

        // Create the axes at PV-selection time, not on the first Stagger click.
        while (extraYAxes.size() < required) {
            DefaultNumericAxis axis = new DefaultNumericAxis("Value");
            axis.setSide(Side.LEFT);
            axis.setUnit(null);
            axis.setAutoRanging(true);
            extraYAxes.add(axis);
            chart.getAxes().add(axis);
        }

        // Retire axes when fewer PVs are selected.
        while (extraYAxes.size() > required) {
            DefaultNumericAxis axis = extraYAxes.remove(extraYAxes.size() - 1);
            chart.getAxes().remove(axis);
        }
    }

    /**
     * Clear the manual stagger ranges, but keep one Y-axis per PV.
     * Unused axes are removed by ensureYAxes() when the PV count changes.
     */
    private void clearStaggering() {
        mainYAxis.setAutoRanging(true);
        for (DefaultNumericAxis axis : extraYAxes) {
            axis.setAutoRanging(true);
        }
    }

    static double[] rangeOf(List<double[]> history) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (double[] values : history) {
            if (values == null) {
                continue;
            }
            for (double value : values) {
                if (!Double.isFinite(value)) {
                    continue;
                }
                low = Math.min(low, value);
                high = Math.max(high, value);
            }
        }
        return new double[]{low, high};
    }

    private static void replaceWaveform(DoubleDataSet dataset, double[] values) {
        dataset.clearData();
        for (int sample = 0; sample < values.length; sample++) {
            dataset.add(sample, values[sample]);
        }
    }

    /**
     * A ChartFX plugin that displays a common sample cursor across the
     * independently scaled Y-axes. Only the latest waveform for each PV
     * contributes to the readout; history traces remain visually unchanged.
     */
    private final class WaveformCursor extends ChartPlugin {
        private final Line vertical = new Line();
        private final Line horizontal = new Line();
        private final Group markers = new Group();
        private final VBox readout = new VBox(2);
        private final Label sampleLabel = new Label();
        private final List<Label> valueLabels = new ArrayList<>();
        private final List<Circle> markerNodes = new ArrayList<>();
        private boolean active;

        WaveformCursor() {
            setAddButtonsToToolBar(false);
            vertical.setStroke(Color.rgb(70, 70, 70, 0.7));
            horizontal.setStroke(Color.rgb(70, 70, 70, 0.45));
            vertical.getStrokeDashArray().addAll(4.0, 3.0);
            horizontal.getStrokeDashArray().addAll(4.0, 3.0);

            readout.setManaged(false);
            readout.setMouseTransparent(true);
            readout.setStyle("-fx-background-color: rgba(255,255,255,0.94);"
                    + "-fx-border-color: #aaaaaa;"
                    + "-fx-border-radius: 3; -fx-background-radius: 3;"
                    + "-fx-padding: 5;");
            sampleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            readout.getChildren().add(sampleLabel);

            vertical.setMouseTransparent(true);
            horizontal.setMouseTransparent(true);
            markers.setMouseTransparent(true);

            registerInputEventHandler(MouseEvent.MOUSE_MOVED, this::mouseMoved);
            registerInputEventHandler(MouseEvent.MOUSE_EXITED, event -> hideCursor());
        }

        void setActive(boolean enabled) {
            active = enabled;
            if (!enabled) {
                hideCursor();
            }
        }

        void hideCursor() {
            if (!getChartChildren().isEmpty()) {
                getChartChildren().clear();
            }
        }

        /** Allocate cursor nodes only when the number of displayed PVs grows. */
        private void ensureCursorEntries(int count) {
            while (valueLabels.size() < count) {
                Label valueLabel = new Label();
                valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
                valueLabel.setVisible(false);
                valueLabel.setManaged(false);
                valueLabels.add(valueLabel);
                readout.getChildren().add(valueLabel);

                Circle marker = new Circle(4);
                marker.setStroke(Color.WHITE);
                marker.setStrokeWidth(1.5);
                marker.setMouseTransparent(true);
                marker.setVisible(false);
                markerNodes.add(marker);
                markers.getChildren().add(marker);
            }
            while (valueLabels.size() > count) {
                int last = valueLabels.size() - 1;
                readout.getChildren().remove(valueLabels.remove(last));
                markers.getChildren().remove(markerNodes.remove(last));
            }
        }

        private void mouseMoved(MouseEvent event) {
            if (!active || !isMouseEventWithinCanvas(event)
                    || waveforms.isEmpty() || chart.getCanvas().getScene() == null) {
                hideCursor();
                return;
            }

            // Both the chart axes and the plugin overlay use canvas-local
            // coordinates. This avoids offsets from the additional Y-axes.
            Point2D mouse = chart.getCanvas().sceneToLocal(
                    event.getSceneX(), event.getSceneY());
            Bounds bounds = chart.getCanvas().getBoundsInLocal();
            if (!bounds.contains(mouse)) {
                hideCursor();
                return;
            }

            // The waveform X coordinates are consecutive integer sample indices.
            int sample = (int) Math.round(xAxis.getValueForDisplay(mouse.getX()));
            double cursorX = xAxis.getDisplayPosition(sample);
            if (!Double.isFinite(cursorX) || cursorX < bounds.getMinX()
                    || cursorX > bounds.getMaxX()) {
                hideCursor();
                return;
            }

            vertical.setStartX(cursorX);
            vertical.setEndX(cursorX);
            vertical.setStartY(bounds.getMinY());
            vertical.setEndY(bounds.getMaxY());
            horizontal.setStartX(bounds.getMinX());
            horizontal.setEndX(bounds.getMaxX());
            horizontal.setStartY(mouse.getY());
            horizontal.setEndY(mouse.getY());

            sampleLabel.setText(Messages.WaveformHistorySample + " " + sample);
            ensureCursorEntries(waveforms.size());

            for (int pv = 0; pv < waveforms.size(); pv++) {
                Label valueLabel = valueLabels.get(pv);
                Circle marker = markerNodes.get(pv);
                valueLabel.setVisible(false);
                valueLabel.setManaged(false);
                marker.setVisible(false);

                DoubleDataSet dataset = waveforms.get(pv);
                if (sample < 0 || sample >= dataset.getDataCount()) {
                    continue; // no waveform values for this PV at this sample
                }

                double value = dataset.get(DataSet.DIM_Y, sample);
                if (!Double.isFinite(value)) {
                    continue;
                }
                double y = axisForPv(pv).getDisplayPosition(value);
                Paint color = Color.BLACK;
                if (pv < renderers.size() && !renderers.get(pv).getDatasetNodes().isEmpty()) {
                    Paint actualColor = renderers.get(pv).getDatasetNodes().get(0).getLineColor();
                    if (actualColor != null) {
                        color = actualColor;
                    }
                }

                valueLabel.setText(String.format(Locale.US,
                        "%s: %.6g", dataset.getName(), value));
                valueLabel.setTextFill(color);
                valueLabel.setManaged(true);
                valueLabel.setVisible(true);

                if (Double.isFinite(y) && y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                    marker.setCenterX(cursorX);
                    marker.setCenterY(y);
                    marker.setFill(color);
                    marker.setVisible(true);
                }
            }

            // Attach the overlay before measuring it so JavaFX can resolve
            // the labels' actual CSS font metrics on the first mouse move.
            if (getChartChildren().isEmpty()) {
                getChartChildren().addAll(vertical, horizontal, markers, readout);
            }

            // Keep the readout within the visible plot, next to the cursor.
            readout.applyCss();
            double width = readout.prefWidth(-1);
            double height = readout.prefHeight(width);
            double left = cursorX + 12;
            if (left + width > bounds.getMaxX()) {
                left = cursorX - width - 12;
            }
            left = Math.max(bounds.getMinX(), Math.min(left, bounds.getMaxX() - width));
            double top = Math.max(bounds.getMinY(),
                    Math.min(mouse.getY() + 12, bounds.getMaxY() - height));
            readout.resizeRelocate(left, top, width, height);
        }
    }

    private static void sizeButton(Node node, double width, double height) {
        if (node instanceof ButtonBase button) {
            button.setMinSize(width, height);
            button.setPrefSize(width, height);
            button.setMaxSize(width, height);
            button.setPadding(Insets.EMPTY);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                sizeButton(child, width, height);
            }
        }
    }
}
