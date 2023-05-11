/*******************************************************************************
 * Copyright (c) 2019-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.BaseLEDWidget;
import org.csstudio.display.builder.model.widgets.LEDWidget;
import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget;
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmColor;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeRectangleClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeRectangleClass extends ConverterBase<Widget>
{
    public Convert_activeRectangleClass(final EdmConverter converter, final Widget parent, final Edm_activeRectangleClass r)
    {
        super(converter, parent, r);

        // '0' means smallest possible line == 1
        final int linewidth = Math.max(1,  r.getLineWidth());

        // EDM applies linewidth inside and outside of widget
        final RectangleWidget rect = (RectangleWidget) widget;
        rect.propLineWidth().setValue(linewidth);
        widget.propX().setValue(r.getX() - converter.getOffsetX() - linewidth/2);
        widget.propY().setValue(r.getY() - converter.getOffsetY() - linewidth/2);
        widget.propWidth().setValue(r.getW()+linewidth);
        widget.propHeight().setValue(r.getH()+linewidth);

        // No 'dash' support
        //if (r.getLineStyle().isExistInEDL()  &&
        //    r.getLineStyle().get() == EdmLineStyle.DASH)

        rect.propTransparent().setValue(! r.isFill());

        rect.propVisible().setValue(!r.isInvisible());

        if (r.isLineAlarm() && r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), rect.propLineColor());
        else
            convertColor(r.getLineColor(), r.getAlarmPv(), rect.propLineColor());
        if (r.isFillAlarm() && r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), rect.propBackgroundColor());
        else
            convertColor(r.getFillColor(), r.getAlarmPv(), rect.propBackgroundColor());

        // Convert to LED?
        widget = convertShapeToLED(widget, r, r.isFillAlarm(), r.getFillColor(), r.getAlarmPv());
    }

    /** Can original widget be replaced with LED because shape uses dynamic color? */
    protected static Widget convertShapeToLED(final Widget widget, final EdmWidget edm, final boolean fill_alarm, final EdmColor fillColor, final String pv_spec)
    {
        if (!(fill_alarm || fillColor.isDynamic())  ||   pv_spec == null   ||  pv_spec.isBlank())
            return widget;

        logger.log(Level.FINE, "Checking " + edm.getType() + " with dynamic color for LED conversion");

        String pv = convertPVName(pv_spec);

        // EDM dynamic colors are defined as ranges like this:
        // >=-0.5 && <0.5: "color0"
        // >=0.5  && <1.5: "color1"
        // >=1.5  && <2.5: "color2"
        // >=2.5  && <3.5: "color3"
        // >=3.5  && <4.5: "color4"
        // >=4.5  && <=5.5: "color5"
        //
        // In most cases, they are then used with enumerated or integer PVs
        // so values 0, 1, .., 5 map to color0..5.
        // We assume that case and try to create an LED or MultiStateLED
        //
        // If the EDM display was meant to be used with double values,
        // this will fail.
        // 1.9 that was resulting in color2 for EDM will now show color1...
        final List<WidgetColor> colors = new ArrayList<>();


        if (fill_alarm)
        {   // Use alarm severity of the PV and alarm colors
            pv = "=highestSeverity(`" + pv + "`)";
            colors.add(WidgetColorService.getColor(NamedWidgetColors.ALARM_OK));
            colors.add(WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR));
            colors.add(WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR));
            colors.add(WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID));
            colors.add(WidgetColorService.getColor(NamedWidgetColors.ALARM_DISCONNECTED));
        }
        else
        {
            int index = 0;
            for (Entry<String, String> entry : fillColor.getRuleMap().entrySet())
            {
                final double[] start_end = parseColorRange(entry.getKey());
                if (// start...end surrounds   index +- 1
                    ((index-1) <  start_end[0]  &&  start_end[0] <= index  &&
                     index     <= start_end[1]  &&  start_end[1] < (index+1))

                    // Or just start and open end
                    ||
                    ((index-1) <  start_end[0]  &&  start_end[0] <= index  &&
                     Double.isNaN(start_end[1]))
                   )
                {
                    final EdmColor edm_color = EdmModel.getColorsList().getColor(entry.getValue());
                    if (edm_color == null)
                    {
                        logger.log(Level.WARNING, "Dynamic color uses unknown color " + entry.getValue());
                        return widget;
                    }
                    final WidgetColor color = convertStaticColor(edm_color);
                    logger.log(Level.FINE, String.format("State %d (%6.3f to %6.3f) --> %s",
                                                         index,
                                                         start_end[0], start_end[1],
                                                         color.toString()));
                    colors.add(color);
                }
                else
                {
                    logger.log(Level.FINE, "Colors don't map to integer state ranges");
                    return widget;
                }

                ++index;
            }
        }

        BaseLEDWidget replacement = null;
        if (colors.size() == 2)
        {
            logger.log(Level.INFO, "Creating LED for " + pv);
            logger.log(Level.INFO, "Off color " + colors.get(0));
            logger.log(Level.INFO, "On  color " + colors.get(1));

            // Create LED in place of original shape
            final LEDWidget led = new LEDWidget();
            led.propName().setValue(widget.propName().getValue());
            led.propX().setValue(widget.propX().getValue());
            led.propY().setValue(widget.propY().getValue());
            led.propWidth().setValue(widget.propWidth().getValue());
            led.propHeight().setValue(widget.propHeight().getValue());
            led.propSquare().setValue(true);

            // Instead of script, use plain PV and LED behavior
            led.propPVName().setValue(pv);
            led.propOffColor().setValue(colors.get(0));
            led.propOnColor().setValue(colors.get(1));

            // LED should ideally be alarm sensitive, but the shape wasn't, so keep it similar
            led.propBorderAlarmSensitive().setValue(false);

            replacement = led;
        }
        else if (colors.size() > 2)
        {
            logger.log(Level.INFO, "Creating MultiStateLED for " + colors.size() + " states of " + pv);
            for (int i=0; i<colors.size(); ++i)
                logger.log(Level.INFO, "State " + i + " color " + colors.get(i));

            // Create LED in place of original shape
            final MultiStateLEDWidget led = new MultiStateLEDWidget();
            led.propName().setValue(widget.propName().getValue());
            led.propX().setValue(widget.propX().getValue());
            led.propY().setValue(widget.propY().getValue());
            led.propWidth().setValue(widget.propWidth().getValue());
            led.propHeight().setValue(widget.propHeight().getValue());
            led.propSquare().setValue(true);

            // Instead of script, use plain PV and LED behavior
            led.propPVName().setValue(pv);
            while (led.propStates().size() > colors.size())
                led.propStates().removeElement();
            while (led.propStates().size() < colors.size())
                led.propStates().addElement();
            for (int i=0; i<colors.size(); ++i)
            {
                led.propStates().getElement(i).color().setValue(colors.get(i));
                led.propStates().getElement(i).label().setValue("");
            }

            // LED should ideally be alarm sensitive, but the shape wasn't, so keep it similar
            led.propBorderAlarmSensitive().setValue(false);

            replacement = led;
        }

        // Replace original shape with LED?
        if (replacement == null)
            return widget;

        final ChildrenProperty parent_children = ChildrenProperty.getChildren(widget.getParent().get());
        parent_children.removeChild(widget);
        parent_children.addChild(replacement);
        return replacement;
    }


    private static Pattern start_end = Pattern.compile("\\s*>=\\s*([-+]?[0-9]*\\.?[0-9]*)\\s*&&\\s*<=?\\s*([-+]?[0-9]*\\.?[0-9]*)\\s*");
    private static Pattern start = Pattern.compile("\\s*>=?\\s*([-+]?[0-9]*\\.?[0-9]*)\\s*");

    /** Parse start and end from ">=-0.5 && <0.5" or ">=4.5  && <=5.5" or just start from ">2.5"
     *  @param range Range of EDM dynamic color
     *  @return [start, end], either start or end may be NaN
     */
    protected static double[] parseColorRange(final String range)
    {
        Matcher matcher = start_end.matcher(range);
        if (matcher.matches())
            return new double[]
            {
                Double.parseDouble(matcher.group(1)),
                Double.parseDouble(matcher.group(2))
            };

        matcher = start.matcher(range);
        if (matcher.matches())
            return new double[]
            {
                Double.parseDouble(matcher.group(1)),
                Double.NaN
            };

        return new double[] { Double.NaN, Double.NaN };
    }

    @Override
    protected Widget createWidget(final EdmWidget edm)
    {
        return new RectangleWidget();
    }
}
