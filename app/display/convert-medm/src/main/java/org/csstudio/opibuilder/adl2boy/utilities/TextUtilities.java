/**
 *
 */
package org.csstudio.opibuilder.adl2boy.utilities;


import static org.csstudio.display.converter.medm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.ITextWidget;
import org.phoebus.ui.vtype.FormatOption;

/**
 * @author hammonds
 *
 */
public class TextUtilities {

    /**
     *
     */
    public static void setWidgetFont(final Widget textModel)
    {
        final WidgetProperty<WidgetFont> font_prop = textModel.getProperty(CommonWidgetProperties.propFont);
        final WidgetFont font = font_prop.getValue();
        int fontSize = convertTextHeightToFontSize(textModel.propHeight().getValue());
        // fontSize = fontSize-(fontSize/10)*3;

        final WidgetFont new_font = new WidgetFont(font.getFamily(), font.getStyle(), fontSize);
        font_prop.setValue(new_font);
    }

    /**
     * @param textModel
     *            Model of BOY widget to be modified
     * @param adlTextWidget
     *            Model of ADL widget.  Sourve of the data
     *
     */
    public static void setAlignment(final Widget textModel, final ADLAbstractWidget adlTextWidget)
    {
        if (adlTextWidget.getName().equals("text")  ||
            adlTextWidget.getName().equals("text update"))
        {
            final WidgetProperty<HorizontalAlignment> prop_align = textModel.getProperty(CommonWidgetProperties.propHorizontalAlignment);

            String alignment = ((ITextWidget)adlTextWidget).getAlignment();
            // Just check for the essential part of "horiz. right"
            if (alignment.contains("center"))
                prop_align.setValue(HorizontalAlignment.CENTER);
            else if (alignment.contains("right"))
                prop_align.setValue(HorizontalAlignment.RIGHT);
            else // default to 'left'
                prop_align.setValue(HorizontalAlignment.LEFT);
        }
    }

    /**
     * @param textModel
     *            Model of BOY widget to be modified
     * @param adlTextWidget
     *            Model of ADL widget.  Sourve of the data
     *
     */
    public static void setFormat(final Widget textModel, final ADLAbstractWidget adlTextWidget)
    {
        if (adlTextWidget.getName().equals("text entry")  ||
            adlTextWidget.getName().equals("text update"))
        {
            final String format = ((ITextWidget)adlTextWidget).getFormat();
            final WidgetProperty<FormatOption> format_prop = textModel.getProperty(CommonWidgetProperties.propFormat);

            if (format.equals("")|| format.equals("decimal"))
                format_prop.setValue(FormatOption.DECIMAL);
            else if (format.equals("exponential"))
                format_prop.setValue(FormatOption.EXPONENTIAL);
            else if (format.equals("engr. notation"))
                format_prop.setValue(FormatOption.ENGINEERING);
            else if (format.equals("hexadecimal"))
                format_prop.setValue(FormatOption.HEX);
            else if (format.equals("string"))
                format_prop.setValue(FormatOption.STRING);
            else if (format.equals("octal"))
                logger.log(Level.WARNING, "Not handled: format - octal");
            else if (format.equals("compact"))
                format_prop.setValue(FormatOption.COMPACT);
            else if (format.equals("sexagesimal"))
                format_prop.setValue(FormatOption.SEXAGESIMAL);
            else if (format.equals("sexagesimal-hms"))
                format_prop.setValue(FormatOption.SEXAGESIMAL_HMS);
            else if (format.equals("sexagesimal-dms"))
                format_prop.setValue(FormatOption.SEXAGESIMAL_DMS);
        }
    }

    public static int convertTextHeightToFontSize(final int h)
    {
        if (h < 9)
            return 8;
        else if (h < 10)
            return 9;
        else if (h < 11)
            return 10;
        else if (h < 12)
            return 11;
        else if (h < 13)
            return 12;
        else if (h < 15)
            return 14;
        else if (h < 27)
            return 16;
        else if (h < 19)
            return 18;
        else if (h < 21)
            return 20;
        else if (h < 25)
            return 24;
        else if (h < 29)
            return 28;
        else
            return 30;
    }
}
