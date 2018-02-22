/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.HorizontalAlignment;
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
            CommonFonts.install();
            font_calibration = new JFXFontCalibration().getCalibrationFactor();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot initialize Java FX", ex);
            font_calibration = 1.0;
        }
    }

    /** Convert model color into JFX color
     *  @param color {@link WidgetColor}
     *  @return {@link Color}
     */
    public static Color convert(final WidgetColor color)
    {
        return Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()/255.0);
    }

    /** Convert model color into web-type RGB text
     *  @param color {@link WidgetColor}
     *  @return RGB text of the form "#FF8080"
     */
    public static String webRGB(final WidgetColor color)
    {
        return appendWebRGB(new StringBuilder(), color).toString();
    }

    /** Convert model color into web-type RGB text
     *  @param buf StringBuilder where RGB text of the form "#FF8080" is added
     *  @param color {@link WidgetColor}
     *  @return {@link StringBuilder}
     */
    public static StringBuilder appendWebRGB(final StringBuilder buf, final WidgetColor color)
    {
        if (color.getAlpha() < 255)
            buf.append("rgba(").append(color.getRed()).append(',')
                               .append(color.getGreen()).append(',')
                               .append(color.getBlue()).append(',')
                               .append(color.getAlpha()/255f).append(')');
        else
        {
            buf.append('#');
            addHex(buf, color.getRed());
            addHex(buf, color.getGreen());
            addHex(buf, color.getBlue());
        }
        return buf;
    }

    private static final char[] hex_chars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    private static void addHex(final StringBuilder buf, final int number)
    {
        buf.append(hex_chars[number / 16]);
        buf.append(hex_chars[number % 16]);
    }

    /** Convert model color into CSS style string for shading tabs, buttons, etc
     *  @param color {@link WidgetColor}
     *  @return style string of the form "-fx-color: ... -fx-outer-border: ... -fx-inner-border: ... -fx-background: ..."
     */
    public static String shadedStyle(final WidgetColor color)
    {
        // How to best set colors?
        // Content Pane can be set in API, but Tab has no usable 'set color' API.
        // TabPane has setBackground(), but in "floating" style that would be
        // the background behind the tabs, which is usually transparent.
        // modena.css of JDK8 reveals a structure of sub-items which are shaded with gradients based
        // on  -fx-color for the inactive tabs,
        //     -fx-outer-border and -fx-inner-border for the, well, border,
        // and -fx-background for the selected tab,
        // so re-define those.
        final String bg = webRGB(color);

        return  "-fx-color: derive(" + bg + ", 50%);" +
        "-fx-outer-border: derive(" + bg + ", -23%);" +
        "-fx-inner-border: linear-gradient(to bottom," +
        "ladder(" + bg + "," +
        "       derive(" + bg + ",30%) 0%," +
        "       derive(" + bg + ",20%) 40%," +
        "       derive(" + bg + ",25%) 60%," +
        "       derive(" + bg + ",55%) 80%," +
        "       derive(" + bg + ",55%) 90%," +
        "       derive(" + bg + ",75%) 100%" +
        ")," +
        "ladder(" + bg + "," +
        "       derive(" + bg + ",20%) 0%," +
        "       derive(" + bg + ",10%) 20%," +
        "       derive(" + bg + ",5%) 40%," +
        "       derive(" + bg + ",-2%) 60%," +
        "       derive(" + bg + ",-5%) 100%" +
        "));" +
        "-fx-background: " + bg + ";";
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
        final double calibrated = font.getSize() * font_calibration;
        switch (font.getStyle())
        {
        case BOLD:
            return Font.font(font.getFamily(), FontWeight.BOLD,   FontPosture.REGULAR, calibrated);
        case ITALIC:
            return Font.font(font.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC,  calibrated);
        case BOLD_ITALIC:
            return Font.font(font.getFamily(), FontWeight.BOLD,   FontPosture.ITALIC,  calibrated);
        default:
            return Font.font(font.getFamily(), FontWeight.NORMAL, FontPosture.REGULAR, calibrated);
        }
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
}
