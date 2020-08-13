/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propSquare;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays an LED which reflects the enumerated state of a PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MultiStateLEDWidget extends BaseLEDWidget
{
    /** Matcher for detecting legacy property names */
    private static final Pattern LEGACY_STATE_PATTERN = Pattern.compile("state_([a-z_]+)_([0-9]+)");

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("multi_state_led", WidgetCategory.MONITOR,
            "LED (Multi State)",
            "/icons/led-multi.png",
            "LED that represents multiple states",
            Arrays.asList("org.csstudio.opibuilder.widgets.LED"))
    {
        @Override
        public Widget createWidget()
        {
            return new MultiStateLEDWidget();
        }
    };

    /** Legacy properties that have already triggered a warning */
    private final CopyOnWriteArraySet<String> warnings_once = new CopyOnWriteArraySet<>();

    /** Custom configurator to read legacy *.opi files */
    private static class LEDConfigurator extends WidgetConfigurator
    {
        public LEDConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            // Legacy XML with off_color, on_color identifies plain boolean LED
            if (XMLUtil.getChildElement(xml, "off_color") != null ||
                XMLUtil.getChildElement(xml, "on_color") != null)
                return false;

            super.configureFromXML(model_reader, widget, xml);

            // Handle legacy state_color_fallback
            final MultiStateLEDWidget model_widget = (MultiStateLEDWidget) widget;
            Element element = XMLUtil.getChildElement(xml, "state_color_fallback");
            if (element != null)
                model_widget.fallback_color.readFromXML(model_reader, element);

            element = XMLUtil.getChildElement(xml, "state_label_fallback");
            if (element != null)
                model_widget.fallback_label.readFromXML(model_reader, element);

            // Handle legacy state_value_0, state_color_0, ..1, ..2, ..
            final ArrayWidgetProperty<StateWidgetProperty> states = model_widget.states;
            int state = 0;
            while ((element = XMLUtil.getChildElement(xml, "state_color_" + state)) != null)
            {
                while (states.size() <= state)
                    states.addElement();
                states.getElement(state).color().readFromXML(model_reader, element);

                element = XMLUtil.getChildElement(xml, "state_value_" + state);
                if (element != null)
                    states.getElement(state).state().readFromXML(model_reader, element);

                element = XMLUtil.getChildElement(xml, "state_label_" + state);
                if (element != null)
                    states.getElement(state).label().readFromXML(model_reader, element);

                ++state;
            }
            // Widget starts with 2 states. If legacy replaced those and added more: OK.
            // If legacy contained only one state, we'll keep the second default state,
            // but then a 1-state LED is really illdefined

            handle_legacy_LED(model_reader, widget, xml_version, xml);

            // If legacy widgets was configured to not use labels, clear them
            XMLUtil.getChildBoolean(xml, "show_boolean_label").ifPresent(show ->
            {
                if (! show)
                    for (int i=0; i<states.size(); ++i)
                        states.getElement(i).label().setValue("");
            });

            return true;
        }
    }

    // Elements of the 'state' structure
    private static final WidgetPropertyDescriptor<Integer> propStateValue =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "value", "Value");

    private static final WidgetPropertyDescriptor<String> propStateLabel =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "label", "Label");

    private static final WidgetPropertyDescriptor<WidgetColor> propStateColor =
        CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "color", "Color");

    private static final WidgetPropertyDescriptor<String> propFallbackLabel =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "fallback_label", "Fallback Label");

    private static final WidgetPropertyDescriptor<WidgetColor> propFallbackColor =
            CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "fallback_color", "Fallback Color");

    // 'state' structure that describes one state
    private static final StructuredWidgetProperty.Descriptor behaviorState =
        new StructuredWidgetProperty.Descriptor(WidgetPropertyCategory.BEHAVIOR, "state", "State");

    /** Property that describes one state of the LED */
    public static class StateWidgetProperty extends StructuredWidgetProperty
    {
        public StateWidgetProperty(final Widget widget, final int state)
        {
            super(behaviorState, widget,
                  Arrays.asList(propStateValue.createProperty(widget, state),
                                propStateLabel.createProperty(widget, "State " + (state + 1)),
                                propStateColor.createProperty(widget, getDefaultColor(state))));
        }
        public WidgetProperty<Integer> state()      { return getElement(0); }
        public WidgetProperty<String> label()       { return getElement(1); }
        public WidgetProperty<WidgetColor> color()  { return getElement(2); }
    };

    // Helper for obtaining initial color for each state
    private static WidgetColor getDefaultColor(final int state)
    {
        if (state == 0)
            return WidgetColorService.resolve(new NamedWidgetColor("Off", 60, 100, 60));
        if (state == 1)
            return WidgetColorService.resolve(new NamedWidgetColor("On", 0, 255, 0));
        // Shade of blue for remaining states
        return WidgetColorService.resolve(new NamedWidgetColor("State " + state, 10, 0, Math.min(255, 40*state)));
    }

    // 'states' array
    private static final ArrayWidgetProperty.Descriptor<StateWidgetProperty> propStates =
            new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "states", "States",
                    (widget, state) -> new StateWidgetProperty(widget, state));

    private volatile ArrayWidgetProperty<StateWidgetProperty> states;
    private volatile WidgetProperty<WidgetColor> fallback_color;
    private volatile WidgetProperty<String> fallback_label;

    public MultiStateLEDWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(states = propStates.createProperty(this, Arrays.asList(new StateWidgetProperty(this, 0),
                                                                              new StateWidgetProperty(this, 1))));
        properties.add(fallback_label = propFallbackLabel.createProperty(this, "Err"));
        properties.add(fallback_color = propFallbackColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID)));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(50, 50, 50, 178)));
        properties.add(square = propSquare.createProperty(this, false));
    }

     @Override
    public WidgetProperty<?> getProperty(final String name)
    {
        // Translate legacy property names
        Matcher matcher = LEGACY_STATE_PATTERN.matcher(name);
        // state_value_0, state_color_0, state_label_0
        if (matcher.matches())
        {
            final int index = Integer.parseInt(matcher.group(2));
            final String states = "states[" + index + "].";
            final String new_name = states + matcher.group(1);
            if (warnings_once.add(name))
                logger.log(Level.WARNING, "Deprecated access to " + this + " property '" + name + "'. Use '" + new_name + "'");
            return getProperty(new_name);
        }

        return super.getProperty(name);
    }
    
    /** @return 'states' property */
    public ArrayWidgetProperty<StateWidgetProperty> propStates()
    {
        return states;
    }

    /** @return 'fallback_label' property */
    public WidgetProperty<String> propFallbackLabel()
    {
        return fallback_label;
    }

    /** @return 'fallback_color' property */
    public WidgetProperty<WidgetColor> propFallbackColor()
    {
        return fallback_color;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new LEDConfigurator(persisted_version);
    }
}
