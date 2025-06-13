package org.csstudio.trends.databrowser3.ui.waveformoverlapview;

import javafx.application.Platform;
import org.csstudio.javafx.rtplot.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Enumeration of configurable trace properties for RTValuePlot.
 * Each property is associated with its expected value type.
 */
enum TraceProperty {
    TRACE_STYLE(TraceType.class),
    LINE_WIDTH(Integer.class),
    LINE_STYLE(LineStyle.class),
    POINT_SIZE(Integer.class),
    POINT_TYPE(PointType.class);

    private final Class<?> type;

    TraceProperty(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }
}

/**
 * Configuration manager for trace properties in an RTValuePlot.
 * This class provides centralized control over visual properties
 * (e.g., line style, point size) for all traces in a plot.
 *
 * @author Mingtao Li
 * China Spallation Neutron Sources
 */
public class TraceConfig {

    private final RTValuePlot plot;
    private TraceType traceType = TraceType.LINES_DIRECT;
    private int lineWidth = 1;
    private LineStyle lineStyle = LineStyle.SOLID;
    private int pointSize = 5;
    private PointType pointType = PointType.NONE;

    public TraceConfig(RTValuePlot plot) {
        this.plot = Objects.requireNonNull(plot, "RTValuePlot cannot be null");
    }

    /**
     * Updates a trace property for all traces in the plot.
     *
     * @param property The trace property to update
     * @param value    The new value for the property, must match the expected type
     * @throws IllegalArgumentException if the value type is invalid
     */
    public void updateProperty(TraceProperty property, Object value) {
        // Validate value type against the property's expected type
        if (!property.getType().isInstance(value)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid value type for %s. Expected: %s, actual: %s",
                    property.name(),
                    property.getType().getName(),
                    value.getClass().getName()
            ));
        }

        // Get all current traces from the plot
        List<Trace<Double>> traces = StreamSupport.stream(plot.getTraces().spliterator(), false)
                .collect(Collectors.toList());

        // Update the property for all traces
        switch (property) {
            case TRACE_STYLE:
                this.traceType = (TraceType) value;
                updateAllTraces(trace -> trace.setType(traceType));
                break;

            case LINE_WIDTH:
                this.lineWidth = (int) value;
                updateAllTraces(trace -> trace.setWidth(lineWidth));
                break;

            case LINE_STYLE:
                this.lineStyle = (LineStyle) value;
                // Special handling: only apply non-solid line styles if line width > 0
                if (lineWidth > 0 || lineStyle == LineStyle.SOLID) {
                    updateAllTraces(trace -> trace.setLineStyle(lineStyle));
                }
                break;

            case POINT_SIZE:
                this.pointSize = (int) value;
                updateAllTraces(trace -> trace.setPointSize(pointSize));
                break;

            case POINT_TYPE:
                this.pointType = (PointType) value;
                updateAllTraces(trace -> trace.setPointType(pointType));
                break;
        }

        // Request UI update on the JavaFX application thread
        Platform.runLater(plot::requestUpdate);
    }

    /**
     * Applies an update operation to all traces in the plot.
     */
    private void updateAllTraces(TraceUpdater updater) {
        plot.getTraces().forEach(updater::update);
    }

    /**
     * Functional interface for trace update operations.
     */
    @FunctionalInterface
    private interface TraceUpdater {
        void update(Trace<Double> trace);
    }

    // Getter methods (uncomment as needed)
    // public TraceType getTraceType() { return traceType; }
    // public int getLineWidth() { return lineWidth; }
    // Additional getters...
}