/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.ui.javafx.ImageCache;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/** JavaFX Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXUtil extends org.phoebus.ui.javafx.JFXUtil
{
    private static double font_calibration = 1.0;

    static
    {
        try
        {
            font_calibration = new JFXFontCalibration().getCalibrationFactor();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot initialize Java FX", ex);
            font_calibration = 1.0;
        }
    }

    private static final ConcurrentHashMap<WidgetColor, Color> colorCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<WidgetColor, String> webRGBCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<WidgetColor, String> shadedStyleCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<WidgetFont, Font> fontCache = new ConcurrentHashMap<>();

    /** Convert model color into JFX color
     *  @param color {@link WidgetColor}
     *  @return {@link Color}
     */
    public static Color convert(final WidgetColor color)
    {
        return colorCache.computeIfAbsent(color, col ->
            Color.rgb(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()/255.0));
    }

    /** Convert model color into web-type RGB text
     *  @param col {@link WidgetColor}
     *  @return RGB text of the form "#FF8080"
     */
    public static String webHex(final WidgetColor col) {
        if(col != null) {
            return String.format((Locale) null, "#%02X%02X%02X", col.getRed(), col.getGreen(), col.getBlue());
        } else {
            return "";
        }
    }

    /** Convert model color into web-type RGB text if transparent; otherwise converts to hex.
     *  @param color {@link WidgetColor}
     *  @return RGB text of the form "#FF8080"
     */
    public static String webRgbOrHex(final WidgetColor color)
    {
        return webRGBCache.computeIfAbsent(color, col ->
        {
            if (col.getAlpha() < 255)
                return "rgba(" + col.getRed() + ',' +
                                 col.getGreen() + ',' +
                                 col.getBlue() + ',' +
                                 col.getAlpha()/255f + ')';
            else
                return webHex(col);
        });
    }

    /** Convert model color into web-type RGB text
     *  @param buf StringBuilder where RGB text of the form "#FF8080" is added
     *  @param color {@link WidgetColor}
     *  @return {@link StringBuilder}
     */
    public static StringBuilder appendWebRGB(final StringBuilder buf, final WidgetColor color)
    {
        return buf.append(webRgbOrHex(color));
    }

    /** Convert model color into CSS style string for shading tabs, buttons, etc
     *  @param color {@link WidgetColor}
     *  @return style string of the form "-fx-base: ..;"
     */
    public static String shadedStyle(final WidgetColor color)
    {
        return shadedStyleCache.computeIfAbsent(color, col ->
        {
            // How to best set colors?
            // Content Pane can be set in API, but Button and Tab have no usable 'set color' API.
            // TabPane has setBackground(), but in "floating" style that would be
            // the background behind the tabs, which is usually transparent.
            //
            // Adjusting the style can break when the underlying style sheet changes,
            // but since at least JDK8 that has been modena.css with little changes until JFX 18,
            // which can be found in javafx-controls-18-linux.jar as
            // com/sun/javafx/scene/control/skin/modena/modena.css
            //
            // Buttons use nested -fx-background-color entries
            // -fx-shadow-highlight-color, -fx-outer-border, -fx-inner-border, -fx-body-color
            // with associated ..-insets and ..-radius which are all based on -fx-base,
            // so redefine that to adjust the overall color.
            // The .button.armed state uses
            //   -fx-pressed-base: derive(-fx-base,-6%);
            // which we change into a more obvious variant.
            final String bg = webRgbOrHex(col);
            return "-fx-base: " + bg + "; " +
                   "-fx-pressed-base: derive(-fx-base,-25%);";
        });
    }

    /** Convert JFX color into model color
     *  @param color {@link Color}
     *  @return {@link WidgetColor}
     */
    public static WidgetColor convert(final Color color)
    {
        return new WidgetColor((int) (color.getRed() * 255),
                               (int) (color.getGreen() * 255),
                               (int) (color.getBlue() * 255),
                               (int) (color.getOpacity() * 255));
    }

    /** Convert model font into JFX font
     *  @param font {@link WidgetFont}
     *  @return {@link Font}
     */
    public static Font convert(final WidgetFont font)
    {
        return fontCache.computeIfAbsent(font, f ->
        {
            final double calibrated = f.getSize() * font_calibration;
            switch (f.getStyle())
            {
            case BOLD:
                return Font.font(f.getFamily(), FontWeight.BOLD,   FontPosture.REGULAR, calibrated);
            case ITALIC:
                return Font.font(f.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC,  calibrated);
            case BOLD_ITALIC:
                return Font.font(f.getFamily(), FontWeight.BOLD,   FontPosture.ITALIC,  calibrated);
            default:
                return Font.font(f.getFamily(), FontWeight.NORMAL, FontPosture.REGULAR, calibrated);
            }
        });
    }

    /** Convert font to Java FX "-fx-font" shorthand form; e.g.
     * [[ <font-style> || <font-weight> ]? <font-size> <font-family> ]
     * per https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html#typefont
     *  @param prefix Typically "-fx-font"
     *  @param font {@link Font}
     *  @return "-fx-font: italic 64px 'Source Sans Pro';" (recall many-word fonts must be surrounded by single quotes)
     */
    public static String cssFontShorthand(final String prefix, final Font font) {
        final StringBuilder buf = new StringBuilder();
        buf.append(prefix).append(": ");
        switch (font.getStyle())
        {
            case "Bold":
                buf.append("bold ");
                break;
            case "Italic":
                buf.append("italic ");
                break;
            case "Bold Italic":
                buf.append("bold italic ");
                break;
            default:
                buf.append("normal ");
        }
        buf.append((int)font.getSize()).append("px ");
        buf.append("'").append(font.getFamily()).append("';");
        return buf.toString();
    }

    /** Convert font to Java FX "-fx-font-*"
     *  @param prefix Typically "-fx-font"
     *  @param font {@link Font}
     *  @return "-fx-font-*: ..."
     */
    public static String cssFont(final String prefix, final Font font)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(prefix).append("-size: ").append((int)font.getSize()).append("px;");
        buf.append(prefix).append("-family: \"").append(font.getFamily()).append("\";");
        switch (font.getStyle())
        {
        case "Bold":
            buf.append(prefix).append("-weight: bold;");
            break;
        case "Italic":
            buf.append(prefix).append("-style: italic;");
            break;
        case "Bold Italic":
            buf.append(prefix).append("-weight: bold;");
            buf.append(prefix).append("-style: italic;");
            break;
        default:
            // Skip the "-fx-font-*: normal"
        }
        return buf.toString();
    }

    /** @param name Name of icon in this plugin
     *  @return {@link ImageView}
     */
    public static ImageView getIcon(final String name)
    {
        final String image_path = "/icons/" + name;
        return ImageCache.getImageView(JFXUtil.class, image_path);
    }

    /** Compute JFX alignment 'Pos' from widget properties
     *  @param horiz {@link HorizontalAlignment}
     *  @param vert {@link VerticalAlignment}
     *  @return {@link Pos}
     */
    public static Pos computePos(final HorizontalAlignment horiz, final VerticalAlignment vert)
    {
        // This depends on the order of 'Pos' and uses Pos.BOTTOM_*, not Pos.BASELINE_*.
        // Could use if/switch orgy to be independent from 'Pos' ordinals.
        return Pos.values()[vert.ordinal() * 3 + horiz.ordinal()];
    }

    /**returns double[] array for given line style scaled by line_width
     *
     * @param style - actual line style
     * @param line_width - actual line width
     * @return double[] with segment lengths
     */
    public static Double[] getDashArray(LineStyle style, double line_width)
    {
        final Double seg_short = line_width > 4. || style == LineStyle.DOT ? line_width : 4.;
        final Double seg_long  = line_width > 4. ? 3. * line_width : 12.;
        switch (style)
        {
        case DASH:
            return new Double[] {seg_long, seg_short};
        case DOT:
            return new Double[] {seg_short, seg_short};
        case DASHDOT:
            return new Double[] {seg_long, seg_short,
                                 seg_short, seg_short};
        case DASHDOTDOT:
            return new Double[] {seg_long, seg_short,
                                 seg_short, seg_short,
                                 seg_short, seg_short};
        case SOLID:
        default:
            return new Double[] {/* Nothing for solid line */};
        }
    }
}
