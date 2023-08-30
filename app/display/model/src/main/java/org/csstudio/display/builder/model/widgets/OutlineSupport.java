
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineStyle;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;

import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/**
 * A helper class for handling the outline "line" property or widgets
 */
public class OutlineSupport {

    /**
     * Helper for custom {@link WidgetConfigurator} to map legacy border to an outline.
     * This is primarily for use by passive shape widgets
     *
     * @param widget Widget
     * @param xml XML source
     * @throws Exception on error
     */
    public static void handleLegacyBorder(final Widget widget, final Element xml) throws Exception
    {
        // Style tends to be a number, but could also be "None".
        final Optional<String> style_text = XMLUtil.getChildString(xml, "border_style");
        if (!style_text.isPresent())
        {
            return;
        }

        if ("none".equalsIgnoreCase(style_text.get()))
        {
            return;
        }

        // There is a "border" configuration.
        // Is there also a "line" configuration?
        if (XMLUtil.getChildDouble(xml, "line_width").orElse(-1.0) >
            XMLUtil.getChildDouble(xml, "border_width").orElse(-1.0))
        {
            // Ignore the smaller "border", use "line"
            return;
        }

        final int style;
        try
        {
            style = Integer.parseInt(style_text.get());
        } catch (NumberFormatException ex)
        {
            throw new Exception("Invalid border_style '" + style_text.get() + "'");
        }

        final Optional<Integer> xml_width = XMLUtil.getChildInteger(xml, "border_width");

        // If there's a border_color, use that for the line_color
        Element el = XMLUtil.getChildElement(xml, "border_color");
        if (el != null)
            widget.getProperty(propLineColor).readFromXML(null, el);

        switch (style)
        {
            case 0: // NONE
            case 15: // EMPTY
            case 3: // LOWERED
                widget.getProperty(propLineWidth).setValue(0);
                return;
            case 1: // LINE
            case 2: // RAISED
            case 4: // ETCHED
            case 5: // RIDGED
            case 6: // BUTTON_RAISED
            case 7: // BUTTON_PRESSED
                xml_width.ifPresent(w -> {
                    widget.getProperty(propLineWidth).setValue(w);
                });
                break;
            case 8: // DOTTED
                widget.getProperty(propLineStyle).setValue(LineStyle.DOT);
                xml_width.ifPresent(w -> {
                    widget.getProperty(propLineWidth).setValue(w);
                });
                break;
            case 9: // DASHED
                widget.getProperty(propLineStyle).setValue(LineStyle.DASH);
                xml_width.ifPresent(w -> {
                    widget.getProperty(propLineWidth).setValue(w);
                });
                break;
            case 10: // DASH_DOT
                widget.getProperty(propLineStyle).setValue(LineStyle.DASHDOT);
                xml_width.ifPresent(w -> {
                    widget.getProperty(propLineWidth).setValue(w);
                });
                break;
            case 11: // DASH_DOT_DOT
                widget.getProperty(propLineStyle).setValue(LineStyle.DASHDOTDOT);
                xml_width.ifPresent(w -> {
                    widget.getProperty(propLineWidth).setValue(w);
                });
                break;
        }
    }
}
