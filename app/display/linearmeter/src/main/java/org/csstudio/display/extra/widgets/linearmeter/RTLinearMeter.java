
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
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
 * @version 1.2
 *
 * Version 1.0 implemented by Fredrik Söderberg.
 * Version 1.1 (some fixes and improvements) by Abraham Wolk.
 * Version 1.2 ("Bar" mode in addition to "Needle" mode, more fine-grained concurrency, and some improvements) by Abraham Wolk.
 *
 * The Linear Meter graphics design is by Dirk Nordt and Fredrik Söderberg.
 *
 */

@SuppressWarnings("nls")
public class RTLinearMeter extends ImageView
{
    // Note: All methods must ensure thread-safety by acquiring
    // readWriteLock.readLock() when reading fields, and by acquiring
    // readWriteLock.writeLock() when writing to one or more fields.
    //
    // The helper functions
    //
    // - void withReadLock(Runnable runnable)
    // - <T> T withReadLock(Supplier<T> supplier)
    // - void withWriteLock(Runnable runnable)
    //
    // have been implemented to facilitate the acquiring and releasing
    // of locks. When a read-lock is held, care must be taken not
    // to try to acquire the write-lock, since it leads to a deadlock.

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
            validRange = true;
        }
        else {
            validRange = false;
            min = 0.0;
            max = 100.0;
        }

        linearMeterScale = new LinearMeterScale(plot_part_listener,
                                                width,
                                                height,
                                                isHorizontal,
                                                min,
                                                max);

        this.minMaxTolerance = minMaxTolerance;
        this.loLo = loLo;
        this.low = low;
        this.high = high;
        this.hiHi = hiHi;
        this.showUnits = showUnits;
        this.showLimits = showLimits;
        this.showWarnings = showWarnings;

        layout();

        this.currentValue = initialValue;
        this.isGradientEnabled = isGradientEnabled;
        this.isHighlightActiveRegionEnabled = isHighlightActiveRegionEnabled;

        this.needleWidth = needleWidth;
        this.needleColor = needleColor;
        this.knobSize = knobSize;
        this.knobColor = knobColor;

        setNormalStatusColor(normalStatusColor);
        setMinorAlarmColor(minorAlarmColor);
        setMajorAlarmColor(majorAlarmColor);

        requestLayout();
    }

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected void withReadLock(Runnable runnable) {
        readWriteLock.readLock().lock();
        try {
            runnable.run();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
    protected <T> T withReadLock(Supplier<T> supplier) {
        readWriteLock.readLock().lock();
        try {
            return supplier.get();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
    protected void withWriteLock(Runnable runnable) {
        readWriteLock.writeLock().lock();
        try {
            runnable.run();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private boolean showUnits = true;
    private String units = "";
    private boolean showLimits = true;

    private double loLo = 0.;
    private double low =  .0;
    private double high = 100.;
    private double hiHi = 100.;

    private static Image warningTriangle = null;

    public void redrawLinearMeterScale() {
        withReadLock(() -> {
            boolean isHorizontal = linearMeterScale.isHorizontal();
            linearMeterScale = new LinearMeterScale(plot_part_listener,
                    linearMeterScale.getBounds().width,
                    linearMeterScale.getBounds().height,
                    linearMeterScale.isHorizontal(),
                    linearMeterScale.getValueRange().getLow(),
                    linearMeterScale.getValueRange().getHigh());
            linearMeterScale.setHorizontal(isHorizontal);
        });
    }

    private enum WARNING {
        NONE(""),
        VALUE_LESS_THAN_MIN("Value < Min"),
        VALUE_GREATER_THAN_MAX("Value > Max"),
        MIN_AND_MAX_NOT_DEFINED("Min and max are not set"),
        LAG("Lag");

        private final String displayName;

        private WARNING(String displayName) {
            this.displayName = displayName;
        };

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    /** Colors */
    private Color foreground = Color.BLACK;
    private Color background = Color.WHITE;

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

    private boolean validRange = false;
    private double minMaxTolerance = 0.0;

    public boolean getValidRange() {
        return withReadLock(() -> {
            return validRange;
        });
    }

    /** Optional scale of this linear meter */
    public LinearMeterScale linearMeterScale;

    /** Value to display */
    private double currentValue = Double.NaN;

    private Color normalStatusColor_lowlighted = Color.LIGHT_GRAY;
    private Color normalStatusColorGradientStartPoint_lowlighted = Color.LIGHT_GRAY;
    private Color normalStatusColorGradientEndPoint_lowlighted = Color.LIGHT_GRAY;
    private Color normalStatusColor_highlighted = Color.LIGHT_GRAY;
    private Color normalStatusColorGradientStartPoint_highlighted = Color.LIGHT_GRAY;
    private Color normalStatusColorGradientEndPoint_highlighted = Color.LIGHT_GRAY;

    private Color minorAlarmColor_lowlighted = Color.ORANGE;
    private Color minorAlarmColor_highlighted = Color.ORANGE;
    private Color minorAlarmColorGradientStartPoint_lowlighted = Color.ORANGE;
    private Color minorAlarmColorGradientEndPoint_lowlighted = Color.ORANGE;
    private Color minorAlarmColorGradientStartPoint_highlighted = Color.ORANGE;
    private Color minorAlarmColorGradientEndPoint_highlighted = Color.ORANGE;

    private Color majorAlarmColor_lowlighted = Color.RED;
    private Color majorAlarmColor_highlighted = Color.RED;
    private Color majorAlarmColorGradientStartPoint_lowlighted = Color.RED;
    private Color majorAlarmColorGradientEndPoint_lowlighted = Color.RED;
    private Color majorAlarmColorGradientStartPoint_highlighted = Color.RED;
    private Color majorAlarmColorGradientEndPoint_highlighted = Color.RED;

    Paint normalStatusActiveColor_lowlighted = Color.WHITE;
    Paint minorAlarmActiveColor_lowlighted = Color.YELLOW;
    Paint majorAlarmActiveColor_lowlighted = Color.RED;

    Paint normalStatusActiveColor_highlighted = Color.WHITE;
    Paint majorAlarmActiveColor_highlighted = Color.YELLOW;
    Paint minorAlarmActiveColor_highlighted = Color.RED;

    private int needleWidth = 2;

    private Color needleColor = Color.BLACK;

    private boolean isGradientEnabled = false;

    private boolean isHighlightActiveRegionEnabled = true;

    enum DisplayMode {
        NEEDLE,
        BAR
    }

    public void setDisplayMode(DisplayMode newDisplayMode) {
        withWriteLock(() -> {
            this.displayMode = newDisplayMode;
        });
    }

    private DisplayMode displayMode = DisplayMode.NEEDLE;

    private void runOnJavaFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        }
        else {
            Platform.runLater(() -> runnable.run());
        }
    }

    public void setIsGradientEnabled(boolean isGradientEnabled) {
        withWriteLock(() -> {
            this.isGradientEnabled = isGradientEnabled;
            updateActiveColors();
        });
    }

    public void setIsHighlightActiveRegionEnabled(boolean isHighlightActiveRegionEnabled) {
        withWriteLock(() -> {
            this.isHighlightActiveRegionEnabled = isHighlightActiveRegionEnabled;
        });
    }

    private void updateActiveColors() {
        withWriteLock(() -> {
            if (isGradientEnabled) {
                if (linearMeterScale.isHorizontal()) {
                    majorAlarmActiveColor_lowlighted = createVerticalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_lowlighted, majorAlarmColorGradientEndPoint_lowlighted);
                    minorAlarmActiveColor_lowlighted = createVerticalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_lowlighted, minorAlarmColorGradientEndPoint_lowlighted);
                    normalStatusActiveColor_lowlighted = createVerticalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted, normalStatusColorGradientEndPoint_highlighted); // The normal status region is never lowlighted.

                    minorAlarmActiveColor_highlighted = createVerticalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_highlighted, minorAlarmColorGradientEndPoint_highlighted);
                    majorAlarmActiveColor_highlighted = createVerticalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_highlighted, majorAlarmColorGradientEndPoint_highlighted);
                    normalStatusActiveColor_highlighted = createVerticalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted, normalStatusColorGradientEndPoint_highlighted);
                } else {
                    majorAlarmActiveColor_lowlighted = createHorizontalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_lowlighted, majorAlarmColorGradientEndPoint_lowlighted);
                    minorAlarmActiveColor_lowlighted = createHorizontalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_lowlighted, minorAlarmColorGradientEndPoint_lowlighted);
                    normalStatusActiveColor_lowlighted = createHorizontalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted, normalStatusColorGradientEndPoint_highlighted); // The normal status region is never lowlighted.

                    minorAlarmActiveColor_highlighted = createHorizontalGradientPaint(lowRectangle, minorAlarmColorGradientStartPoint_highlighted, minorAlarmColorGradientEndPoint_highlighted);
                    majorAlarmActiveColor_highlighted = createHorizontalGradientPaint(loLoRectangle, majorAlarmColorGradientStartPoint_highlighted, majorAlarmColorGradientEndPoint_highlighted);
                    normalStatusActiveColor_highlighted = createHorizontalGradientPaint(normalRectangle, normalStatusColorGradientStartPoint_highlighted, normalStatusColorGradientEndPoint_highlighted);
                }
            }
            else {
                normalStatusActiveColor_lowlighted = normalStatusColor_highlighted; // The normal status region is never lowlighted.
                majorAlarmActiveColor_lowlighted = majorAlarmColor_lowlighted;
                minorAlarmActiveColor_lowlighted = minorAlarmColor_lowlighted;

                normalStatusActiveColor_highlighted = normalStatusColor_highlighted;
                minorAlarmActiveColor_highlighted = minorAlarmColor_highlighted;
                majorAlarmActiveColor_highlighted = majorAlarmColor_highlighted;
            }
        });
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
        withWriteLock(() -> {
            this.normalStatusColor_lowlighted = computeLowlightedColor(normalStatusColor);
            this.normalStatusColor_highlighted = normalStatusColor;

            this.normalStatusColorGradientStartPoint_lowlighted = computeGradientStartPoint(normalStatusColor_lowlighted);
            this.normalStatusColorGradientEndPoint_lowlighted = computeGradientEndPoint(normalStatusColor_lowlighted);

            this.normalStatusColorGradientStartPoint_highlighted = computeGradientStartPoint(normalStatusColor_highlighted);
            this.normalStatusColorGradientEndPoint_highlighted = computeGradientEndPoint(normalStatusColor_highlighted);

            updateActiveColors();
        });
    }

    public void setMinorAlarmColor(Color minorAlarmColor) {
        withWriteLock(() -> {
            this.minorAlarmColor_lowlighted = computeLowlightedColor(minorAlarmColor);
            this.minorAlarmColor_highlighted = minorAlarmColor;

            this.minorAlarmColorGradientStartPoint_lowlighted = computeGradientStartPoint(minorAlarmColor_lowlighted);
            this.minorAlarmColorGradientEndPoint_lowlighted = computeGradientEndPoint(minorAlarmColor_lowlighted);

            this.minorAlarmColorGradientStartPoint_highlighted = computeGradientStartPoint(minorAlarmColor_highlighted);
            this.minorAlarmColorGradientEndPoint_highlighted = computeGradientEndPoint(minorAlarmColor_highlighted);

            updateActiveColors();
        });
    }

    public void setMajorAlarmColor(Color majorAlarmColor) {
        withWriteLock(() -> {
            this.majorAlarmColor_lowlighted = computeLowlightedColor(majorAlarmColor);
            this.majorAlarmColor_highlighted = majorAlarmColor;

            this.majorAlarmColorGradientStartPoint_lowlighted = computeGradientStartPoint(majorAlarmColor_lowlighted);
            this.majorAlarmColorGradientEndPoint_lowlighted = computeGradientEndPoint(majorAlarmColor_lowlighted);

            this.majorAlarmColorGradientStartPoint_highlighted = computeGradientStartPoint(majorAlarmColor_highlighted);
            this.majorAlarmColorGradientEndPoint_highlighted = computeGradientEndPoint(majorAlarmColor_highlighted);

            updateActiveColors();
        });
    }

    public void setNeedleWidth(int needleWidth) {
        withWriteLock(() -> {
            this.needleWidth = needleWidth;
        });
    }

    public void setNeedleColor(Color needleColor) {
        withWriteLock(() -> {
            this.needleColor = needleColor;
        });
    }

    public void setShowUnits(boolean newValue) {
        withWriteLock(() -> {
            showUnits = newValue;
            redraw();
        });
    }

    public void setUnits(String newValue) {
        withWriteLock(() -> {
            if (!units.equals(newValue)) {
                units = newValue;
                redraw();
            }
        });
    }

    private void logNewWarningIfDifferent(WARNING oldWarning, WARNING newWarning) {
        if (oldWarning != newWarning) {
            logger.log(Level.WARNING, newWarning.toString() + " on Linear Meter!");
        }
    }
    public void setShowLimits(boolean newValue) {
        withWriteLock(() -> {
            WARNING oldWarning = determineWarning();
            showLimits = newValue;
            WARNING newWarning = determineWarning();
            logNewWarningIfDifferent(oldWarning, newWarning);
            redraw();
        });
    }

    public void setRange(double minimum, double maximum, boolean validRange) {
        withWriteLock(() -> {
            this.validRange = validRange;
            linearMeterScale.setValueRange(minimum, maximum);
        });
        redraw();
    }

    public void setMinMaxTolerance(double minMaxTolerance) {
        withWriteLock(() -> {
            WARNING oldWarning = determineWarning();
            this.minMaxTolerance = minMaxTolerance;
            WARNING newWarning = determineWarning();
            logNewWarningIfDifferent(oldWarning, newWarning);
            redrawIndicator(currentValue, newWarning);
        });
    }

    public double getLoLo() {
        return withReadLock(() -> {
            return loLo;
        });
    }

    public void setLoLo(double loLo) {
        withWriteLock(() -> {
            this.loLo = loLo;
            layout();
            updateMeterBackground();
        });
    }

    public double getLow() {
        return withReadLock(() -> {
            return low;
        });
    }

    public void setLow(double low) {
        withWriteLock(() -> {
            this.low = low;
            layout();
            updateMeterBackground();
        });
    }

    public double getHigh() {
        return withReadLock(() -> {
            return high;
        });
    }

    public void setHigh(double high) {
        withWriteLock(() -> {
            this.high = high;
            layout();
            updateMeterBackground();
        });
    }

    public double getHiHi() {
        return withReadLock(() -> {
            return hiHi;
        });
    }

    public void setHiHi(double hiHi) {
        withWriteLock(() -> {
            this.hiHi = hiHi;
            layout();
            updateMeterBackground();
        });
    }

    private Color knobColor = new Color(0, 0, 0, 255);

    public void setKnobColor(Color knobColor) {
        withWriteLock(() -> {
            this.knobColor = knobColor;
            requestLayout();
        });
    }

    private int knobSize = 1;

    public void setKnobSize(int knobSize) {
        withWriteLock(() -> {
            this.knobSize = knobSize;
            requestLayout();
        });
    }

    private void redraw() {
        withWriteLock(() -> {
            updateMeterBackground();
            redrawIndicator(currentValue, determineWarning());
        });
    }

    private record ImageBuffers(BufferedImage meterBackground,
                                BufferedImage combinedBufferedImage,
                                WritableImage awtJFXConvertBuffer,
                                int width,
                                int height) { }

    private Optional<ImageBuffers> maybeImageBuffers = Optional.empty();
    /**
     * Redraw on UI thread by adding needle to 'meter_background'
     */
    private void redrawIndicator(double value, WARNING warning) {
        withReadLock(() -> {
            if (maybeImageBuffers.isEmpty()) {
                return;
            }
            else {
                ImageBuffers imageBuffers = maybeImageBuffers.get();
                BufferedImage meterBackground = imageBuffers.meterBackground();
                BufferedImage combined = imageBuffers.combinedBufferedImage();
                WritableImage awtJFXConvertBuffer = imageBuffers.awtJFXConvertBuffer;
                int width = imageBuffers.width();
                int height = imageBuffers.height();
                if (meterBackground.getType() != BufferedImage.TYPE_INT_ARGB) {
                    throw new IllegalPathStateException("Need TYPE_INT_ARGB for direct buffer access, not " + meterBackground.getType());
                }

                int[] src = ((DataBufferInt) meterBackground.getRaster().getDataBuffer()).getData();
                int[] dest = ((DataBufferInt) combined.getRaster().getDataBuffer()).getData();
                System.arraycopy(src, 0, dest, 0, width * height);

                // Add needle & label
                Graphics2D gc = combined.createGraphics();

                DisplayMode displayMode = this.displayMode;
                if (displayMode.equals(DisplayMode.NEEDLE)) {
                    redrawBorderAroundLinearMeter(gc); // When drawing a needle, redraw the border *before* drawing the needle.
                    drawNeedle(gc, value);
                }
                else if (displayMode.equals(DisplayMode.BAR)) {
                    drawBar(gc, value);
                    redrawBorderAroundLinearMeter(gc); // When drawing a bar, redraw the border *after* drawing the bar.
                }
                else {
                    throw new RuntimeException("Unhandled case");
                }

                if (warning != WARNING.NONE) {
                    drawWarningText(gc, warning.toString());
                }
                if (showUnits) {
                    drawUnit(gc);
                }

                // Convert to JFX image and show
                runOnJavaFXThread(() -> {
                    withWriteLock(() -> {
                        awtJFXConvertBuffer.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), dest, 0, width);
                        setImage(awtJFXConvertBuffer);
                    });
                });

                logger.log(Level.FINE, "Redraw meter");
            }
        });
    }

    private void redrawBorderAroundLinearMeter(Graphics2D gc) {
        {
            int width;
            int height;

            // Re-draw border around widget:
            width = linearMeterScale.getBounds().width - marginLeft - marginRight - 1;
            height = linearMeterScale.getBounds().height - marginAbove - marginBelow - 1;
            paintRectangle(gc,
                    new Rectangle(marginLeft,
                            marginAbove,
                            width,
                            height),
                    TRANSPARENT);
        }
    }

    /** Call to update size of meter
     *
     *  @param width
     *  @param height
     */
    public void setSize(int width, int height) {
        withWriteLock(() -> {
            linearMeterScale.setBounds(0, 0, width, height);
            updateMeterBackground();
            layout();
            updateActiveColors();
            requestLayout();
        });
    }

    /** @param color Foreground (labels, tick marks) color */
    public void setForeground(javafx.scene.paint.Color color) {
        withWriteLock(() -> {
            foreground = GraphicsUtils.convert(color);
            linearMeterScale.setColor(color);
        });
    }

    /** @param color Background color */
    public void setBackground(javafx.scene.paint.Color color)
    {
        withWriteLock(() -> {
            background = GraphicsUtils.convert(color);
        });
    }

    /** @param font Label font */
    public void setFont(javafx.scene.text.Font font)
    {
        withWriteLock(() -> {
            linearMeterScale.setScaleFont(font);
            this.font = GraphicsUtils.convert(font);
        });
    }

    private boolean showWarnings = true;

    public void setShowWarnings(boolean showWarnings) {
        withWriteLock(() -> {
            this.showWarnings = showWarnings;
        });
    }

    private boolean lag = false;
    private boolean isValueWaitingToBeDrawn = false;
    private double valueWaitingToBeDrawn = Double.NaN;
    /** @param newValue Current value */
    public void setCurrentValue(double newValue)
    {
        withWriteLock(() -> {
            valueWaitingToBeDrawn = newValue;

            if (isValueWaitingToBeDrawn) {
                lag = true;
            }
            else {
                isValueWaitingToBeDrawn = true;

                drawNewValue(valueWaitingToBeDrawn);
                isValueWaitingToBeDrawn = false;
                lag = false;
            }
        });
    }

    private Optional<Integer> computeIndicatorPosition(double value) {
        if (Double.isNaN(value)) {
            return Optional.empty();
        }
        return withReadLock(() -> {
            if (linearMeterScale.isHorizontal()) {
                return Optional.of((int) Math.round(marginLeft + pixelsPerScaleUnit * (value - linearMeterScale.getValueRange().getLow())));
            }
            else {
                return Optional.of((int) Math.round(linearMeterScale.getBounds().height - marginBelow - pixelsPerScaleUnit * (value - linearMeterScale.getValueRange().getLow())));
            }
        });
    }

    private void drawNewValue(double newValue) {
        withWriteLock(() -> {
            WARNING oldWarning = determineWarning();
            AtomicReference<Double> newValueAtomicReference = new AtomicReference<>(newValue); // Workaround, since captured variables need to be effectively final in Java.
            double oldValue = currentValue;
            Optional<Integer> maybeOldIndicatorPosition = computeIndicatorPosition(oldValue);
            currentValue = newValueAtomicReference.get();

            if (newValueAtomicReference.get() > linearMeterScale.getValueRange().getHigh() && newValueAtomicReference.get() <= linearMeterScale.getValueRange().getHigh() + minMaxTolerance) {
                newValueAtomicReference.set(linearMeterScale.getValueRange().getHigh());
            }
            if (newValueAtomicReference.get() < linearMeterScale.getValueRange().getLow() && newValueAtomicReference.get() >= linearMeterScale.getValueRange().getLow() - minMaxTolerance) {
                newValueAtomicReference.set(linearMeterScale.getValueRange().getLow());
            }

            WARNING newWarning = determineWarning();
            logNewWarningIfDifferent(oldWarning, newWarning);

            if (oldValue != newValueAtomicReference.get()) {
                if (!Double.isNaN(newValueAtomicReference.get())){
                    Optional<Integer> maybeNewIndicatorPosition = computeIndicatorPosition(newValue);
                    boolean indicatorPositionHasChanged = maybeNewIndicatorPosition.isPresent() != maybeOldIndicatorPosition.isPresent() || maybeOldIndicatorPosition.isPresent() && maybeNewIndicatorPosition.isPresent() && maybeOldIndicatorPosition.get() != maybeNewIndicatorPosition.get();
                    if (indicatorPositionHasChanged || determineWarning() != newWarning) {
                        redrawIndicator(newValueAtomicReference.get(), newWarning);
                    }
                }
                else if (!Double.isNaN(oldValue)) {
                    redrawIndicator(newValueAtomicReference.get(), newWarning);
                }
            }
        });
    }

    private WARNING determineWarning() {
        return withReadLock(() -> {
            if (!showWarnings) {
                return WARNING.NONE;
            }
            else if (lag) {
                return WARNING.LAG;
            }
            else if (!validRange) {
                return WARNING.MIN_AND_MAX_NOT_DEFINED;
            }
            else if (currentValue < linearMeterScale.getValueRange().getLow() - minMaxTolerance) {
                return WARNING.VALUE_LESS_THAN_MIN;
            }
            else if (currentValue > linearMeterScale.getValueRange().getHigh() + minMaxTolerance) {
                return WARNING.VALUE_GREATER_THAN_MAX;
            }
            else {
                return WARNING.NONE;
            }
        });
    }

    /** @param visible Whether the scale must be displayed or not. */
    public void setScaleVisible (boolean visible)
    {
        withWriteLock(() -> {
            linearMeterScale.setVisible(visible);
            updateMeterBackground();
        });
    }

    /** Request a complete redraw with new layout */
    private void requestLayout()
    {
        withWriteLock(() -> {
            updateMeterBackground();
            WARNING warning = determineWarning();
            redrawIndicator(currentValue, warning);
        });
    }

    private void computeLayout()
    {
        withReadLock(() -> {
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
        });
    }

    /** Draw meter background (scale) into image buffer
     *  @return Latest image, must be of type BufferedImage.TYPE_INT_ARGB
     */
    private void updateMeterBackground()
    {
        withWriteLock(() -> {
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

            gc.setBackground(background);
            gc.clearRect(0, 0, width, height);

            linearMeterScale.paint(gc, new Rectangle(0,0,0,0));
            paintMeter(gc);

            {
                BufferedImage combined = new BufferedImage(linearMeterScale.getBounds().width, linearMeterScale.getBounds().height, BufferedImage.TYPE_INT_ARGB);
                WritableImage awtJFXConvertBuffer = new WritableImage(linearMeterScale.getBounds().width, linearMeterScale.getBounds().height);
                maybeImageBuffers = Optional.of(new ImageBuffers(image, combined, awtJFXConvertBuffer, width, height));
            }
        });
    }

    private void paintMeter(Graphics2D graphics) {
        withReadLock(() -> {
            Color color = graphics.getColor();
            if (showLimits) {
                if (isHighlightActiveRegionEnabled) {
                    paintRectangle(graphics, normalRectangle, normalStatusActiveColor_lowlighted);
                    paintRectangle(graphics, lowRectangle, minorAlarmActiveColor_lowlighted);
                    paintRectangle(graphics, highRectangle, minorAlarmActiveColor_lowlighted);
                    paintRectangle(graphics, loLoRectangle, majorAlarmActiveColor_lowlighted);
                    paintRectangle(graphics, hiHiRectangle, majorAlarmActiveColor_lowlighted);
                }
                else {
                    paintRectangle(graphics, normalRectangle, normalStatusActiveColor_highlighted);
                    paintRectangle(graphics, lowRectangle, minorAlarmActiveColor_highlighted);
                    paintRectangle(graphics, highRectangle, minorAlarmActiveColor_highlighted);
                    paintRectangle(graphics, loLoRectangle, majorAlarmActiveColor_highlighted);
                    paintRectangle(graphics, hiHiRectangle, majorAlarmActiveColor_highlighted);
                }
            }
            else {
                paintRectangle(graphics,
                        new Rectangle(marginLeft,
                                marginAbove,
                                linearMeterScale.getBounds().width - marginLeft - marginRight,
                                linearMeterScale.getBounds().height - marginAbove - marginBelow),
                        normalStatusActiveColor_lowlighted);
            }
            graphics.setColor(color);
        });
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

    /** Draw needle and label for current value */
    private void drawNeedle(Graphics2D gc, double value) {
        withReadLock(() -> {
            if (Double.isNaN(value)) {
                return;
            }
            else {
                Stroke oldStroke = gc.getStroke();
                Paint oldPaint = gc.getPaint();
                RenderingHints oldrenderingHints = gc.getRenderingHints();

                gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (showLimits) {
                    if (isHighlightActiveRegionEnabled) {
                        if (value <= loLo) {
                            paintRectangle(gc, loLoRectangle, majorAlarmColor_highlighted);
                        }
                        else if (value >= hiHi) {
                            paintRectangle(gc, hiHiRectangle, majorAlarmColor_highlighted);
                        }
                        else if (value <= low && value > loLo) {
                            paintRectangle(gc, lowRectangle, minorAlarmActiveColor_highlighted);
                        }
                        else if (value >= high && value < hiHi) {
                            paintRectangle(gc, highRectangle, minorAlarmActiveColor_highlighted);
                        }
                        else {
                            paintRectangle(gc, normalRectangle, normalStatusActiveColor_highlighted);
                        }
                    }
                }

                if (linearMeterScale.isHorizontal()) {
                    if (value >= linearMeterScale.getValueRange().getLow() && value <= linearMeterScale.getValueRange().getHigh()) {

                        Optional<Integer> maybeCurrentIndicatorPosition = computeIndicatorPosition(value);

                        if (maybeCurrentIndicatorPosition.isPresent()) {
                            int currentIndicatorPosition = maybeCurrentIndicatorPosition.get();

                            if (knobSize > 0) {
                                int[] XVal = { currentIndicatorPosition - (int) Math.round((1.0 * knobSize) / 4.0),
                                        currentIndicatorPosition + (int) Math.round((1.0 * knobSize) / 4.0),
                                        currentIndicatorPosition };

                                int[] YVal = { 0, 0, marginAbove - 2 };

                                gc.setStroke(AxisPart.TICK_STROKE);
                                gc.setColor(knobColor);
                                gc.fillPolygon(XVal, YVal, 3);
                                gc.setColor(knobColor);
                                gc.drawPolygon(XVal, YVal, 3);
                            }

                            if (needleWidth > 0) {
                                gc.setStroke(new BasicStroke((float) needleWidth));
                                gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                                gc.setPaint(needleColor);

                                int y1 = marginAbove + needleWidth / 2 + 1;
                                int y2 = linearMeterScale.getBounds().height - marginBelow - (needleWidth - 1) / 2 - 2;

                                gc.drawLine(currentIndicatorPosition, y1, currentIndicatorPosition, y2);
                            }
                        }
                    }
                } else {
                    if (value >= linearMeterScale.getValueRange().getLow() && value <= linearMeterScale.getValueRange().getHigh()) {

                        Optional<Integer> maybeCurrentIndicatorPosition = computeIndicatorPosition(value);
                        if (maybeCurrentIndicatorPosition.isPresent()) {
                            int currentIndicatorPosition = maybeCurrentIndicatorPosition.get() - 1;

                            if (knobSize > 0) {
                                int[] YVal = { currentIndicatorPosition + (int) Math.round((1.0 * knobSize / 4.0)),
                                        currentIndicatorPosition - (int) Math.round((1.0 * knobSize / 4.0)),
                                        currentIndicatorPosition };

                                int[] XVal = { 0, 0, marginLeft - 2 };

                                gc.setStroke(AxisPart.TICK_STROKE);
                                gc.setColor(knobColor);
                                gc.fillPolygon(XVal, YVal, 3);
                                gc.setColor(knobColor);
                                gc.drawPolygon(XVal, YVal, 3);
                            }

                            if (needleWidth > 0) {
                                gc.setStroke(new BasicStroke((float) needleWidth));
                                gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                                gc.setPaint(needleColor);

                                int x1 = marginLeft + (needleWidth)/2 + 1;
                                int x2 = linearMeterScale.getBounds().width - marginRight - (needleWidth+1)/2 - 1;

                                gc.drawLine(x1, currentIndicatorPosition, x2, currentIndicatorPosition);
                            }
                        }
                    }
                }

                gc.setRenderingHints(oldrenderingHints);
                gc.setStroke(oldStroke);
                gc.setPaint(oldPaint);
            }
        });
    }
    private final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private void drawBar(Graphics2D gc, double value) {
        withReadLock(() -> {
            if (Double.isNaN(value)) {
                return;
            }
            else {
                Stroke oldStroke = gc.getStroke();
                Paint oldPaint = gc.getPaint();
                RenderingHints oldrenderingHints = gc.getRenderingHints();

                gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Optional<Integer> maybeCurrentIndicatorPosition = computeIndicatorPosition(value <= linearMeterScale.getValueRange().getHigh() ? value : linearMeterScale.getValueRange().getHigh());

                if (maybeCurrentIndicatorPosition.isPresent()) {
                    if (linearMeterScale.isHorizontal()) {
                        int currentIndicatorPosition = maybeCurrentIndicatorPosition.get() + 1;
                        if (value > linearMeterScale.getValueRange().getLow()) {
                            if (isHighlightActiveRegionEnabled) {
                                if (value <= loLo) {
                                    gc.setPaint(majorAlarmColor_highlighted);
                                }
                                else if (value >= hiHi) {
                                    gc.setPaint(majorAlarmColor_highlighted);
                                }
                                else if (value <= low && value > loLo) {
                                    gc.setPaint(minorAlarmActiveColor_highlighted);
                                }
                                else if (value >= high && value < hiHi) {
                                    gc.setPaint(minorAlarmActiveColor_highlighted);
                                }
                                else {
                                    gc.setPaint(needleColor);
                                }
                            }
                            else {
                                gc.setPaint(needleColor);
                            }
                            // Draw the bar:
                            gc.fillRect(marginLeft, marginAbove, currentIndicatorPosition - marginLeft, meterBreadth);
                        }
                    } else {
                        int currentIndicatorPosition = maybeCurrentIndicatorPosition.get() - 1;
                        if (value > linearMeterScale.getValueRange().getLow()) {
                            if (isHighlightActiveRegionEnabled) {
                                if (value <= loLo) {
                                    gc.setPaint(majorAlarmColor_highlighted);
                                }
                                else if (value >= hiHi) {
                                    gc.setPaint(majorAlarmColor_highlighted);
                                }
                                else if (value <= low && value > loLo) {
                                    gc.setPaint(minorAlarmActiveColor_highlighted);
                                }
                                else if (value >= high && value < hiHi) {
                                    gc.setPaint(minorAlarmActiveColor_highlighted);
                                }
                                else {
                                    gc.setPaint(needleColor);
                                }
                            }
                            else {
                                gc.setPaint(needleColor);
                            }
                            // Draw the bar:
                            gc.fillRect(marginLeft, currentIndicatorPosition, meterBreadth - 1, linearMeterScale.getBounds().height - currentIndicatorPosition - marginBelow);
                        }
                    }
                }

                gc.setRenderingHints(oldrenderingHints);
                gc.setStroke(oldStroke);
                gc.setPaint(oldPaint);
            }
        });
    }

    /** Should be invoked when meter no longer used to release resources */
    public void dispose()
    {
        withWriteLock(() -> {
            // Release memory ASAP
            maybeImageBuffers = Optional.empty();
        });
    }

    public void setHorizontal(boolean horizontal) {
        withWriteLock(() -> {
            linearMeterScale.setHorizontal(horizontal);
            redrawLinearMeterScale();
            updateMeterBackground();
            redrawIndicator(currentValue, determineWarning());
        });
    }

    private void drawUnit(Graphics2D gc) {
        withReadLock(() -> {
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
        });
    }

    private void drawWarning_horizontal(Graphics2D gc, String warningText) {
        withReadLock(() -> {
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
        });
    }

    private void drawWarningText(Graphics2D gc, String warningText) {
        withReadLock(() -> {
            RenderingHints oldRenderingHints = gc.getRenderingHints();
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (linearMeterScale.isHorizontal()) {
                drawWarning_horizontal(gc, warningText);
            }
            else {
                drawWarning_vertical(gc, warningText);
            }
            gc.setRenderingHints(oldRenderingHints);
        });
    }

    private void drawWarning_vertical(Graphics2D gc, String warningText) {
        withReadLock(() -> {
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
        });
    }

    private void paintRectangle(Graphics2D gc, Rectangle rectangle, Paint paint){
        withReadLock(() -> {
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
            gc.setColor(foreground);
            gc.draw(rectangle);

            gc.setColor(oldColor);
        });
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
        withReadLock(() -> {
            double displayedLoLo;
            double displayedLow;
            double displayedHiHi;
            double displayedHigh;

            double loLoValue = loLo;
            double lowValue = low;
            double highValue = high;
            double hiHiValue = hiHi;

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
                int knobSizeValue = knobSize;
                marginAbove = displayMode == DisplayMode.NEEDLE && knobSizeValue >= 1 ? knobSizeValue + 2 : 0;
                if (linearMeterScale.isVisible() && fontMetrics != null) {
                    var majorTicks = linearMeterScale.getTicks().getMajorTicks();
                    if (majorTicks.size() >= 2) {
                        marginLeft = fontMetrics.stringWidth(majorTicks.get(0).getLabel()) / 2;
                        marginRight = fontMetrics.stringWidth(majorTicks.get(majorTicks.size() - 1).getLabel()) / 2 - 1;
                    } else if (majorTicks.size() == 1) {
                        marginRight = marginLeft = fontMetrics.stringWidth(majorTicks.get(0).getLabel()) / 2;
                    } else {
                        marginRight = 0;
                        marginLeft = 0;
                    }
                    marginBelow = (int) (0.5 * linearMeterScale.getTickLength() + 4 + fontMetrics.getAscent() + fontMetrics.getDescent());
                } else {
                    marginLeft = 0;
                    marginRight = 0;
                    marginBelow = 0;
                }

                if (showUnits && fontMetrics != null) {
                    marginBelow += 1 + fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
                }

                pixelsPerScaleUnit = (linearMeterScale.getBounds().width - marginLeft - marginRight - 1) / (linearMeterScale.getValueRange().getHigh() - linearMeterScale.getValueRange().getLow());
                meterBreadth = Math.round(linearMeterScale.getBounds().height - marginAbove - marginBelow) - 1;

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
                int knobSizeValue = knobSize;
                marginLeft = displayMode == DisplayMode.NEEDLE && knobSizeValue >= 1 ? knobSizeValue + 2 : 0;
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
                    marginRight = 0;
                    marginAbove = 0;
                    marginBelow = 0;
                }

                if (showUnits && fontMetrics != null) {
                    marginBelow += 1 + fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
                }

                pixelsPerScaleUnit = (linearMeterScale.getBounds().height - marginAbove - marginBelow - 1) / (linearMeterScale.getValueRange().getHigh() - linearMeterScale.getValueRange().getLow());
                meterBreadth = Math.round(linearMeterScale.getBounds().width - marginLeft - marginRight);

                double y_loLoRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedLoLo);
                double y_lowRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedLow);
                double y_normalRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedHigh);
                double y_highRectangle = marginAbove + pixelsPerScaleUnit * (linearMeterScale.getValueRange().getHigh() - displayedHiHi);

                loLoRectangle = new Rectangle(marginLeft,
                        (int) Math.round(y_loLoRectangle),
                        meterBreadth - 1,
                        (int) (Math.round(pixelsPerScaleUnit * (displayedLoLo - linearMeterScale.getValueRange().getLow()))));

                lowRectangle = new Rectangle(marginLeft,
                        (int) Math.round(y_lowRectangle),
                        meterBreadth - 1,
                        (int) (Math.round(y_loLoRectangle) - Math.round(y_lowRectangle)));

                normalRectangle =  new Rectangle(marginLeft,
                        (int) Math.round(y_normalRectangle),
                        meterBreadth - 1,
                        (int) (Math.round(y_lowRectangle) - Math.round(y_normalRectangle)));

                highRectangle = new Rectangle(marginLeft,
                        (int) Math.round(y_highRectangle),
                        meterBreadth - 1,
                        (int) (Math.round(y_normalRectangle) - Math.round(y_highRectangle)));

                hiHiRectangle = new Rectangle(marginLeft,
                        marginAbove,
                        meterBreadth - 1,
                        (int) Math.round(y_highRectangle) - marginAbove);
            }
        });
    }
}
