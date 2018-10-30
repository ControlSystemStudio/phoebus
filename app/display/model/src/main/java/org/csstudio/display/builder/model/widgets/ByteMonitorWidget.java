/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static  org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOffColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOnColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRotationStep;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propSquare;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Widget that displays the bits in an Integer or Long Integer value as a set of LEDs
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ByteMonitorWidget extends PVWidget
{
    /** Widget descriptor */
	public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("byte_monitor", WidgetCategory.MONITOR,
            "Byte Monitor",
            "/icons/byte_monitor.png",
            "Displays the bits in an Integer or Long Integer value as a set of LEDs",
            Arrays.asList("org.csstudio.opibuilder.widgets.bytemonitor"))
    {
        @Override
        public Widget createWidget()
        {
            return new ByteMonitorWidget();
        }
    };

    /** 'start bit' property: Number of first (smallest) bit */
    public static final WidgetPropertyDescriptor<Integer> propStartBit =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "startBit", Messages.ByteMonitor_StartBit)
        {
            @Override
            public WidgetProperty<Integer> createProperty(final Widget widget, final Integer value)
            {
                return new IntegerWidgetProperty(this, widget, value, 0, 31);
            }
        };

    /** 'num. bits' property: Bit number in the integer to start displaying. */
    public static final WidgetPropertyDescriptor<Integer> propNumBits =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "numBits", Messages.ByteMonitor_NumBits)
        {
            @Override
            public WidgetProperty<Integer> createProperty(final Widget widget, final Integer value)
            {
                return new IntegerWidgetProperty(this, widget, value, 1, 32);
            }
        };

    /** 'bit reverse' property: Reverse the direction that bits are displayed; if no, the start bit (the smallest bit) is on right or bottom. */
    public static final WidgetPropertyDescriptor<Boolean> propBitReverse =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "bitReverse", Messages.ByteMonitor_BitReverse);

    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final Optional<Boolean> square = XMLUtil.getChildBoolean(xml, "square_led");
            if (square.isPresent())
                ((ByteMonitorWidget) widget).propSquare().setValue(square.get());

            // Legacy "labels"
            final Element el = XMLUtil.getChildElement(xml, "label");
            if (el != null)
            {
                final List<String> labels = new ArrayList<>();
                for (Element e : XMLUtil.getChildElements(el, "s"))
                    labels.add(XMLUtil.getString(e));
                if (labels.size() > 0)
                {
                    final boolean reverse = XMLUtil.getChildBoolean(xml, "bitReverse").orElse(false);
                    final WidgetProperty<WidgetColor> fg =
                            propForegroundColor.createProperty(widget,
                                                               WidgetColorService.getColor(NamedWidgetColors.TEXT));
                    fg.readFromXML(model_reader, xml);
                    addLabels((ByteMonitorWidget) widget, xml, labels, reverse, fg.getValue());
                }
            }

            return true;
        }

        // Add LabelWidget XML for legacy labels
        private void addLabels(final ByteMonitorWidget widget, final Element xml, final List<String> labels,
                               final boolean reverse, final WidgetColor foreground)
        {
            double x = widget.propX().getValue();
            double y = widget.propY().getValue();
            double w = widget.propWidth().getValue();
            double h = widget.propHeight().getValue();
            double n = widget.propNumBits().getValue();
            double dx, dy;
            final boolean horiz = widget.propHorizontal().getValue();
            if (horiz)
            {
                dy = 0;
                dx = w / n;
                x = reverse ? x : x + w - dx;
                w = dx;
            }
            else
            {
                dx = 0;
                dy = h / n;
                y = reverse ? y : y + h - dy;
                h = dy;
            }
            final Node parent = xml.getParentNode();
            final Node next = xml.getNextSibling();
            int i = 0;
            for (String text : labels)
            {
                final Element label = createLabelWidget(xml, widget.getName() + "#" + i, (int)x, (int)y, (int)w, (int)h, text, horiz, foreground);
                if (next != null)
                    parent.insertBefore(label, next);
                else
                    parent.appendChild(label);
                x += reverse ? dx : -dx;
                y += reverse ? dy : -dy;
                ++i;
            }
        }

        // Create LabelWidget XML
        private Element createLabelWidget(final Element xml, final String name, final int x, final int y, final int w, final int h,
                                          final String text, final boolean horiz, final WidgetColor foreground)
        {
            System.out.println(text);
            final Document doc = xml.getOwnerDocument();
            final Element label = doc.createElement(XMLTags.WIDGET);
            label.setAttribute(XMLTags.TYPE, LabelWidget.WIDGET_DESCRIPTOR.getType());

            Element el = doc.createElement(XMLTags.NAME);
            el.appendChild(doc.createTextNode(name));
            label.appendChild(el);

            el = doc.createElement(XMLTags.X);
            el.appendChild(doc.createTextNode(Integer.toString(x)));
            label.appendChild(el);

            el = doc.createElement(XMLTags.Y);
            el.appendChild(doc.createTextNode(Integer.toString(y)));
            label.appendChild(el);

            el = doc.createElement(XMLTags.WIDTH);
            el.appendChild(doc.createTextNode(Integer.toString(w)));
            label.appendChild(el);

            el = doc.createElement(XMLTags.HEIGHT);
            el.appendChild(doc.createTextNode(Integer.toString(h)));
            label.appendChild(el);

            el = doc.createElement(XMLTags.TEXT);
            el.appendChild(doc.createTextNode(text));
            label.appendChild(el);

            el = doc.createElement(propHorizontalAlignment.getName());
            el.appendChild(doc.createTextNode(Integer.toString(HorizontalAlignment.CENTER.ordinal())));
            label.appendChild(el);

            el = doc.createElement(propVerticalAlignment.getName());
            el.appendChild(doc.createTextNode(Integer.toString(VerticalAlignment.MIDDLE.ordinal())));
            label.appendChild(el);

            final Element fel = doc.createElement(XMLTags.COLOR);
            fel.setAttribute("red", String.valueOf(foreground.getRed()));
            fel.setAttribute("green", String.valueOf(foreground.getGreen()));
            fel.setAttribute("blue", String.valueOf(foreground.getBlue()));
            el = doc.createElement("foreground_color");
            el.appendChild(fel);
            label.appendChild(el);

            if (horiz)
            {
                el = doc.createElement(propRotationStep.getName());
                el.appendChild(doc.createTextNode(Integer.toString(RotationStep.NINETY.ordinal())));
                label.appendChild(el);
            }

            return label;
        }
    };


    private volatile WidgetProperty<WidgetColor> off_color;
    private volatile WidgetProperty<WidgetColor> on_color;
    private volatile WidgetProperty<Integer> startBit;
    private volatile WidgetProperty<Integer> numBits;
    private volatile WidgetProperty<Boolean> bitReverse;
    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Boolean> square;

    public ByteMonitorWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 160, 20);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(startBit = propStartBit.createProperty(this,0));
        properties.add(numBits = propNumBits.createProperty(this,8));
        properties.add(bitReverse = propBitReverse.createProperty(this,false));
        properties.add(horizontal = propHorizontal.createProperty(this,true));
        properties.add(square = propSquare.createProperty(this,false));
        properties.add(off_color = propOffColor.createProperty(this, new WidgetColor(60, 100, 60)));
        properties.add(on_color = propOnColor.createProperty(this, new WidgetColor(60, 255, 60)));
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /** @return 'off_color' property */
    public WidgetProperty<WidgetColor> propOffColor()
    {
        return off_color;
    }

    /** @return 'on_color' property */
    public WidgetProperty<WidgetColor> propOnColor()
    {
        return on_color;
    }

    /** @return 'startBit' property */
    public WidgetProperty<Integer> propStartBit()
    {
        return startBit;
    }

    /** @return 'numBits' property */
    public WidgetProperty<Integer> propNumBits()
    {
        return numBits;
    }

    /** @return 'bitReverse' property */
    public WidgetProperty<Boolean> propBitReverse()
    {
        return bitReverse;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'square' property */
    public WidgetProperty<Boolean> propSquare()
    {
        return square;
    }
}
