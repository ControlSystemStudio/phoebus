
package org.csstudio.display.extra.widgets.linearmeter;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.IllegalPathStateException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javafx.application.Platform;
import org.csstudio.javafx.rtplot.internal.AxisPart;
import org.csstudio.javafx.rtplot.internal.PlotPart;
import org.csstudio.javafx.rtplot.internal.PlotPartListener;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;

/**
 * @author European Spallation Source ERIC
 * @version 1.1
 *
 * Version 1.0 implemented by Fredrik Söderberg.
 * Version 1.1 (some fixes and improvements) by Abraham Wolk.
 *
 * The Linear Meter graphics design is by Dirk Nordt and Fredrik Söderberg.
 *
 */

@SuppressWarnings("nls")
public class RTLinearMeter extends ImageView
{
    // Note: All methods that are called from different threads must ensure thread-safety by running
    // relevant code on the JavaFX application thread. (The helper function runOnJavaFXThread() can
    // be used for this.)

    public RTLinearMeter(double initialValue,
                         int width,
                         int height,
                         double min,
                         double max,
                         double minMaxTolerance,
                         double loLo,
                         double low,
                         double high,
                         double hiHi,
                         boolean showUnits,
                         boolean showLimits,
                         boolean isHorizontal,
                         boolean isGradientEnabled,
                         boolean isHighlightActiveRegionEnabled,
                         Color normalStatusColor,
                         Color minorAlarmColor,
                         Color majorAlarmColor,
                         int needleWidth,
                         Color needleColor,
                         int knobSize,
                         Color knobColor,
                         boolean showWarnings)
    {
        if (warningTriangle == null) {
            try {
                warningTriangle = ImageIO.read(getClass().getResource("/graphics/Warning_Triangle_Red.png"));
            } catch (IOException ioException) {
                logger.log(Level.WARNING, "Unable to load warning triangle icon!");
            }
        }

        if (Double.isFinite(min) && Double.isFinite(max)) {
            validRange.set(true);
        }
        else {
            validRange.set(false);
            min = 0.0;
            max = 100.0;
        }

        linearMeterScale = new LinearMeterScale(plot_part_listener,
                                                width,
                                                height,
                                                isHorizontal,
                                                min,
                                                max);

        this.minMaxTolerance.set(minMaxTolerance);
        this.loLo.set(loLo);
        this.low.set(low);
        this.high.set(high);
        this.hiHi.set(hiHi);
        this.showUnits.set(showUnits);
        this.showLimits.set(showLimits);
        this.showWarnings.set(showWarnings);

        layout();

        this.currentValue.set(initialValue);
        this.isGradientEnabled.set(isGradientEnabled);
        this.isHighlightActiveRegionEnabled.set(isHighlightActiveRegionEnabled);

        this.needleWidth.set(needleWidth);
        this.needleColor.set(needleColor);
        this.knobSize.set(knobSize);
        this.knobColor.set(knobColor);

        setNormalStatusColor(normalStatusColor);
        setMinorAlarmColor(minorAlarmColor);
        setMajorAlarmColor(majorAlarmColor);

        requestLayout();
    }

    private AtomicBoolean showUnits = new AtomicBoolean(true);
    private AtomicReference<String> units = new AtomicReference<>("");
    private AtomicBoolean showLimits = new AtomicBoolean(true);

    private AtomicReference<Double> loLo = new AtomicReference<>(0.0);
    private AtomicReference<Double> low = new AtomicReference<>(0.0);
    private AtomicReference<Double> high = new AtomicReference<>(100.0);
    private AtomicReference<Double> hiHi = new AtomicReference<>(100.0);

    private static Image warningTriangle = null;

    public void redrawLinearMeterScale() {
        boolean isHorizontal = linearMeterScale.isHorizontal();
        linearMeterScale = new LinearMeterScale(plot_part_listener,
                                                linearMeterScale.getBounds().width,
                                                linearMeterScale.getBounds().height,
                                                linearMeterScale.isHorizontal(),
                                                linearMeterScale.getValueRange().getLow(),
                                                linearMeterScale.getValueRange().getHigh());
        linearMeterScale.setHorizontal(isHorizontal);
        if (font != null) {
            linearMeterScale.setScaleFont(GraphicsUtils.convert(font));
        }
    }

    private enum WARNING {
        NONE,
        VALUE_LESS_THAN_MIN,
        VALUE_GREATER_THAN_MAX,
        MIN_AND_MAX_NOT_DEFINED,
        LAG,
        NO_UNIT
    }

    private AtomicReference<WARNING> currentWarning = new AtomicReference<WARNING>(WARNING.NONE);

    /** Colors */
    private AtomicReference<Color> foreground = new AtomicReference<>(Color.BLACK);
    private AtomicReference<Color> background = new AtomicReference<>(Color.WHITE);

    /** Fonts */
    private Font font;

    /** Empty listener to LinearMeterScale */
    protected PlotPartListener plot_part_listener = new PlotPartListener()
    {
        @Override
        public void layoutPlotPart(PlotPart plotPart) { }

        @Override
        public void refreshPlotPart(PlotPart plotPart) { }
    };

    private AtomicBoolean validRange = new AtomicBoolean(false);
    private AtomicReference<Double> minMaxTolerance = new AtomicReference<>(0.0);

    public boolean getValidRange() {
        return validRange.get();
    }

    /** Optional scale of this linear meter */
    public LinearMeterScale linearMeterScale;

    /** Value to display */
    private AtomicReference<Double> currentValue = new AtomicReference<Double>(Double.NaN);

    private AtomicReference<Color> normalStatusColor_lowlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);
    private AtomicReference<Color> normalStatusColorGradientStartPoint_lowlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);
    private AtomicReference<Color> normalStatusColorGradientEndPoint_lowlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);
    private AtomicReference<Color> normalStatusColor_highlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);
    private AtomicReference<Color> normalStatusColorGradientStartPoint_highlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);
    private AtomicReference<Color> normalStatusColorGradientEndPoint_highlighted = new AtomicReference<Color>(Color.LIGHT_GRAY);

    private AtomicReference<Color> minorAlarmColor_lowlighted = new AtomicReference<Color>(Color.ORANGE);
    private AtomicReference<Color> minorAlarmColor_highlighted = new AtomicReference<Color>(Color.ORANGE);
    private AtomicReference<Color> minorAlarmColorGradientStartPoint_lowlighted = new AtomicReference<Color>(Color.ORANGE);
    private AtomicReference<Color> minorAlarmColorGradientEndPoint_lowlighted = new AtomicReference<Color>(Color.ORANGE);
    private AtomicReference<Color> minorAlarmColorGradientStartPoint_highlighted = new AtomicReference<Color>(Color.ORANGE);
    private AtomicReference<Color> minorAlarmColorGradientEndPoint_highlighted = new AtomicReference<Color>(Color.ORANGE);

    private AtomicReference<Color> majorAlarmColor_lowlighted = new AtomicReference<Color>(Color.RED);
    private AtomicReference<Color> majorAlarmColor_highlighted = new AtomicReference<Color>(Color.RED);
    private AtomicReference<Color> majorAlarmColorGradientStartPoint_lowlighted = new AtomicReference<Color>(Color.RED);
    private AtomicReference<Color> majorAlarmColorGradientEndPoint_lowlighted = new AtomicReference<Color>(Color.RED);
    private AtomicReference<Color> majorAlarmColorGradientStartPoint_highlighted = new AtomicReference<Color>(Color.RED);
    private AtomicReference<Color> majorAlarmColorGradientEndPoint_highlighted = new AtomicReference<Color>(Color.RED);

    AtomicReference<Paint> normalStatusActiveColor_lowlighted = new AtomicReference<>(Color.WHITE);
    AtomicReference<Paint> minorAlarmActiveColor_lowlighted = new AtomicReference<>(Color.YELLOW);
    AtomicReference<Paint> majorAlarmActiveColor_lowlighted = new AtomicReference<>(Color.RED);

    AtomicReference<Paint> normalStatusActiveColor_highlighted = new AtomicReference<>(Color.WHITE);
    AtomicReference<Paint> majorAlarmActiveColor_highlighted = new AtomicReference<>(Color.YELLOW);
    AtomicReference<Paint> minorAlarmActiveColor_highlighted = new AtomicReference<>(Color.RED);

    private AtomicInteger needleWidth = new AtomicInteger(2);

    private AtomicReference<Color> needleColor = new AtomicReference<>(Color.BLACK);

    private AtomicBoolean isGradientEnabled = new AtomicBoolean(false);

    private AtomicBoolean isHighlightActiveRegionEnabled = new AtomicBoolean(true);

    private void runOnJavaFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(() -> runnable.run());
        }
    }

    public void setIsGradientEnabled(boolean isGradientEnabled) {
        runOnJavaFXThread(() -> {
            this.isGradientEnabled.set(isGradientEnabled);
            updateActiveColors();
        });
    }

    public void setIsHighlightActiveRegionEnabled(boolean isHighlightActiveRegionEnabled) {
        this.isHighlightActiveRegionEnabled.set(isHighlightActiveRegionEnabled);
    }

    private void updateActiveColors() {
        if (isGradientEnabled.get()) {
            if (linearMeterScale.isHorizontal()) {
                majorAlarmActiveColor_lowlighted.set(createVerticalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_lowlighted.get(), majorAlarmColorGradientEndPoint_lowlighted.get()));
                minorAlarmActiveColor_lowlighted.set(createVerticalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_lowlighted.get(), minorAlarmColorGradientEndPoint_lowlighted.get()));
                normalStatusActiveColor_lowlighted.set(createVerticalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted.get(), normalStatusColorGradientEndPoint_highlighted.get())); // The normal status region is never lowlighted.

                minorAlarmActiveColor_highlighted.set(createVerticalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_highlighted.get(), minorAlarmColorGradientEndPoint_highlighted.get()));
                majorAlarmActiveColor_highlighted.set(createVerticalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_highlighted.get(), majorAlarmColorGradientEndPoint_highlighted.get()));
                normalStatusActiveColor_highlighted.set(createVerticalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted.get(), normalStatusColorGradientEndPoint_highlighted.get()));
            } else {
                majorAlarmActiveColor_lowlighted.set(createHorizontalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_lowlighted.get(), majorAlarmColorGradientEndPoint_lowlighted.get()));
                minorAlarmActiveColor_lowlighted.set(createHorizontalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_lowlighted.get(), minorAlarmColorGradientEndPoint_lowlighted.get()));
                normalStatusActiveColor_lowlighted.set(createHorizontalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted.get(), normalStatusColorGradientEndPoint_highlighted.get())); // The normal status region is never lowlighted.

                minorAlarmActiveColor_highlighted.set(createHorizontalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_highlighted.get(), minorAlarmColorGradientEndPoint_highlighted.get()));
                majorAlarmActiveColor_highlighted.set(createHorizontalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_highlighted.get(), majorAlarmColorGradientEndPoint_highlighted.get()));
                normalStatusActiveColor_highlighted.set(createHorizontalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted.get(), normalStatusColorGradientEndPoint_highlighted.get()));
            }
        }
        else {
            normalStatusActiveColor_lowlighted.set(normalStatusColor_highlighted.get()); // The normal status region is never lowlighted.
            majorAlarmActiveColor_lowlighted.set(majorAlarmColor_lowlighted.get());
            minorAlarmActiveColor_lowlighted.set(minorAlarmColor_lowlighted.get());

            normalStatusActiveColor_highlighted.set(normalStatusColor_highlighted.get());
            minorAlarmActiveColor_highlighted.set(minorAlarmColor_highlighted.get());
            majorAlarmActiveColor_highlighted.set(majorAlarmColor_highlighted.get());
        }
    }

    private Color computeGradientStartPoint(Color color) {
        float[] hsbValues = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newSaturationValue;
        if (hsbValues[1] < 0.01) {
            newSaturationValue = hsbValues[1]; // To prevent rounding errors leading to the color changing.
        }
        else {
            newSaturationValue = (float) 1.0 - ((float) 1.0 - hsbValues[1]) * (float) 0.8;
        }
        float newBrightnessValue = hsbValues[2] * (float) 0.8;
        Color gradientEndPoint_withoutAlpha = Color.getHSBColor(hsbValues[0],
                                                                newSaturationValue,
                                                                newBrightnessValue);
        Color gradientEndPoint = new Color(gradientEndPoint_withoutAlpha.getRed(),
                                           gradientEndPoint_withoutAlpha.getGreen(),
                                           gradientEndPoint_withoutAlpha.getBlue(),
                                           color.getAlpha());
        return gradientEndPoint;
    }

    private Color computeGradientEndPoint(Color color) {
        float[] hsbValues = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newSaturationValue;
        if (hsbValues[1] < 0.01) {
            newSaturationValue = hsbValues[1]; // To prevent rounding errors leading to the color changing.
        }
        else {
            newSaturationValue = hsbValues[1] * (float) 0.85;
        }
        float newBrightnessValue = 1 - (1 - hsbValues[2]) * (float) 0.5;
        Color gradientEndPoint_withoutAlpha = Color.getHSBColor(hsbValues[0],
                                                                newSaturationValue,
                                                                newBrightnessValue);
        Color gradientEndPoint = new Color(gradientEndPoint_withoutAlpha.getRed(),
                                           gradientEndPoint_withoutAlpha.getGreen(),
                                           gradientEndPoint_withoutAlpha.getBlue(),
                                           color.getAlpha());
        return gradientEndPoint;
    }

    private Color computeLowlightedColor(Color color) {
        float[] hsbValues = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newSaturationValue = hsbValues[1] * 0.1f;
        Color lowlightedColor_withoutAlpha = Color.getHSBColor(hsbValues[0],
                                                               newSaturationValue,
                                                               hsbValues[2]);
        Color lowlightedColor = new Color(lowlightedColor_withoutAlpha.getRed(),
                                          lowlightedColor_withoutAlpha.getGreen(),
                                          lowlightedColor_withoutAlpha.getBlue(),
                                          color.getAlpha());
        return lowlightedColor;
    }

    public void setNormalStatusColor(Color normalStatusColor) {
            this.normalStatusColor_lowlighted.set(computeLowlightedColor(normalStatusColor));
            this.normalStatusColor_highlighted.set(normalStatusColor);

            this.normalStatusColorGradientStartPoint_lowlighted.set(computeGradientStartPoint(normalStatusColor_lowlighted.get()));
            this.normalStatusColorGradientEndPoint_lowlighted.set(computeGradientEndPoint(normalStatusColor_lowlighted.get()));

            this.normalStatusColorGradientStartPoint_highlighted.set(computeGradientStartPoint(normalStatusColor_highlighted.get()));
            this.normalStatusColorGradientEndPoint_highlighted.set(computeGradientEndPoint(normalStatusColor_highlighted.get()));

            updateActiveColors();
    }

    public void setMinorAlarmColor(Color minorAlarmColor) {
            this.minorAlarmColor_lowlighted.set(computeLowlightedColor(minorAlarmColor));
            this.minorAlarmColor_highlighted.set(minorAlarmColor);

            this.minorAlarmColorGradientStartPoint_lowlighted.set(computeGradientStartPoint(minorAlarmColor_lowlighted.get()));
            this.minorAlarmColorGradientEndPoint_lowlighted.set(computeGradientEndPoint(minorAlarmColor_lowlighted.get()));

            this.minorAlarmColorGradientStartPoint_highlighted.set(computeGradientStartPoint(minorAlarmColor_highlighted.get()));
            this.minorAlarmColorGradientEndPoint_highlighted.set(computeGradientEndPoint(minorAlarmColor_highlighted.get()));

            updateActiveColors();
    }

    public void setMajorAlarmColor(Color majorAlarmColor) {
        runOnJavaFXThread(() -> {
            this.majorAlarmColor_lowlighted.set(computeLowlightedColor(majorAlarmColor));
            this.majorAlarmColor_highlighted.set(majorAlarmColor);

            this.majorAlarmColorGradientStartPoint_lowlighted.set(computeGradientStartPoint(majorAlarmColor_lowlighted.get()));
            this.majorAlarmColorGradientEndPoint_lowlighted.set(computeGradientEndPoint(majorAlarmColor_lowlighted.get()));

            this.majorAlarmColorGradientStartPoint_highlighted.set(computeGradientStartPoint(majorAlarmColor_highlighted.get()));
            this.majorAlarmColorGradientEndPoint_highlighted.set(computeGradientEndPoint(majorAlarmColor_highlighted.get()));

            updateActiveColors();
        });
    }

    public void setNeedleWidth(int needleWidth) {
        this.needleWidth.set(needleWidth);
    }

    public void setNeedleColor(Color needleColor) {
        this.needleColor.set(needleColor);
    }

    public void setShowUnits(boolean newValue) {
        showUnits.set(newValue);
        updateMeterBackground();
        redrawIndicator(currentValue.get(), currentWarning.get());
    }

    public void setUnits(String newValue) {

        if (!units.equals(newValue)) {
            units.set(newValue);
            updateMeterBackground();
            redrawIndicator(currentValue.get(), currentWarning.get());
        }
    }

    public void setShowLimits(boolean newValue) {
        showLimits.set(newValue);
        updateMeterBackground();
        determineWarning();
        redrawIndicator(currentValue.get(), currentWarning.get());
    }

    public void setRange(double minimum, double maximum, boolean validRange) {
        this.validRange.set(validRange);
        linearMeterScale.setValueRange(minimum, maximum);

        updateMeterBackground();
        redrawIndicator(currentValue.get(), currentWarning.get());
    }

    public void setMinMaxTolerance(double minMaxTolerance) {
            this.minMaxTolerance.set(minMaxTolerance);
            determineWarning();
            redrawIndicator(currentValue.get(), currentWarning.get());
    }

    public double getLoLo() {
        return loLo.get();
    }

    public void setLoLo(double loLo) {
        this.loLo.set(loLo);
        layout();
        updateMeterBackground();
    }

    public double getLow() {
        return low.get();
    }

    public void setLow(double low) {
        runOnJavaFXThread(() -> {
            this.low.set(low);
            layout();
            updateMeterBackground();
        });
    }

    public double getHigh() {
        return high.get();
    }

    public void setHigh(double high) {
        this.high.set(high);
        layout();
        updateMeterBackground();
    }

    public double getHiHi() {
        return hiHi.get();
    }

    public void setHiHi(double hiHi) {
        this.hiHi.set(hiHi);
        layout();
        updateMeterBackground();
    }

    private AtomicReference<Color> knobColor = new AtomicReference<Color>(new Color(0, 0, 0, 255));

    public void setKnobColor(Color knobColor) {
        this.knobColor.set(knobColor);
        requestLayout();
    }

    private AtomicInteger knobSize = new AtomicInteger(1);

    public void setKnobSize(int knobSize) {
            this.knobSize.set(knobSize);
            requestLayout();
    }

    private BufferedImage meter_background = null;

    /** Redraw on UI thread by adding needle to 'meter_background' */
    private void redrawIndicator(double value, WARNING warning) {
        if (meter_background != null) {
            if (meter_background.getType() != BufferedImage.TYPE_INT_ARGB) {
                throw new IllegalPathStateException("Need TYPE_INT_ARGB for direct buffer access, not " + meter_background.getType());
            }

            BufferedImage combined = new BufferedImage(linearMeterScale.getBounds().width, linearMeterScale.getBounds().height, BufferedImage.TYPE_INT_ARGB);
            int[] src = ((DataBufferInt) meter_background.getRaster().getDataBuffer()).getData();
            int[] dest = ((DataBufferInt) combined.getRaster().getDataBuffer()).getData();
            System.arraycopy(src, 0, dest, 0, linearMeterScale.getBounds().width * linearMeterScale.getBounds().height);

            // Add needle & label
            Graphics2D gc = combined.createGraphics();

            drawValue(gc, value);
            drawWarning(gc, warning);
            if (showUnits.get()) {
                drawUnit(gc);
            }

            // Convert to JFX image and show

            WritableImage awt_jfx_convert_buffer = new WritableImage(linearMeterScale.getBounds().width, linearMeterScale.getBounds().height);

            awt_jfx_convert_buffer.getPixelWriter().setPixels(0, 0, linearMeterScale.getBounds().width, linearMeterScale.getBounds().height, PixelFormat.getIntArgbInstance(), dest, 0, linearMeterScale.getBounds().width);
            runOnJavaFXThread(() -> { draw(awt_jfx_convert_buffer); });
            logger.log(Level.FINE, "Redraw meter");
        }
    }

    private void draw(WritableImage awt_jfx_convert_buffer) {
        setImage(awt_jfx_convert_buffer);
    }

    /** Call to update size of meter
     *
     *  @param width
     *  @param height
     */
    public void setSize(int width, int height) {
        linearMeterScale.setBounds(0, 0, width, height);
        layout();
        updateActiveColors();
        requestLayout();
    }

    /** @param color Foreground (labels, tick marks) color */
    public void setForeground(javafx.scene.paint.Color color) {
        foreground.set(GraphicsUtils.convert(color));
        linearMeterScale.setColor(color);
    }

    /** @param color Background color */
    public void setBackground(javafx.scene.paint.Color color)
    {
        background.set(GraphicsUtils.convert(color));
    }

    /** @param font Label font */
    public void setFont(javafx.scene.text.Font font)
    {
        runOnJavaFXThread(() -> {
            linearMeterScale.setScaleFont(font);
            this.font = GraphicsUtils.convert(font);
        });
    }

    private AtomicBoolean showWarnings = new AtomicBoolean(true);

    public void setShowWarnings(boolean showWarnings) {
        this.showWarnings.set(showWarnings);
    }

    private AtomicBoolean lag = new AtomicBoolean(false);
    private AtomicBoolean isValueWaitingToBeDrawn = new AtomicBoolean(false);
    private AtomicReference<Double> valueWaitingToBeDrawn = new AtomicReference<>(Double.NaN);
    /** @param newValue Current value */
    public void setCurrentValue(double newValue)
    {
        valueWaitingToBeDrawn.set(newValue);

        if (isValueWaitingToBeDrawn.get()) {
            lag.set(true);
        }
        else {
            isValueWaitingToBeDrawn.set(true);

            drawNewValue(valueWaitingToBeDrawn.get());
            isValueWaitingToBeDrawn.set(false);
            lag.set(false);
        }
    }

    private void drawNewValue(double newValue) {
        double oldValue = currentValue.get();
        currentValue.set(newValue);

        if (newValue > linearMeterScale.getValueRange().getHigh() && newValue <= linearMeterScale.getValueRange().getHigh() + minMaxTolerance.get()) {
            newValue = linearMeterScale.getValueRange().getHigh();
        }
        if (newValue < linearMeterScale.getValueRange().getLow() && newValue >= linearMeterScale.getValueRange().getLow() - minMaxTolerance.get()) {
            newValue = linearMeterScale.getValueRange().getLow();
        }

        if (oldValue != newValue) {
            if (!Double.isNaN(newValue)){
                int newIndicatorPosition;
                if (linearMeterScale.isHorizontal()) {
                    newIndicatorPosition = (int) (marginLeft + pixelsPerScaleUnit * (newValue - linearMeterScale.getValueRange().getLow()));
                }
                else {
                    newIndicatorPosition = (int) (linearMeterScale.getBounds().height - marginBelow - pixelsPerScaleUnit * (newValue - linearMeterScale.getValueRange().getLow()));
                }
                WARNING newWarning = determineWarning();
                if (currentIndicatorPosition == null || currentIndicatorPosition != newIndicatorPosition || currentWarning.get() != newWarning) {
                    redrawIndicator(newValue, newWarning);
                }
            }
            else if (!Double.isNaN(oldValue)) {
                redrawIndicator(newValue, determineWarning());
            }
        }
    }

    private WARNING determineWarning() {
        if (!showWarnings.get()) {
            return WARNING.NONE;
        }
        else if (lag.get()) {
            return WARNING.LAG;
        }
        else if (showUnits.get() && units.get().equals("")) {
            return WARNING.NO_UNIT;
        }
        else if (!validRange.get()) {
            return WARNING.MIN_AND_MAX_NOT_DEFINED;
        }
        else if (currentValue.get() < linearMeterScale.getValueRange().getLow() - minMaxTolerance.get()) {
            return WARNING.VALUE_LESS_THAN_MIN;
        }
        else if (currentValue.get() > linearMeterScale.getValueRange().getHigh() + minMaxTolerance.get()) {
            return WARNING.VALUE_GREATER_THAN_MAX;
        }
        else {
            return WARNING.NONE;
        }
    }

    private void drawWarning(Graphics2D gc, WARNING warning) {
        if (warning != WARNING.NONE) {
            String warningText = "";
            if (warning == WARNING.VALUE_LESS_THAN_MIN) {
                warningText = "VALUE < MIN";

            }
            else if (warning == WARNING.VALUE_GREATER_THAN_MAX) {
                warningText = "VALUE > MAX";

            }
            else if (warning == WARNING.NO_UNIT) {
                warningText = "NO UNIT DEFINED";
            }
            else if (warning == WARNING.MIN_AND_MAX_NOT_DEFINED) {
                warningText = "MIN AND MAX ARE NOT SET";

            }
            else if (warning == WARNING.LAG) {
                warningText = "LAG";
            }

            drawWarningText(gc, warningText);
            if (currentWarning.get() != warning) {
                logger.log(Level.WARNING, warningText + " on Linear Meter!");
            }
        }
        currentWarning.set(warning);
    }

    /** @param visible Whether the scale must be displayed or not. */
    public void setScaleVisible (boolean visible)
    {
        runOnJavaFXThread(() -> {
            linearMeterScale.setVisible(visible);
            updateMeterBackground();
        });
    }

    /** Request a complete redraw with new layout */
    private void requestLayout()
    {
        updateMeterBackground();
        redrawIndicator(currentValue.get(), currentWarning.get());
    }

    private void computeLayout()
    {
        logger.log(Level.FINE, "computeLayout");
        layout();

        if (linearMeterScale.isHorizontal()) {
            linearMeterScale.configure(
                    loLoRectangle.x,
                    lowRectangle.y + loLoRectangle.height,
                    pixelsPerScaleUnit);
        } else {
            linearMeterScale.configure(
                    linearMeterScale.getBounds().width - marginRight,
                    linearMeterScale.getBounds().height - marginBelow,
                    pixelsPerScaleUnit);
        }
    }

    /** Draw meter background (scale) into image buffer
     *  @return Latest image, must be of type BufferedImage.TYPE_INT_ARGB
     */
    private void updateMeterBackground()
    {
        int width = linearMeterScale.getBounds().width;
        int height = linearMeterScale.getBounds().height;

        if (width <= 0 || height <= 0){
            return;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = image.createGraphics();

        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        linearMeterScale.computeTicks(gc);
        computeLayout();

        gc.setBackground(background.get());
        gc.clearRect(0, 0, width, height);

        linearMeterScale.paint(gc, new Rectangle(0,0,0,0));
        paintMeter(gc);

        meter_background = image;
    }

    private void paintMeter(Graphics2D graphics) {
        Color color = graphics.getColor();
        if (showLimits.get()) {
            if (isHighlightActiveRegionEnabled.get()) {
                paintRectangle(graphics, normalRectangle, normalStatusActiveColor_lowlighted.get());
                paintRectangle(graphics, lowRectangle, minorAlarmActiveColor_lowlighted.get());
                paintRectangle(graphics, highRectangle, minorAlarmActiveColor_lowlighted.get());
                paintRectangle(graphics, loLoRectangle, majorAlarmActiveColor_lowlighted.get());
                paintRectangle(graphics, hiHiRectangle, majorAlarmActiveColor_lowlighted.get());
            }
            else {
                paintRectangle(graphics, normalRectangle, normalStatusActiveColor_highlighted.get());
                paintRectangle(graphics, lowRectangle, minorAlarmActiveColor_highlighted.get());
                paintRectangle(graphics, highRectangle, minorAlarmActiveColor_highlighted.get());
                paintRectangle(graphics, loLoRectangle, majorAlarmActiveColor_highlighted.get());
                paintRectangle(graphics, hiHiRectangle, majorAlarmActiveColor_highlighted.get());
            }
        }
        else {
            paintRectangle(graphics,
                           new Rectangle(marginLeft,
                                         marginAbove,
                                         linearMeterScale.getBounds().width - marginLeft - marginRight,
                                         linearMeterScale.getBounds().height - marginAbove - marginBelow),
                           normalStatusActiveColor_lowlighted.get());
        }
        graphics.setColor(color);
    }

    private GradientPaint createHorizontalGradientPaint(Rectangle rectangle,
                                                        Color colorGradientStartPoint,
                                                        Color colorGradientEndPoint) {
        GradientPaint gradientPaint = new GradientPaint(rectangle.x, rectangle.y, colorGradientStartPoint,
                                                        rectangle.x + rectangle.width / 2, rectangle.y, colorGradientEndPoint,
                                                        true);
        return gradientPaint;
    }

    private GradientPaint createVerticalGradientPaint(Rectangle rectangle,
                                                      Color colorGradientStartPoint,
                                                      Color colorGradientEndPoint) {
        GradientPaint gradientPaint = new GradientPaint(rectangle.x, rectangle.y, colorGradientStartPoint,
                                                        rectangle.x, rectangle.y + rectangle.height / 2, colorGradientEndPoint,
                                                        true);
        return gradientPaint;
    }

    Integer currentIndicatorPosition;
    /** Draw needle and label for current value */
    private void drawValue(Graphics2D gc, double value) {

        if (Double.isNaN(value)) {
            currentIndicatorPosition = null;
        }
        else {
            Stroke oldStroke = gc.getStroke();
            Paint oldPaint = gc.getPaint();
            RenderingHints oldrenderingHints = gc.getRenderingHints();

            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (showLimits.get()) {
                if (isHighlightActiveRegionEnabled.get()) {
                    if (value <= loLo.get()) {
                        paintRectangle(gc, loLoRectangle, majorAlarmColor_highlighted.get());
                    }
                    else if (value >= hiHi.get()) {
                        paintRectangle(gc, hiHiRectangle, majorAlarmColor_highlighted.get());
                    }
                    else if (value <= low.get() && value > loLo.get()) {
                        paintRectangle(gc, lowRectangle, minorAlarmActiveColor_highlighted.get());
                    }
                    else if (value >= high.get() && value < hiHi.get()) {
                        paintRectangle(gc, highRectangle, minorAlarmActiveColor_highlighted.get());
                    }
                    else {
                        paintRectangle(gc, normalRectangle, normalStatusActiveColor_highlighted.get());
                    }
                }
            }

            if (linearMeterScale.isHorizontal()) {
                if (value >= linearMeterScale.getValueRange().getLow() && value <= linearMeterScale.getValueRange().getHigh()) {

                    currentIndicatorPosition = (int) (marginLeft + pixelsPerScaleUnit * (value - linearMeterScale.getValueRange().getLow()));

                    if (knobSize.get() > 0) {
                        int[] XVal = { currentIndicatorPosition - (int) Math.round((1.0 * knobSize.get()) / 4.0),
                                       currentIndicatorPosition + (int) Math.round((1.0 * knobSize.get()) / 4.0),
                                       currentIndicatorPosition };

                        int[] YVal = { 0, 0, marginAbove - 2 };

                        gc.setStroke(AxisPart.TICK_STROKE);
                        gc.setColor(knobColor.get());
                        gc.fillPolygon(XVal, YVal, 3);
                        gc.setColor(knobColor.get());
                        gc.drawPolygon(XVal, YVal, 3);
                    }

                    if (needleWidth.get() > 0) {
                        gc.setStroke(new BasicStroke((float) needleWidth.get()));
                        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        gc.setPaint(needleColor.get());

                        int y1 = marginAbove + needleWidth.get() / 2 + 1;
                        int y2 = linearMeterScale.getBounds().height - marginBelow - (needleWidth.get() - 1) / 2 - 1;

                        gc.drawLine(currentIndicatorPosition, y1, currentIndicatorPosition, y2);
                    }
                }
            } else {
                if (value >= linearMeterScale.getValueRange().getLow() && value <= linearMeterScale.getValueRange().getHigh()) {

                    currentIndicatorPosition = (int) (linearMeterScale.getBounds().height - marginBelow - pixelsPerScaleUnit * (value - linearMeterScale.getValueRange().getLow()));

                    if (knobSize.get() > 0) {
                        int[] YVal = { currentIndicatorPosition + (int) Math.round((1.0 * knobSize.get() / 4.0)),
                                       currentIndicatorPosition - (int) Math.round((1.0 * knobSize.get() / 4.0)),
                                       currentIndicatorPosition };

                        int[] XVal = { 0, 0, marginLeft - 2 };

                        gc.setStroke(AxisPart.TICK_STROKE);
                        gc.setColor(knobColor.get());
                        gc.fillPolygon(XVal, YVal, 3);
                        gc.setColor(knobColor.get());
                        gc.drawPolygon(XVal, YVal, 3);
                    }

                    if (needleWidth.get() > 0) {
                        gc.setStroke(new BasicStroke((float) needleWidth.get()));
                        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        gc.setPaint(needleColor.get());

                        int x1 = marginLeft + (needleWidth.get())/2 + 1;
                        int x2 = linearMeterScale.getBounds().width - marginRight - (needleWidth.get()+1)/2;

                        gc.drawLine(x1, currentIndicatorPosition, x2, currentIndicatorPosition);
                    }
                }
            }
            gc.setRenderingHints(oldrenderingHints);
            gc.setStroke(oldStroke);
            gc.setPaint(oldPaint);
        }
    }

    /** Should be invoked when meter no longer used to release resources */
    public void dispose()
    {
        // Release memory ASAP
        meter_background = null;
    }

    public void setHorizontal(boolean horizontal) {
        runOnJavaFXThread(() -> {
            linearMeterScale.setHorizontal(horizontal);
            redrawLinearMeterScale();
            updateMeterBackground();
            redrawIndicator(currentValue.get(), currentWarning.get());
        });
    }

    private void drawUnit(Graphics2D gc) {
        int center_x = marginLeft + (linearMeterScale.getBounds().width - marginLeft - marginRight) / 2;
        int center_y = linearMeterScale.getBounds().height;
        RenderingHints renderingHints = gc.getRenderingHints();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setFont(font);
        gc.setColor(Color.BLACK);
        FontMetrics fontMetrics = gc.getFontMetrics(gc.getFont());
        String stringToPrint = "[" + units + "]";
        int delta_x = fontMetrics.stringWidth(stringToPrint) / 2;
        int delta_y = fontMetrics.getMaxDescent();

        gc.drawString(stringToPrint,
                      center_x - delta_x,
                      center_y - delta_y);
        gc.setRenderingHints(renderingHints);
    }

    private void drawWarning_horizontal(Graphics2D gc, String warningText) {
        int center_x = marginLeft + (linearMeterScale.getBounds().width - marginLeft - marginRight) / 2;
        int center_y = marginAbove + (linearMeterScale.getBounds().height - marginAbove - marginBelow) / 2;
        gc.setFont(font);
        gc.setColor(Color.BLACK);
        FontMetrics fontMetrics = gc.getFontMetrics(gc.getFont());
        int delta_x = (warningTriangle.getWidth(null) + fontMetrics.stringWidth(warningText)) / 2;
        int delta_y = fontMetrics.getAscent() / 2;

        gc.drawImage(warningTriangle,
                     center_x - delta_x,
                     center_y + delta_y - warningTriangle.getHeight(null) / 2 - 3 * fontMetrics.getAscent() / 8,
                     null);
        gc.drawString(warningText,
                      center_x - delta_x + warningTriangle.getWidth(null) + 2,
                      center_y + delta_y);
    }

    private void drawWarningText(Graphics2D gc, String warningText) {
        RenderingHints oldRenderingHints = gc.getRenderingHints();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (linearMeterScale.isHorizontal()) {
            drawWarning_horizontal(gc, warningText);
        }
        else {
            drawWarning_vertical(gc, warningText);
        }
        gc.setRenderingHints(oldRenderingHints);
    }

    private void drawWarning_vertical(Graphics2D gc, String warningText) {

        gc.setFont(font);
        gc.setColor(Color.BLACK);
        FontMetrics fontMetrics = gc.getFontMetrics(gc.getFont());
        String[] warningText_split;
        if (fontMetrics.stringWidth(warningText) <= meterBreadth) {
            warningText_split = new String[] { warningText };
        } else {
            String[] warningText_splitByWhitespace = warningText.split("\\s+");
            if (Arrays.stream(warningText_splitByWhitespace).allMatch(subString -> fontMetrics.stringWidth(subString) <= meterBreadth)) {
                warningText_split = warningText_splitByWhitespace;
            }
            else {
                warningText_split = warningText.split(""); // Split on every character
            }
        }

        int center_x = marginLeft + (linearMeterScale.getBounds().width - marginLeft - marginRight) / 2;
        int center_y = marginAbove + (linearMeterScale.getBounds().height - marginAbove - marginBelow) / 2;
        int warningTriangleHeight = warningTriangle.getHeight(null);
        int fontSizeInPixels = fontMetrics.getHeight();
        int delta_y = (warningTriangleHeight + warningText_split.length * fontSizeInPixels) / 2;

        gc.drawImage(warningTriangle,
                     center_x - warningTriangle.getWidth(null) / 2,
                     center_y - delta_y,
                     null);

        for (int i = 0; i < warningText_split.length; i++) {
            gc.drawString(warningText_split[i],
                          center_x - fontMetrics.stringWidth(warningText_split[i]) / 2,
                          center_y - delta_y + warningTriangleHeight + (fontSizeInPixels + 4) * (i + 1));
        }
    }

    private void paintRectangle(Graphics2D gc, Rectangle rectangle, Paint paint){

        //Store old values of clip and color
        Shape oldClip = gc.getClip();
        Color oldColor = gc.getColor();

        //Paint rectangle with specified gradient
        gc.setClip(rectangle);
        gc.setPaint(paint);
        gc.fill(rectangle);
        gc.setClip(oldClip);

        //Draw border of rectangle.
        //TODO: can this be included in the paint?
        gc.setColor(foreground.get());
        gc.draw(rectangle);

        gc.setColor(oldColor);
    }

    private Rectangle loLoRectangle;
    private Rectangle lowRectangle;
    private Rectangle highRectangle;
    private Rectangle hiHiRectangle;
    private Rectangle normalRectangle;

    private int marginLeft, marginAbove, marginRight, marginBelow = 0;

    public double pixelsPerScaleUnit = 1.0;

    private int meterBreadth = 0;

    private void layout() {

        double displayedLoLo;
        double displayedLow;
        double displayedHiHi;
        double displayedHigh;

        double loLoValue = loLo.get();
        double lowValue = low.get();
        double highValue = high.get();
        double hiHiValue = hiHi.get();

        displayedLoLo = Double.isFinite(loLoValue) ? Math.max(loLoValue, linearMeterScale.getValueRange().getLow()) : linearMeterScale.getValueRange().getLow();
        displayedLow = Double.isFinite(lowValue) ? Math.max(Math.max(lowValue, linearMeterScale.getValueRange().getLow()), displayedLoLo) : linearMeterScale.getValueRange().getLow();

        displayedHiHi = Double.isFinite(highValue) ? Math.min(hiHiValue, linearMeterScale.getValueRange().getHigh()) : linearMeterScale.getValueRange().getHigh();
        displayedHigh = Double.isFinite(hiHiValue) ? Math.min(Math.min(highValue, linearMeterScale.getValueRange().getHigh()), displayedHiHi) : linearMeterScale.getValueRange().getHigh();

        FontMetrics fontMetrics = null;
        if (font != null) {
            Canvas canvas = new Canvas();
            fontMetrics = canvas.getFontMetrics(font);
        }

        if (linearMeterScale.isHorizontal()) {
            int knobSizeValue = knobSize.get();
            marginAbove = knobSizeValue >= 1 ? knobSizeValue + 2 : 0;
            if (linearMeterScale.isVisible() && fontMetrics != null) {
                var majorTicks = linearMeterScale.getTicks().getMajorTicks();
                if (majorTicks.size() >= 2) {
                    marginLeft = fontMetrics.stringWidth(majorTicks.get(0).getLabel()) / 2;
                    marginRight = fontMetrics.stringWidth(majorTicks.get(majorTicks.size() - 1).getLabel()) / 2;
                } else if (majorTicks.size() == 1) {
                    marginRight = marginLeft = fontMetrics.stringWidth(majorTicks.get(0).getLabel()) / 2;
                } else {
                    marginRight = 0;
                    marginLeft = 0;
                }
                marginBelow = (int) (0.5 * linearMeterScale.getTickLength() + 4 + fontMetrics.getAscent() + fontMetrics.getDescent());
            } else {
                marginLeft = 0;
                marginRight = 1;
                marginBelow = 1;
            }

            if (showUnits.get() && fontMetrics != null) {
                marginBelow += 1 + fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
            }

            pixelsPerScaleUnit = (linearMeterScale.getBounds().width - marginLeft - marginRight) / (linearMeterScale.getValueRange().getHigh() - linearMeterScale.getValueRange().getLow());
            meterBreadth = Math.round(linearMeterScale.getBounds().height - marginAbove - marginBelow);

            double x_loLoRectangle = marginLeft;
            double x_lowRectangle = marginLeft + pixelsPerScaleUnit * (displayedLoLo - linearMeterScale.getValueRange().getLow());
            double x_normalRectangle = marginLeft + pixelsPerScaleUnit * (displayedLow - linearMeterScale.getValueRange().getLow());
            double x_highRectangle = marginLeft + pixelsPerScaleUnit * (displayedHigh - linearMeterScale.getValueRange().getLow());
            double x_hiHiRectangle = marginLeft + pixelsPerScaleUnit * (displayedHiHi - linearMeterScale.getValueRange().getLow());

            loLoRectangle = new Rectangle((int) Math.round(x_loLoRectangle),
                                          marginAbove,
                                          (int) (Math.round(x_lowRectangle) - Math.round(x_loLoRectangle)),
                                          meterBreadth);

            lowRectangle = new Rectangle((int) Math.round(x_lowRectangle),
                                         marginAbove,
                                         (int) (Math.round(x_normalRectangle) - Math.round(x_lowRectangle)),
                                         meterBreadth);

            normalRectangle = new Rectangle((int) Math.round(x_normalRectangle),
                                            marginAbove,
                                            (int) (Math.round(x_highRectangle) - Math.round(x_normalRectangle)),
                                            meterBreadth);

            highRectangle = new Rectangle((int) Math.round(x_highRectangle),
                                          marginAbove,
                                          (int) (Math.round(x_hiHiRectangle) - Math.round(x_highRectangle)),
                                          meterBreadth);

            hiHiRectangle = new Rectangle((int) Math.round(x_hiHiRectangle),
                                          marginAbove,
                                          (int) (Math.round(pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedHiHi))),
                                          meterBreadth);
        }
        else {
            int knobSizeValue = knobSize.get();
            marginLeft = knobSizeValue >= 1 ? knobSizeValue + 2 : 0;
            if (linearMeterScale.isVisible() && fontMetrics != null) {
                int maxTickLabelWidth = 0;
                maxTickLabelWidth = 0;
                var majorTicks = linearMeterScale.getTicks().getMajorTicks();
                for (var majorTick : majorTicks) {
                    int labelStringWidth = fontMetrics.stringWidth(majorTick.getLabel());
                    maxTickLabelWidth = Math.max(maxTickLabelWidth, labelStringWidth);
                }
                marginRight = RTLinearMeter.this.linearMeterScale.getTickLength() + maxTickLabelWidth + 1;
                marginAbove = fontMetrics.getAscent() / 2 + 1;
                marginBelow = fontMetrics.getAscent() / 2 + 1;
            } else {
                marginRight = 1;
                marginAbove = 0;
                marginBelow = 1;
            }

            if (showUnits.get() && fontMetrics != null) {
                marginBelow += 1 + fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
            }

            pixelsPerScaleUnit = (linearMeterScale.getBounds().height - marginAbove - marginBelow) / (linearMeterScale.getValueRange().getHigh() - linearMeterScale.getValueRange().getLow());
            meterBreadth = Math.round(linearMeterScale.getBounds().width - marginLeft - marginRight);

            double y_loLoRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedLoLo);
            double y_lowRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedLow);
            double y_normalRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedHigh);
            double y_highRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedHiHi);

            loLoRectangle = new Rectangle(marginLeft,
                                          (int) Math.round(y_loLoRectangle),
                                          meterBreadth,
                                          (int) (Math.round(pixelsPerScaleUnit * (displayedLoLo - linearMeterScale.getValueRange().getLow()) )));

            lowRectangle = new Rectangle(marginLeft,
                                         (int) Math.round(y_lowRectangle),
                                         meterBreadth,
                                         (int) (Math.round(y_loLoRectangle) - Math.round(y_lowRectangle)));

            normalRectangle =  new Rectangle(marginLeft,
                                             (int) Math.round(y_normalRectangle),
                                             meterBreadth,
                                             (int) (Math.round(y_lowRectangle) - Math.round(y_normalRectangle)));

            highRectangle = new Rectangle(marginLeft,
                                          (int) Math.round(y_highRectangle),
                                          meterBreadth,
                                          (int) (Math.round(y_normalRectangle) - Math.round(y_highRectangle)));

            hiHiRectangle = new Rectangle(marginLeft,
                                          marginAbove,
                                          meterBreadth,
                                          (int) Math.round(y_highRectangle) - marginAbove);
        }
    }
}
