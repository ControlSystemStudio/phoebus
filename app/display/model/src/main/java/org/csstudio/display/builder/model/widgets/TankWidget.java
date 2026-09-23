/*******************************************************************************
 * Copyright (c) 2015-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propLogscale;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.phoebus.ui.color.NamedWidgetColors;
import org.phoebus.ui.color.WidgetColor;
import org.phoebus.ui.color.WidgetColorService;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays a tank with variable fill level
 *
 *  <p>Extends {@link ScaledPVWidget} to inherit common scale/limit
 *  properties (min, max, alarm thresholds, limit colours).  This avoids
 *  the property duplication that existed across Tank, ProgressBar and
 *  Thermometer.
 *
 *  <p>Additional display properties include a configurable scale format,
 *  dual-scale support (left + right / top + bottom when horizontal),
 *  minor ticks, perpendicular label orientation, and log scaling.
 *  The dual-scale feature is modelled after CS-Studio BOY's tank which
 *  supported markers on both sides of the tank body.
 *
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, ScaledPVWidget refactoring,
 *          dual scale, alarm limits, format/precision controls
 */
@SuppressWarnings("nls")
public class TankWidget extends ScaledPVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("tank", WidgetCategory.MONITOR,
            "Tank",
            "/icons/tank.png",
            "Tank that 'fills' relative to numeric value of a PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.tank"))
    {
        @Override
        public Widget createWidget()
        {
            return new TankWidget();
        }
    };

    /** 'empty_color' */
    public static final WidgetPropertyDescriptor<WidgetColor> propEmptyColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "empty_color", Messages.WidgetProperties_EmptyColor);

    /** Widget configurator to read legacy *.opi files*/
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
            {
                final TankWidget tank = (TankWidget) widget;

                Element element = XMLUtil.getChildElement(xml, "color_fillbackground");
                if (element != null)
                    tank.empty_color.readFromXML(model_reader, element);

                element = XMLUtil.getChildElement(xml, "scale_font");
                if (element != null)
                    tank.font.readFromXML(model_reader, element);

                element = XMLUtil.getChildElement(xml, "show_scale");
                if (element != null)
                    tank.propScaleVisible().readFromXML(model_reader, element);

                if (XMLUtil.getChildBoolean(xml, "show_markers").orElse(true)  &&
                    (XMLUtil.getChildBoolean(xml, "show_hi").orElse(true)   ||
                     XMLUtil.getChildBoolean(xml, "show_hihi").orElse(true) ||
                     XMLUtil.getChildBoolean(xml, "show_lo").orElse(true)   ||
                     XMLUtil.getChildBoolean(xml, "show_lolo").orElse(true)))
                {   // There was at least one marker,
                    // but this widget is not supporting the legacy markers.
                    // -> Adjust width so that tank uses roughly the same space,
                    //    _not_ extending into the region that used to be occupied
                    //    by the markers.
                    tank.propWidth().setValue(Math.max(tank.propWidth().getValue() - 50, 50));
                }
            }

            return true;
        }
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> empty_color;
    private volatile WidgetProperty<Boolean> horizontal;


    /** Constructor */
    public TankWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 150, 200);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.READ_BACKGROUND)));
        properties.add(fillColor = propFillColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(empty_color = propEmptyColor.createProperty(this, new WidgetColor(192, 192, 192)));
        properties.add(scaleVisible = propScaleVisible.createProperty(this, true));
        properties.add(oppositeScaleVisible = propOppositeScaleVisible.createProperty(this, false));
        properties.add(showMinorTicks = propShowMinorTicks.createProperty(this, true));
        properties.add(showScaleLabels = propShowScaleLabels.createProperty(this, true));
        properties.add(perpendicularTickLabels = propPerpendicularTickLabels.createProperty(this, false));
        properties.add(logScale = propLogscale.createProperty(this, false));
        properties.add(horizontal = propHorizontal.createProperty(this, false));
        properties.add(borderWidth = propTankBorderWidth.createProperty(this, 0));
    }

    @Override
    public WidgetProperty<?> getProperty(String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        // Support legacy scripts/rules that access color_fillbackground
        if (name.equals("color_fillbackground"))
            return propEmptyColor();
        return super.getProperty(name);
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackground()
    {
        return background;
    }

    /** @return 'empty_color' property */
    public WidgetProperty<WidgetColor> propEmptyColor()
    {
        return empty_color;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

}
