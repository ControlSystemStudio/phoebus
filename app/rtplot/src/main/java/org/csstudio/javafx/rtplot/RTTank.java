/*******************************************************************************
 * Copyright (c) 2014-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.javafx.rtplot.internal.LinearTicks;
import org.csstudio.javafx.rtplot.internal.PlotPart;
import org.csstudio.javafx.rtplot.internal.PlotPartListener;
import org.csstudio.javafx.rtplot.internal.YAxisImpl;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.UpdateThrottle;
import org.phoebus.ui.vtype.ScaleFormat;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

/** Tank with scale
 *
 *  <p>Renders a vertical tank with fill level, optional left and right
 *  scales, a foreground outline, and optional alarm/warning limit lines.
 *  With {@link #setThermometerStyle} the tank is drawn as a thermometer,
 *  a narrow tube with a bulb, using the same scales and limit lines.
 *  The dual-scale layout is modelled after CS-Studio BOY's tank widget
 *  which supported markers on both sides of the bar.
 *
 *  <p>When alarm limits come from a live PV they are drawn as solid
 *  lines; when they are manually configured the lines are drawn dashed
 *  so the operator can tell at a glance that they are not tied to the
 *  control system's alarm state.
 *
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, dual scale, alarm limits,
 *          format/precision, outline, log scale fixes
 */
@SuppressWarnings("nls")
public class RTTank extends Canvas
{
    /** Area of this canvas */
    protected volatile Rectangle area = new Rectangle(0, 0, 0, 0);

    /** Background color */
    private volatile Color background = Color.WHITE;

    /** Foreground color (used for scale ticks and the tank outline) */
    private volatile Color foreground = Color.BLACK;

    /** Fill color */
    private volatile Color empty = Color.LIGHT_GRAY.brighter().brighter();
    private volatile Color empty_shadow = Color.LIGHT_GRAY;

    /** Fill color.
     *  {@code fill_highlight} is a lighter variant used for a gradient.
     */
    private volatile Color fill = Color.BLUE;
    private volatile Color fill_highlight = new Color(72, 72, 255);

    /** Configurable alarm/warning line colors (default: named MAJOR=red, MINOR=orange) */
    private volatile Color limit_major_color = Color.RED;
    private volatile Color limit_minor_color = new Color(255, 128, 0);

    /** Alarm and warning limits drawn as horizontal lines (NaN = not set/hidden) */
    private volatile double limit_lolo = Double.NaN;
    private volatile double limit_lo   = Double.NaN;
    private volatile double limit_hi   = Double.NaN;
    private volatile double limit_hihi = Double.NaN;

    /** Are the alarm limits sourced from PV metadata? When false, use dashed lines. */
    private volatile boolean limits_from_pv = true;

    /** Is the right-side scale displayed? */
    private volatile boolean right_scale_visible = false;

    /** Border width in pixels around the tank body; 0 = no border (default) */
    private volatile int border_width = 0;

    /** Extra inset from the canvas edge to the plot body on all four sides.
     *  0 for the tank look. With a bar track, the track extends into this
     *  space, so it becomes the gap between the track and the fill. */
    private volatile int innerPadding = 0;

    /** Paint the empty part like the track of a progress bar, framed and
     *  shaded across the bar like the JavaFX control, instead of the tank's
     *  left-to-center gradient. */
    private volatile boolean barTrack = false;

    /** Stop positions of the track and fill shades, see {@link #shadesOf} */
    private static final float[] TRACK_FRACTIONS = { 0.0f, 1f/3, 2f/3, 1.0f };

    /** Stop positions of the frame shades, see {@link #frameShadesOf} */
    private static final float[] FRAME_FRACTIONS = { 0.0f, 1.0f };

    /** Corner arcs of the bar track frame and of the track and fill inside it,
     *  twice the radii of the JavaFX style sheet */
    private static final int FRAME_ARC = 6;
    private static final int TRACK_ARC = 4;

    /** Shades of {@link #empty} for the bar track */
    private volatile Color[] trackShades = shadesOf(Color.LIGHT_GRAY.brighter().brighter());

    /** Shades of the frame around the bar track */
    private volatile Color[] frameShades = frameShadesOf(255);

    /** Shades of {@link #fill} for the filled part of a bar */
    private volatile Color[] fillShades = shadesOf(Color.BLUE);

    /** Draw as a thermometer instead of a tank: a narrow tube, centered
     *  in the plot area, with a bulb at the bottom. Scale, value mapping
     *  and alarm limits are shared with the tank look. */
    private volatile boolean thermometerStyle = false;

    /** Extra bulb diameter beyond the tube width, in pixels.
     *  Thermometer look only; the bulb is clamped to the space left by the scales. */
    private volatile int bulbSize = 20;

    /** Thermometer geometry, computed by {@link #thermoGeometry} and
     *  shared by tube, bulb, liquid and scales so they stay aligned.
     *  All values are canvas pixels. */
    record ThermoGeom(double centerX, double tubeWidth,
                      double tubeTop, double tubeBottom,
                      double bulbCenterY, double bulbRadius)
    {
        double tubeLeft()     { return centerX - tubeWidth / 2; }
        double tubeRight()    { return centerX + tubeWidth / 2; }
        double bulbDiameter() { return 2 * bulbRadius; }
    }

    /** Pixels reserved around the thermometer for the scales: width of the
     *  left and right scale, label overhang at the top and bottom */
    record ScaleSpace(int left, int right, int top, int bottom) {}

    /** Geometry from the most recent thermometer layout, {@code null} until then */
    private volatile ThermoGeom thermoGeom = null;

    /** Current value, i.e. fill level */
    private volatile double value = 5.0;

    /** Does layout need to be re-computed? */
    protected final AtomicBoolean need_layout = new AtomicBoolean(true);

    /** Throttle updates, enforcing a 'dormant' period */
    private final UpdateThrottle update_throttle;

    /** Buffer for image of the tank and scale */
    private volatile Image plot_image = null;

    /** Is the scale displayed or not. */
    private volatile boolean scale_visible = true;

    protected final AtomicBoolean needUpdate = new AtomicBoolean(true);

    /** Listener to {@link PlotPart}s, triggering refresh of canvas */
    protected final PlotPartListener plot_part_listener = new PlotPartListener()
    {
        @Override
        public void layoutPlotPart(final PlotPart plotPart)
        {
            need_layout.set(true);
        }

        @Override
        public void refreshPlotPart(final PlotPart plotPart)
        {
            requestUpdate();
        }
    };

    /** Redraw the canvas on UI thread by painting the 'plot_image' */
    final private Runnable redraw_runnable = () ->
    {
        final GraphicsContext gc = getGraphicsContext2D();
        final Image image = plot_image;
        if (image != null)
            synchronized (image)
            {
                // Clear the canvas for e.g. transparent pixels (otherwise image appears drawn over previous)
                gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
                // Draw the updated image
                gc.drawImage(image, 0, 0);
            }
    };

    /** Left-side scale axis */
    final private YAxisImpl<Double> scale = new YAxisImpl<>("", plot_part_listener);

    /** Right-side (opposite) scale axis.
     *  Always has {@code is_right=true} and {@code force_text_up=true}
     *  so that axis labels read in the same direction as the left scale.
     */
    final private YAxisImpl<Double> right_scale = new YAxisImpl<>("", plot_part_listener);

    final private PlotPart plot_area = new PlotPart("main", plot_part_listener);



    /** Constructor */
    public RTTank()
    {
        final ChangeListener<? super Number> resize_listener = (prop, old, value) ->
        {
            area = new Rectangle((int)getWidth(), (int)getHeight());
            need_layout.set(true);
            requestUpdate();
        };
        widthProperty().addListener(resize_listener);
        heightProperty().addListener(resize_listener);

        // 20Hz default throttle.
        // When parallel_rendering is enabled, each tank renders on the shared thread pool
        // so that many tanks on a display update concurrently.  The default (false) serialises
        // all renders on the single global UpdateThrottle.TIMER thread — safe but slow for
        // displays with many Tank / ProgressBar widgets.
        update_throttle = new UpdateThrottle(50, TimeUnit.MILLISECONDS, () ->
        {
            if (needUpdate.getAndSet(false)){
                plot_image = updateImageBuffer(); // This will return null if image buffer instantiation times out
                if(plot_image != null){
                    redrawSafely();
                }
                else{
                    requestUpdate();
                }
            }
        }, Activator.parallel_rendering ? Activator.thread_pool : UpdateThrottle.TIMER);

        // Configure right-side scale — must happen after update_throttle is
        // initialised because setOnRight() triggers requestUpdate() via the
        // PlotPartListener.  setForceTextUp(true) keeps the label direction
        // consistent with the left scale.
        right_scale.setOnRight(true);
        right_scale.setForceTextUp(true);
    }


    /** Update the dormant time between updates
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setUpdateThrottle(final long dormant_time, final TimeUnit unit)
    {
        update_throttle.setDormantTime(dormant_time, unit);
    }

    /** @param font Scale font */
    public void setFont(final Font font)
    {
        scale.setLabelFont(font);
        scale.setScaleFont(font);
        right_scale.setLabelFont(font);
        right_scale.setScaleFont(font);
    }

    /** @param width Border width in pixels around the tank body (0 = no border, max = 5) */
    public void setBorderWidth(final int width)
    {
        border_width = Math.max(0, Math.min(5, width));
        requestUpdate();
    }

    /** @param pixels Extra inset from all four canvas edges to the plot body, 0..20.
     *                For a bar, the gap between the track and the fill.
     *                The thermometer look keeps a fixed margin instead. */
    public void setInnerPadding(final int pixels)
    {
        innerPadding = Math.clamp(pixels, 0, 20);
        need_layout.set(true);
        requestUpdate();
    }

    /** @param bar Paint the empty part like a progress bar track ({@code true})
     *             or like a tank ({@code false}, default) */
    public void setBarTrack(final boolean bar)
    {
        barTrack = bar;
        requestUpdate();
    }

    /** Select the thermometer look: a narrow tube with a bulb at the bottom
     *  instead of the full width tank body. Scale, value mapping and alarm
     *  limits behave the same in both looks.
     *  @param thermometer {@code true} for thermometer, {@code false} (default) for tank */
    public void setThermometerStyle(final boolean thermometer)
    {
        thermometerStyle = thermometer;
        need_layout.set(true);
        requestUpdate();
    }

    /** @param pixels Extra bulb diameter beyond the tube width (thermometer look),
     *                clamped at layout time so the bulb fits the plot area */
    public void setBulbSize(final int pixels)
    {
        bulbSize = Math.max(0, pixels);
        need_layout.set(true);
        requestUpdate();
    }

    /** @param color Background color */
    public void setBackground(final javafx.scene.paint.Color color)
    {
        background = GraphicsUtils.convert(Objects.requireNonNull(color));
    }

    /** @param color Foreground color (scale ticks and tank outline) */
    public void setForeground(final javafx.scene.paint.Color color)
    {
        foreground = GraphicsUtils.convert(Objects.requireNonNull(color));
        scale.setColor(color);
        right_scale.setColor(color);
    }

    /** @param color Color for empty region */
    public void setEmptyColor(final javafx.scene.paint.Color color)
    {
        empty = GraphicsUtils.convert(Objects.requireNonNull(color));
        empty_shadow = new Color(
                Math.max(0, empty.getRed()   - 32),
                Math.max(0, empty.getGreen() - 32),
                Math.max(0, empty.getBlue()  - 32),
                empty.getAlpha()
            );
        trackShades = shadesOf(empty);
        frameShades = frameShadesOf(empty.getAlpha());
    }

    /** Shades of a bar color, following the JavaFX progress bar style sheet,
     *  which shades the track and the bar across the bar through -7%, 0%,
     *  -3% and -9% of the color's brightness.
     *  @param color Track or fill color
     *  @return The four shades, across the bar
     */
    private static Color[] shadesOf(final Color color)
    {
        return new Color[] { darker(color, 7), color, darker(color, 3), darker(color, 9) };
    }

    /** Shades of the frame around the bar track, as in the stock Phoebus
     *  progress bar: the JavaFX style sheet shades the frame from -10% of
     *  the text box border color to that color, and Phoebus sets the border
     *  color to a light gray with the alpha of the track color.
     *  @param alpha Alpha of the track color
     *  @return The two shades, across the bar
     */
    private static Color[] frameShadesOf(final int alpha)
    {
        final Color border = new Color(236, 236, 236, alpha);
        return new Color[] { darker(border, 10), border };
    }

    /** @param color Color to darken
     *  @param percent How much darker, in percent of the brightness
     *  @return Darkened color, alpha unchanged
     */
    private static Color darker(final Color color, final int percent)
    {
        final double scale = 1.0 - percent / 100.0;
        return new Color((int) (color.getRed()   * scale),
                         (int) (color.getGreen() * scale),
                         (int) (color.getBlue()  * scale),
                         color.getAlpha());
    }

    /** Paint the track of a bar: a one pixel frame around the shaded track.
     *  The frame is painted as a ring, not under the track, so that a
     *  semi-transparent track color is not applied twice.
     *  @param gc Graphics
     *  @param track Outer bounds of the track, including the frame
     */
    private void paintBarTrack(final Graphics2D gc, final Rectangle track)
    {
        final Rectangle inside = new Rectangle(track);
        inside.grow(-1, -1);
        final Area frame = new Area(new RoundRectangle2D.Double(track.x, track.y, track.width, track.height,
                                                                FRAME_ARC, FRAME_ARC));
        frame.subtract(new Area(new RoundRectangle2D.Double(inside.x, inside.y, inside.width, inside.height,
                                                            TRACK_ARC, TRACK_ARC)));
        gc.setPaint(barPaint(track, FRAME_FRACTIONS, frameShades));
        gc.fill(frame);
        gc.setPaint(barPaint(inside, TRACK_FRACTIONS, trackShades));
        gc.fillRoundRect(inside.x, inside.y, inside.width, inside.height, TRACK_ARC, TRACK_ARC);
    }

    /** @param bounds Area to fill
     *  @return Paint for the filled region: a bar fill or the tank gradient
     */
    private Paint fillPaint(final Rectangle bounds)
    {
        if (barTrack)
            return barPaint(bounds, TRACK_FRACTIONS, fillShades);
        final int center = bounds.x + bounds.width/2;
        return new GradientPaint(bounds.x, 0, fill, center, 0, fill_highlight, true);
    }

    /** @param bounds Area to fill
     *  @param fractions Stop positions of the shades
     *  @param shades Shades across the bar
     *  @return Gradient across the bar, in the style of the JavaFX progress bar.
     *          The tank renders vertically, so the bar runs along Y and the
     *          shading across it is along X, for both widget orientations.
     *          An area too narrow for a gradient gets the first shade.
     */
    private static Paint barPaint(final Rectangle bounds, final float[] fractions, final Color[] shades)
    {
        if (bounds.width < 2)
            return shades[0];
        final int right = bounds.x + bounds.width;
        return new LinearGradientPaint(bounds.x, bounds.y, right, bounds.y,
                                       fractions, shades,
                                       MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    /** @param color Color for filled region */
    public void setFillColor(final javafx.scene.paint.Color color)
    {
        fill = GraphicsUtils.convert(Objects.requireNonNull(color));
        final int saturationContribution = (int) ( 48.f * Color.RGBtoHSB(fill.getRed(), fill.getGreen(), fill.getBlue(), null)[1] );
        fillShades = shadesOf(fill);
        fill_highlight = new Color(
            Math.min(255, fill.getRed()   + 32 + saturationContribution),
            Math.min(255, fill.getGreen() + 32 + saturationContribution),
            Math.min(255, fill.getBlue()  + 32 + saturationContribution),
            empty.getAlpha()
        );
    }

    /** @param visible Whether the left-side scale must be displayed. */
    public void setScaleVisible (boolean visible)
    {
        if (visible != scale_visible)
        {
            scale_visible = visible;
            need_layout.set(true);
        }
    }

    /** @param logscale Use log scale for y-axis? */
    public void setLogScale(final boolean logscale)
    {
        scale.setLogarithmic(logscale);
        right_scale.setLogarithmic(logscale);
        requestUpdate();
    }

    /** @param show Show minor tick marks on the scale? */
    public void setShowMinorTicks(final boolean show)
    {
        scale.setShowMinorTicks(show);
        right_scale.setShowMinorTicks(show);
        requestUpdate();
    }

    /** Show or hide the tick labels while keeping the tick marks.
     *  Stacked widgets can then share one labelled scale: only the first
     *  shows labels, the others show aligned tick marks.
     *  @param visible {@code true} (default) for labels, {@code false} for ticks only */
    public void setScaleLabelsVisible(final boolean visible)
    {
        // The axes request layout and refresh themselves when this changes
        scale.setScaleLabelsVisible(visible);
        right_scale.setScaleLabelsVisible(visible);
    }

    /** Configure the number format used for scale tick labels.
     *  @param format    Display format; {@code null} or {@link ScaleFormat#DEFAULT} restores automatic formatting.
     *  @param precision Number of decimal places; clamped to [0, 15].
     */
    public void setLabelFormat(final ScaleFormat format, final int precision)
    {
        final int prec = Math.max(0, Math.min(15, precision));
        final NumberFormat fmt;
        if (format == null  ||  format == ScaleFormat.DEFAULT)
            fmt = null;
        else switch (format)
        {
        case DECIMAL:
            fmt = LinearTicks.createDecimalFormat(prec);
            break;
        case EXPONENTIAL:
            fmt = LinearTicks.createExponentialFormat(prec);
            break;
        case ENGINEERING:
            // Engineering format constrains the exponent to multiples of 3.
            // RTTank places ticks via the axis algorithm which does not guarantee
            // that constraint, so this is a best-effort approximation using
            // exponential notation with the requested precision.
            fmt = LinearTicks.createExponentialFormat(prec);
            break;
        case COMPACT:
            // COMPACT picks decimal or exponential per value depending on magnitude.
            // A scale axis applies one NumberFormat to all tick labels, so per-value
            // switching cannot be expressed as a single static format.
            // Fall back to automatic formatting, which already chooses a compact
            // representation based on the axis range.
            fmt = null;
            break;
        case SIGNIFICANT:
            fmt = significantDigitsFormat(prec);
            break;
        default:
            fmt = null;
            break;
        }
        scale.setLabelFormat(fmt);
        right_scale.setLabelFormat(fmt);
        requestUpdate();
    }

    /** Build a {@link NumberFormat} that formats each value to {@code prec} significant
     *  figures using {@code %g}-style notation (decimal or scientific per value magnitude).
     */
    private static NumberFormat significantDigitsFormat(final int prec)
    {
        final String pattern = "%." + prec + "g";
        return new NumberFormat()
        {
            @Override
            public StringBuffer format(final double v, final StringBuffer buf, final java.text.FieldPosition pos)
            {
                return buf.append(normaliseExponent(String.format(java.util.Locale.ROOT, pattern, v)));
            }
            @Override
            public StringBuffer format(final long v, final StringBuffer buf, final java.text.FieldPosition pos)
            {
                return buf.append(normaliseExponent(String.format(java.util.Locale.ROOT, pattern, (double) v)));
            }
            @Override
            public Number parse(final String s, final java.text.ParsePosition pos)
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** Pre-compiled pattern for stripping the sign and leading zeros from a
     *  {@code %g} exponent string such as {@code "-01"} or {@code "+02"}.
     */
    private static final Pattern EXP_LEADING_ZEROS = Pattern.compile("^[+-]?0*");

    /** Normalise a {@code %g}-formatted string to match Phoebus axis convention:
     *  uppercase {@code E}, no leading zeros on the exponent, no {@code +} sign.
     *  Examples: {@code "1.0e-01"} &rarr; {@code "1.0E-1"},
     *            {@code "2.5e+02"} &rarr; {@code "2.5E2"}.
     */
    private static String normaliseExponent(final String s)
    {
        final int e = s.indexOf('e');
        if (e < 0)
            return s;   // decimal notation — no exponent to fix
        final String mantissa = s.substring(0, e);
        final String raw = s.substring(e + 1);      // e.g. "-01", "+02"
        final boolean neg = raw.startsWith("-");
        final String digits = EXP_LEADING_ZEROS.matcher(raw).replaceFirst("");
        return mantissa + "E" + (neg ? "-" : "") + (digits.isEmpty() ? "0" : digits);
    }

    /** Set alarm and warning limit values to display as horizontal lines on the tank.
     *  Pass {@link Double#NaN} for any limit that should not be shown.
     *  @param lolo LOLO (major alarm) lower limit
     *  @param lo   LO   (minor warning) lower limit
     *  @param hi   HI   (minor warning) upper limit
     *  @param hihi HIHI (major alarm) upper limit
     */
    public void setLimits(final double lolo, final double lo,
                          final double hi,   final double hihi)
    {
        limit_lolo = lolo;
        limit_lo   = lo;
        limit_hi   = hi;
        limit_hihi = hihi;
        requestUpdate();
    }

    /** Set the colors used to draw alarm limit lines.
     *  @param minor color for LO / HI (minor warning) lines
     *  @param major color for LOLO / HIHI (major alarm) lines
     */
    public void setAlarmColors(final javafx.scene.paint.Color minor,
                               final javafx.scene.paint.Color major)
    {
        limit_minor_color = GraphicsUtils.convert(Objects.requireNonNull(minor));
        limit_major_color = GraphicsUtils.convert(Objects.requireNonNull(major));
        requestUpdate();
    }

    /** Indicate whether the current alarm limits come from PV metadata.
     *  When {@code false} the limit lines are drawn dashed to signal that
     *  they are manually configured and do not reflect live alarm state.
     *  @param from_pv {@code true} for solid (PV-sourced), {@code false} for dashed
     */
    public void setLimitsFromPV(final boolean from_pv)
    {
        limits_from_pv = from_pv;
        requestUpdate();
    }

    /** Show or hide a second scale on the opposite (right) side of the tank.
     *  Both scales share the same range, log mode, format and tick settings.
     *  @param visible {@code true} to show, {@code false} to hide (default)
     */
    public void setRightScaleVisible(final boolean visible)
    {
        if (right_scale_visible == visible)
            return;
        right_scale_visible = visible;
        need_layout.set(true);
        requestUpdate();
    }

    /** @param perpendicular Draw tick labels perpendicular to the axis direction?
     *                       When {@code false}, labels are rotated parallel to the axis.
     */
    public void setPerpendicularTickLabels(final boolean perpendicular)
    {
        scale.setPerpendicularTickLabels(perpendicular);
        right_scale.setPerpendicularTickLabels(perpendicular);
        need_layout.set(true);   // scale width changes between rotated and perpendicular modes
        requestUpdate();
    }

    /** Set value range
     *  @param low Lower limit
     *  @param high Upper limit
     */
    public void setRange(final double low, final double high)
    {
        // Guard against NaN, Infinite, or inverted/flat range
        if (!Double.isFinite(low) || !Double.isFinite(high) || low >= high)
            return;
        scale.setValueRange(low, high);
        right_scale.setValueRange(low, high);
    }

    /** @param value Set value */
    public void setValue(final double value)
    {
        if (Double.isFinite(value))
            this.value = value;
        else
            this.value = scale.getValueRange().getLow();
        requestUpdate();
    }

    /** Map a value to a Y pixel within the plot bounds (low value at bottom).
     *  Returns -1 when the mapping is undefined (e.g. log scale with non-positive inputs).
     */
    private int valueToY(final Rectangle pb, final double min, final double max,
                         final double v, final boolean logscale)
    {
        final double frac;
        if (logscale)
        {
            if (min <= 0 || max <= 0 || v <= 0)
                return -1;
            frac = Math.log(v / min) / Math.log(max / min);
        }
        else
            frac = (v - min) / (max - min);
        return (int) (pb.y + pb.height * (1.0 - frac));
    }

    /** Draw a single horizontal limit line across the tank area at the given value. */
    private void drawLimitLineAt(final Graphics2D gc, final Rectangle pb,
                                  final double min, final double max,
                                  final double limit, final Color color)
    {
        if (!Double.isFinite(limit) || limit <= min || limit >= max)
            return;
        final int y = valueToY(pb, min, max, limit, scale.isLogarithmic());
        if (y < pb.y || y > pb.y + pb.height)
            return;
        gc.setColor(color);
        gc.drawLine(pb.x, y, pb.x + pb.width, y);
    }

    /** Compute the fill height in pixels for the current value.
     *  Handles both linear and logarithmic scales.
     *
     *  @param plotHeight Pixel height of the plot area
     *  @param min        Scale minimum (&lt; max)
     *  @param max        Scale maximum
     *  @param current    Current PV value
     *  @param logscale   Whether the scale uses log spacing
     *  @return Fill level in pixels: 0 = empty, plotHeight = full
     */
    private static int computeFillLevel(final int plotHeight, final double min, final double max,
                                        final double current, final boolean logscale)
    {
        if (current <= min)
            return 0;
        if (current >= max)
            return plotHeight;
        if (logscale) // by mellguth2, https://github.com/ControlSystemStudio/phoebus/issues/2726
        {   // Refuse to map if any input is non-positive (log undefined)
            if (min <= 0 || max <= 0 || current <= 0)
                return 0;
            return (int) (plotHeight * Math.log(current / min) / Math.log(max / min));
        }
        return (int) (plotHeight * (current - min) / (max - min) + 0.5);
    }

    /** @return Is at least one alarm limit line configured? */
    private boolean hasLimitLines()
    {
        return !Double.isNaN(limit_lolo) || !Double.isNaN(limit_lo) ||
               !Double.isNaN(limit_hi)   || !Double.isNaN(limit_hihi);
    }

    /** @return Stroke for limit lines: solid for limits from the PV,
     *          dashed for manually configured ones */
    private Stroke limitLineStroke()
    {
        if (limits_from_pv)
            return new BasicStroke(2f);
        return new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                               10f, new float[] { 6f, 4f }, 0f);
    }

    // Thermometer look. Sizes in pixels.

    /** The tube widens with the widget up to a cap, as in the stock thermometer */
    private static final int TUBE_MIN_WIDTH = 6;
    private static final int TUBE_MAX_WIDTH = 20;

    /** Tube height kept above the bulb, so the bulb never swallows the tube */
    private static final int TUBE_MIN_HEIGHT = 8;

    /** The bulb is always at least this much wider than the tube */
    private static final int BULB_MIN_OVERHANG = 6;

    /** Gap between a scale and the tube wall */
    private static final int SCALE_GAP = 3;

    /** Margin between the thermometer and the edge of the widget */
    private static final int THERMO_MARGIN = 3;

    /** Compute layout of plot components.
     *  Supports independent left and right scales; the plot area sits
     *  between them.  A 1-pixel inset is added on any edge that has no
     *  scale so the outline {@code drawRoundRect} is not clipped.
     */
    private void computeLayout(final Graphics2D gc, final Rectangle bounds)
    {
        if (thermometerStyle)
        {
            computeThermoLayout(gc, bounds);
            return;
        }

        int left_width  = 0;
        int right_width = 0;
        int[] ends = { 0, 0 };   // [bottom gap, top gap]

        if (scale_visible)
        {
            left_width = scale.getDesiredPixelSize(bounds, gc);
            ends = scale.getPixelGaps(gc);
        }
        if (right_scale_visible)
        {
            right_width = right_scale.getDesiredPixelSize(bounds, gc);
            final int[] r_ends = right_scale.getPixelGaps(gc);
            ends[0] = Math.max(ends[0], r_ends[0]);
            ends[1] = Math.max(ends[1], r_ends[1]);
        }

        // Inset = ceil(border_width/2) keeps the outer stroke edge inside the canvas.
        // On sides with a scale the label area provides ample margin so inset=0.
        // When there is no border, inset=1 is the original clip guard.
        // innerPadding is added on all four sides regardless of scale presence.
        // A bar track grows back into it, so next to a scale it keeps a gap
        // for the border and one pixel, clear of the axis line.
        final int half_bw_ceil = (border_width + 1) / 2;
        final int padding = innerPadding;
        final int scaleGap = barTrack ? half_bw_ceil + 1 : 0;
        final int inset_left   = (left_width  == 0) ? Math.max(1, half_bw_ceil) + padding : padding + scaleGap;
        final int inset_right  = (right_width == 0) ? Math.max(1, half_bw_ceil) + padding : padding + scaleGap;
        final int inset_top    = (ends[1] == 0) ? Math.max(1, half_bw_ceil) + padding : padding;
        final int inset_bottom = (ends[0] == 0) ? Math.max(1, half_bw_ceil) + padding : padding;

        final int top    = bounds.y + ends[1] + inset_top;
        final int height = bounds.height - ends[0] - ends[1] - inset_top - inset_bottom;

        if (scale_visible)
            scale.setBounds(new Rectangle(bounds.x, top, left_width, height));
        if (right_scale_visible)
            right_scale.setBounds(new Rectangle(bounds.x + bounds.width - right_width,
                                                top, right_width, height));

        plot_area.setBounds(bounds.x + left_width + inset_left, top,
                            bounds.width - left_width - right_width - inset_left - inset_right, height);
    }

    /** Thermometer layout
     *
     *  <p>Unlike the tank, the scales sit right next to the narrow tube and
     *  only span the tube, not the bulb. Scales, tube and bulb are centered
     *  as a whole in the available width.
     *  The geometry is kept in {@link #thermoGeom} so that painting and
     *  the scale transform use the same numbers.
     */
    private void computeThermoLayout(final Graphics2D gc, final Rectangle bounds)
    {
        final ScaleSpace space = measureThermoScales(gc, bounds);
        final ThermoGeom geom = thermoGeometry(bounds, space);
        placeThermoScales(geom, space);

        // Plot area is the bounding box of tube and bulb, the reference for scale.paint()
        final double bulbBottom = geom.bulbCenterY() + geom.bulbRadius();
        plot_area.setBounds((int) Math.round(geom.centerX() - geom.bulbRadius()),
                            (int) Math.round(geom.tubeTop()),
                            (int) Math.round(geom.bulbDiameter()),
                            (int) Math.round(bulbBottom - geom.tubeTop()));
        thermoGeom = geom;
    }

    /** @return Space taken by the visible scales */
    private ScaleSpace measureThermoScales(final Graphics2D gc, final Rectangle bounds)
    {
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        if (scale_visible)
        {
            left = scale.getDesiredPixelSize(bounds, gc);
            final int[] gaps = scale.getPixelGaps(gc);   // [bottom, top]
            bottom = gaps[0];
            top = gaps[1];
        }
        if (right_scale_visible)
        {
            right = right_scale.getDesiredPixelSize(bounds, gc);
            final int[] gaps = right_scale.getPixelGaps(gc);
            bottom = Math.max(bottom, gaps[0]);
            top = Math.max(top, gaps[1]);
        }
        return new ScaleSpace(left, right, top, bottom);
    }

    /** Size and place tube and bulb in the space left by the scales
     *  @param bounds Canvas area
     *  @param space Space reserved for the scales
     *  @return Thermometer geometry
     */
    ThermoGeom thermoGeometry(final Rectangle bounds, final ScaleSpace space)
    {
        // Vertical extent, leaving room for the label overhang and the outline stroke
        final int halfOutline = (border_width + 1) / 2;
        final double inset = THERMO_MARGIN + halfOutline + 1.0;
        final double top = bounds.y + space.top() + inset;
        final double bottom = Math.max(top + 1.0, bounds.y + bounds.height - space.bottom() - inset);
        final double height = bottom - top;

        // Width left for tube and bulb
        final int leftSpace = space.left() > 0 ? space.left() + SCALE_GAP : 0;
        final int rightSpace = space.right() > 0 ? space.right() + SCALE_GAP : 0;
        final double width = Math.max(TUBE_MIN_WIDTH,
                                      bounds.width - 2.0 * THERMO_MARGIN - leftSpace - rightSpace);

        // Tube takes half the width, capped, as in the stock thermometer
        final double tubeWidth = Math.clamp(width / 2, TUBE_MIN_WIDTH, TUBE_MAX_WIDTH);

        // Bulb is wider than the tube, but must fit the remaining width and height
        double bulbDiameter = tubeWidth + bulbSize;
        bulbDiameter = Math.min(bulbDiameter, Math.min(width, height - TUBE_MIN_HEIGHT));
        bulbDiameter = Math.max(bulbDiameter, tubeWidth + BULB_MIN_OVERHANG);
        final double bulbRadius = bulbDiameter / 2;

        // Center the assembly. On each side, the scale or the bulb reaches
        // out from the tube center, whichever is wider. A widget that is
        // narrower than that keeps tube and bulb in view and clips the scale.
        final double leftExtent = Math.max(tubeWidth / 2 + leftSpace, bulbRadius);
        final double rightExtent = Math.max(tubeWidth / 2 + rightSpace, bulbRadius);
        final double centered = bounds.x + (bounds.width - leftExtent - rightExtent) / 2 + leftExtent;
        final double centerX = Math.max(bounds.x + THERMO_MARGIN + bulbRadius,
                                        Math.min(centered, bounds.x + bounds.width - THERMO_MARGIN - bulbRadius));

        // The tube ends where its walls meet the bulb circle, never above its own top
        final double bulbCenterY = bottom - bulbRadius;
        final double halfChord = Math.min(tubeWidth / 2, bulbRadius - 0.001);
        final double tubeBottom = Math.max(top,
                bulbCenterY - Math.sqrt(bulbRadius * bulbRadius - halfChord * halfChord));

        return new ThermoGeom(centerX, tubeWidth, top, tubeBottom, bulbCenterY, bulbRadius);
    }

    /** Place the scales flush against the tube walls, spanning only the tube.
     *  The left scale is always positioned, even when hidden, because its
     *  value transform maps the liquid level and the limit lines onto the tube. */
    private void placeThermoScales(final ThermoGeom geom, final ScaleSpace space)
    {
        final int y = (int) Math.round(geom.tubeTop());
        final int height = Math.max(1, (int) Math.round(geom.tubeBottom() - geom.tubeTop()));
        scale.setBounds(new Rectangle((int) Math.round(geom.tubeLeft() - SCALE_GAP - space.left()),
                                      y, space.left(), height));
        if (right_scale_visible)
            right_scale.setBounds(new Rectangle((int) Math.round(geom.tubeRight() + SCALE_GAP),
                                                y, space.right(), height));
    }

    /** Draw the thermometer from the geometry of the last layout:
     *  empty tube, liquid, limit lines and glass outline */
    private void drawThermometer(final Graphics2D gc, final double min, final double max, final double current)
    {
        final ThermoGeom geom = thermoGeom;
        if (geom == null)
            return;
        final int arc = (int) Math.max(2, geom.tubeWidth() * 0.6);

        paintEmptyTube(gc, geom, arc);
        paintLiquid(gc, geom, arc, liquidLevel(geom, min, max, current));
        paintThermoLimits(gc, geom, min, max);
        paintGlassOutline(gc, geom, arc);
    }

    /** @return Y coordinate of the liquid surface, taken from the scale so it
     *          lines up with the tick marks, and clamped to the tube */
    private double liquidLevel(final ThermoGeom geom, final double min, final double max,
                               final double current)
    {
        // Like the tank, show NaN as empty. Values far outside the range
        // would overflow the integer screen coordinate, so limit them first.
        if (Double.isNaN(current))
            return geom.tubeBottom();
        final double inRange = Math.clamp(current, min, max);
        return Math.clamp(scale.getScreenCoord(inRange), geom.tubeTop(), geom.tubeBottom());
    }

    /** Paint the tube in the empty color, shaded like the tank */
    private void paintEmptyTube(final Graphics2D gc, final ThermoGeom geom, final int arc)
    {
        gc.setPaint(new GradientPaint((float) geom.tubeLeft(), 0, empty,
                                      (float) geom.centerX(), 0, empty_shadow, true));
        gc.fillRoundRect((int) Math.round(geom.tubeLeft()), (int) Math.round(geom.tubeTop()),
                         (int) Math.round(geom.tubeWidth()),
                         (int) Math.round(geom.tubeBottom() - geom.tubeTop()),
                         arc, arc);
    }

    /** Paint the bulb, which is always full, and the liquid column up to {@code level} */
    private void paintLiquid(final Graphics2D gc, final ThermoGeom geom, final int arc, final double level)
    {
        gc.setPaint(new GradientPaint((float) geom.tubeLeft(), 0, fill,
                                      (float) geom.centerX(), 0, fill_highlight, true));
        final int bulbDiameter = (int) Math.round(geom.bulbDiameter());
        gc.fillOval((int) Math.round(geom.centerX() - geom.bulbRadius()),
                    (int) Math.round(geom.bulbCenterY() - geom.bulbRadius()),
                    bulbDiameter, bulbDiameter);
        if (level < geom.tubeBottom())
            gc.fillRoundRect((int) Math.round(geom.tubeLeft()), (int) Math.round(level),
                             (int) Math.round(geom.tubeWidth()),
                             (int) Math.round(geom.tubeBottom() - level) + arc,
                             arc, arc);
    }

    /** Paint the alarm limit lines across the tube */
    private void paintThermoLimits(final Graphics2D gc, final ThermoGeom geom,
                                   final double min, final double max)
    {
        if (!hasLimitLines())
            return;
        gc.setStroke(limitLineStroke());
        drawThermoLimit(gc, geom, min, max, limit_lolo, limit_major_color);
        drawThermoLimit(gc, geom, min, max, limit_lo,   limit_minor_color);
        drawThermoLimit(gc, geom, min, max, limit_hi,   limit_minor_color);
        drawThermoLimit(gc, geom, min, max, limit_hihi, limit_major_color);
        gc.setStroke(new BasicStroke(1f));
    }

    /** Draw one limit line across the tube, placed via the scale so that it
     *  matches the tick marks. Limits outside the range are skipped. */
    private void drawThermoLimit(final Graphics2D gc, final ThermoGeom geom,
                                 final double min, final double max,
                                 final double limit, final Color color)
    {
        if (!Double.isFinite(limit) || limit <= min || limit >= max)
            return;
        final int y = scale.getScreenCoord(limit);
        if (y < geom.tubeTop() || y > geom.tubeBottom())
            return;
        gc.setColor(color);
        gc.drawLine((int) Math.round(geom.tubeLeft()), y, (int) Math.round(geom.tubeRight()), y);
    }

    /** Paint the glass outline: tube walls, rounded top and the bulb arc.
     *  Nothing is drawn for border width 0. */
    private void paintGlassOutline(final Graphics2D gc, final ThermoGeom geom, final int arc)
    {
        if (border_width <= 0)
            return;
        final double left = geom.tubeLeft();
        final double right = geom.tubeRight();
        final double top = geom.tubeTop();
        final double bottom = geom.tubeBottom();

        final Path2D.Double outline = new Path2D.Double();
        outline.moveTo(left, bottom);
        outline.lineTo(left, top + arc / 2.0);
        outline.quadTo(left, top, left + arc / 2.0, top);
        outline.lineTo(right - arc / 2.0, top);
        outline.quadTo(right, top, right, top + arc / 2.0);
        outline.lineTo(right, bottom);
        // Around the bulb, from the right wall back to the left wall
        final double dy = bottom - geom.bulbCenterY();
        final double angleRight = Math.toDegrees(Math.atan2(-dy, right - geom.centerX()));
        final double angleLeft  = Math.toDegrees(Math.atan2(-dy, left - geom.centerX()));
        outline.append(new Arc2D.Double(geom.centerX() - geom.bulbRadius(),
                                        geom.bulbCenterY() - geom.bulbRadius(),
                                        geom.bulbDiameter(), geom.bulbDiameter(),
                                        angleRight, angleLeft - angleRight - 360, Arc2D.OPEN),
                       true);
        outline.closePath();

        gc.setColor(foreground);
        gc.setStroke(new BasicStroke(border_width));
        gc.draw(outline);
        gc.setStroke(new BasicStroke(1f));
    }

    /** Draw all components into image buffer */
    protected Image updateImageBuffer()
    {
        final Rectangle area_copy = area;
        if (area_copy.width <= 0  ||  area_copy.height <= 0)
            return null;

        final BufferUtil buffer = BufferUtil.getBufferedImage(area_copy.width, area_copy.height);
        if (buffer == null)
            return null;
        final BufferedImage image = buffer.getImage();
        final Graphics2D gc = buffer.getGraphics();

        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        if (need_layout.getAndSet(false))
            computeLayout(gc, area_copy);

        final Rectangle plot_bounds = plot_area.getBounds();

        gc.setColor(background);
        gc.fillRect(0, 0, area_copy.width, area_copy.height);

        if (scale_visible)
            scale.paint(gc, plot_bounds);
        if (right_scale_visible)
            right_scale.paint(gc, plot_bounds);

        plot_area.paint(gc);

        final AxisRange<Double> range = scale.getValueRange();
        final double min = Math.min(range.getLow(), range.getHigh());
        final double max = Math.max(range.getLow(), range.getHigh());
        final double current = value;
        if (thermometerStyle)
            drawThermometer(gc, min, max, current);
        else
            drawTank(gc, plot_bounds, min, max, current, range.getLow() <= range.getHigh());

        gc.dispose();

        // Convert to JFX
        return SwingFXUtils.toFXImage(image, null);
    }

    /** Draw the tank body: track, fill level, optional border and limit lines
     *  @param normal Range runs bottom-up? Otherwise the tank fills from the top
     */
    private void drawTank(final Graphics2D gc, final Rectangle plotBounds,
                          final double min, final double max, final double current,
                          final boolean normal)
    {
        // A widget too small for its padding and scale has no room for the
        // fill, so draw no body rather than one that looks empty
        if (plotBounds.width <= 0  ||  plotBounds.height <= 0)
            return;

        final int level = computeFillLevel(plotBounds.height, min, max, current, scale.isLogarithmic());
        final int arc = Math.min(plotBounds.width, plotBounds.height) / 10;
        // The body is the tank, or the track of a bar. The track extends into
        // the inner padding so that the fill sits inside it, like in the
        // JavaFX progress bar.
        final Rectangle body = new Rectangle(plotBounds);
        final int bodyArc;
        final int fillArc;
        if (barTrack)
        {
            body.grow(innerPadding, innerPadding);
            bodyArc = FRAME_ARC;
            fillArc = TRACK_ARC;
            paintBarTrack(gc, body);
        }
        else
        {
            bodyArc = fillArc = arc;
            final int center = plotBounds.x + plotBounds.width/2;
            gc.setPaint(new GradientPaint(plotBounds.x, 0, empty, center, 0, empty_shadow, true));
            gc.fillRoundRect(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height, arc, arc);
        }

        gc.setPaint(fillPaint(plotBounds));
        if (normal)
            gc.fillRoundRect(plotBounds.x, plotBounds.y+plotBounds.height-level, plotBounds.width, level, fillArc, fillArc);
        else
            gc.fillRoundRect(plotBounds.x, plotBounds.y, plotBounds.width, level, fillArc, fillArc);

        // Optional border: stroked CENTRED on the body. The inner half of the
        // stroke covers the fill edge (no gap); the outer half extends beyond
        // the body into the inset margin. For a tank, ticks land at the body
        // edges, the centre of the border stroke, matching the CS-Studio BOY
        // convention.
        if (border_width > 0)
        {
            // Java2D: fillRoundRect covers x..x+w-1, drawRoundRect strokes x..x+w.
            // Using w-1, h-1 aligns the stroke centre with the fill boundary,
            // making all four edges symmetric.
            gc.setColor(foreground);
            gc.setStroke(new BasicStroke(border_width));
            gc.drawRoundRect(body.x, body.y, body.width - 1, body.height - 1,
                             bodyArc, bodyArc);
            gc.setStroke(new BasicStroke(1f));
        }

        if (normal && hasLimitLines())
        {
            gc.setStroke(limitLineStroke());
            drawLimitLineAt(gc, plotBounds, min, max, limit_lolo, limit_major_color);
            drawLimitLineAt(gc, plotBounds, min, max, limit_lo,   limit_minor_color);
            drawLimitLineAt(gc, plotBounds, min, max, limit_hi,   limit_minor_color);
            drawLimitLineAt(gc, plotBounds, min, max, limit_hihi, limit_major_color);
            gc.setStroke(new BasicStroke(1f));
        }
    }

    /** Request a complete redraw of the plot */
    final public void requestUpdate()
    {
        needUpdate.set(true);
        update_throttle.trigger();
    }

    /** Redraw the current image and cursors
     *  <p>May be called from any thread.
     */
    final void redrawSafely()
    {
        Platform.runLater(redraw_runnable);
    }

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {   // Stop updates which could otherwise still use
        // what's about to be disposed
        update_throttle.dispose();
    }
}
