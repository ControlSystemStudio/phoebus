/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.phoebus.ui.color.WidgetColor;

/** Widget that displays a thermometer
 *
 *  <p>Extends {@link ScaledPVWidget} so the thermometer offers the same
 *  range, scale and alarm limit properties as the {@link TankWidget}.
 *  The scale related properties only take effect with the RTTank based
 *  renderer, see {@link #SCALE_MODE_PROPS}.
 *
 *  <p>Existing {@code .bob} files load unchanged: {@code fill_color},
 *  {@code limits_from_pv}, {@code minimum} and {@code maximum} keep their
 *  XML names. Older Phoebus versions ignore the new properties.
 *
 * @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ThermometerWidget extends ScaledPVWidget
{
    /** Properties that only have an effect with the RTTank based renderer
     *  ({@code thermometer_scale_mode=true}).
     *  The property editor hides these while the stock renderer is in use. */
    public static final Set<String> SCALE_MODE_PROPS = Set.of(
        "format", "precision",
        "empty_color", "foreground_color", "font",
        "log_scale",
        "scale_visible", "show_minor_ticks", "show_scale_labels",
        "opposite_scale_visible", "perpendicular_tick_labels",
        "tank_border_width",
        "bulb_size",
        "alarm_limits_from_pv", "show_alarm_limits",
        "level_lolo", "level_low", "level_high", "level_hihi",
        "minor_alarm_color", "major_alarm_color");

    /** 'bulb_size': how much wider than the tube the bulb is, in pixels (0..50).
     *  The bulb is always drawn; 0 gives the narrowest bulb, not none.
     *  With the default of 20 and no scale, a thermometer of the default
     *  width has about the proportions of the stock thermometer. */
    public static final WidgetPropertyDescriptor<Integer> propBulbSize =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "bulb_size",
                                     Messages.WidgetProperties_BulbSize, 0, 50);

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor("thermometer",
            WidgetCategory.MONITOR,
            "Thermometer",
            "/icons/Thermo.png",
            "A thermometer",
            Arrays.asList("org.csstudio.opibuilder.widgets.thermometer"))
    {
        @Override
        public Widget createWidget()
        {
            return new ThermometerWidget();
        }
    };

    private volatile WidgetProperty<WidgetColor> emptyColor;
    private volatile WidgetProperty<Integer>     bulbSize;

    /** Constructor */
    public ThermometerWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 40, 160);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        defineScaleLookProperties(properties, false, true);
        properties.add(emptyColor = TankWidget.propEmptyColor.createProperty(this, new WidgetColor(250, 250, 250)));
        properties.add(bulbSize   = propBulbSize.createProperty(this, 20));
    }

    /** @return 'empty_color' property, the color of the empty part of the tube */
    public WidgetProperty<WidgetColor> propEmptyColor()               { return emptyColor; }

    /** @return 'bulb_size' property */
    public WidgetProperty<Integer> propBulbSize()                     { return bulbSize; }
}
