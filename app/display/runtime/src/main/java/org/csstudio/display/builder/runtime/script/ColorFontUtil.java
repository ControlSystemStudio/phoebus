package org.csstudio.display.builder.runtime.script;

import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.logging.Level;



public class ColorFontUtil {

    /** the color of black */
    final static public WidgetColor BLACK = new WidgetColor(0, 0, 0);

    /** the color of blue */
    final static public WidgetColor BLUE = new WidgetColor(0, 0, 255);

    /** the color of cyan */
    final static public WidgetColor CYAN = new WidgetColor(0, 255, 255);

    /** the color of dark gray */
    final static public WidgetColor DARK_GRAY = new WidgetColor(150, 150, 150);

    /** the color of gray */
    final static public WidgetColor GRAY = new WidgetColor(200, 200, 200);

    /** the color of green */
    final static public WidgetColor GREEN = new WidgetColor(0, 255, 0);

    /** the color of light blue */
    final static public WidgetColor LIGHT_BLUE = new WidgetColor(153, 186, 243);

    /** the color of orange */
    final static public WidgetColor ORANGE = new WidgetColor(255, 128, 0);

    /** the color of pink */
    final static public WidgetColor PINK = new WidgetColor(255, 0, 255);

    /** the color of orange */
    final static public WidgetColor PURPLE = new WidgetColor(128, 0, 255);

    /** the color of red */
    final static public WidgetColor RED = new WidgetColor(255, 0, 0);

    /** the color of white */
    final static public WidgetColor WHITE = new WidgetColor(255, 255, 255);

    /** the color of yellow */
    final static public WidgetColor YELLOW = new WidgetColor(255, 255, 0);

    static
    {
	logger.log(Level.INFO, "Script accessed ColorFontUtil. Update to use org.csstudio.display.builder.model.properties.WigetColor/WidgetFont.");
    }

    /**
     * Get a color with the given
     * red, green and blue values.
     *
     * @param red the red component of the new instance
     * @param green the green component of the new instance
     * @param blue the blue component of the new instance
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_INVALID_ARGUMENT - if the red, green or blue argument is not between 0 and 255</li>
     * </ul>
     */
    public final static WidgetColor getColorFromRGB(int red, int green, int blue){
        return new WidgetColor(red, green, blue);
    }

    /**
     * Get a new font data given a font name,
     * the height of the desired font in points,
     * and a font style.
     *
     * @param name the name of the font (must not be null)
     * @param height the font height in points
     * @param style A bitwise combination of NORMAL(0), BOLD(1) and ITALIC(2).
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_NULL_ARGUMENT - when the font name is null</li>
     *    <li>ERROR_INVALID_ARGUMENT - if the height is negative</li>
     * </ul>
     */
    public final static WidgetFont getFont(String name, int height, int style){
	WidgetFontStyle style_str;
	if (style == 1)
	{
	    style_str = WidgetFontStyle.BOLD;;
	}
	else if (style == 2)
	{
	    style_str = WidgetFontStyle.ITALIC;
	}
	else
	{
	    style_str = WidgetFontStyle.REGULAR;
	}
        return new WidgetFont(name, style_str, height);
    }
}
