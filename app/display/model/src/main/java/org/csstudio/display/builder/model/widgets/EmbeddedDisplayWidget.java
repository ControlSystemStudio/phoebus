/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Widget that shows another display inside itself
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EmbeddedDisplayWidget extends MacroWidget
{
    public static final int DEFAULT_WIDTH = 400,
                            DEFAULT_HEIGHT = 300;

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("embedded", WidgetCategory.STRUCTURE,
            "Embedded Display",
            "/icons/embedded.png",
            "Widget that embeds another display",
            Arrays.asList("org.csstudio.opibuilder.widgets.linkingContainer"))
    {
        @Override
        public Widget createWidget()
        {
            return new EmbeddedDisplayWidget();
        }
    };

    /** Resize behavior */
    public enum Resize
    {
        /** No resize, add scroll bars if content too large for container */
        None(Messages.Resize_None),

        /** Scale embedded *.opi or *.bob content to fit the container */
        ResizeContent(Messages.Resize_Content),

        /** Size the container to fit the embedded *.opi or *.bob content */
        SizeToContent(Messages.Resize_Container),

        /** Stretch the embedded content to fit the container,
         *  separately scaling the horizontal and vertical size
         */
        StretchContent(Messages.Resize_Stretch),

        /** No resize, but also no scroll bars. Oversized content is cropped */
        Crop(Messages.Resize_Crop);

        private final String label;

        private Resize(final String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static final WidgetPropertyDescriptor<Resize> propResize =
        new WidgetPropertyDescriptor<>(
            WidgetPropertyCategory.DISPLAY, "resize", Messages.WidgetProperties_ResizeBehavior)
    {
        @Override
        public EnumWidgetProperty<Resize> createProperty(final Widget widget,
                                                         final Resize default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    static final WidgetPropertyDescriptor<String> propGroupName =
        CommonWidgetProperties.newStringPropertyDescriptor(
            WidgetPropertyCategory.DISPLAY, "group_name", Messages.EmbeddedDisplayWidget_GroupName);

    public static final WidgetPropertyDescriptor<DisplayModel> runtimeModel =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, "embedded_model", "Embedded Model")
        {
            @Override
            public WidgetProperty<DisplayModel> createProperty(final Widget widget, DisplayModel default_value)
            {
                return new RuntimeWidgetProperty<>(runtimeModel, widget, default_value)
                {
                    @Override
                    public void setValueFromObject(final Object value)
                            throws Exception
                    {
                        if (! (value instanceof DisplayModel))
                            throw new IllegalArgumentException("Expected DisplayModel, got " + Objects.toString(value));
                        doSetValue((DisplayModel)value, true);
                    }
                };
            }
        };

    /** Custom configurator to read legacy *.opi files */
    private static class EmbeddedDisplayWidgetConfigurator extends WidgetConfigurator
    {
        public EmbeddedDisplayWidgetConfigurator(final Version xml_version)
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
                // Fall back to legacy "opi_file" for display file
                if (XMLUtil.getChildElement(xml, propFile.getName()) == null)
                {
                    final Optional<String> opi_file = XMLUtil.getChildString(xml, "opi_file");
                    if (opi_file.isPresent())
                        widget.setPropertyValue(propFile, opi_file.get());
                }

                // Transition legacy "resize_behaviour"
                Element element = XMLUtil.getChildElement(xml, "resize_behaviour");
                if (element != null)
                {
                    try
                    {   // 0=SIZE_OPI_TO_CONTAINER, 1=SIZE_CONTAINER_TO_OPI, 2=CROP_OPI, 3=SCROLL_OPI
                        // Problem with any resize is that we now use the content's connfigured width x height
                        // as size, while the legacy implementation self-determined the size and
                        // width x height were usually not configured.
                        // This likely results in unexpected resizing until the size of the legacy display file
                        // (which doesn't matter to the legacy tool) gets configured.
                        final int old_resize = Integer.parseInt(XMLUtil.getString(element));
                        if (old_resize == 0)
                            widget.setPropertyValue(propResize, Resize.ResizeContent);
                        else if (old_resize == 1)
                            widget.setPropertyValue(propResize, Resize.SizeToContent);
                        else // 'scroll' or 'crop' -> crop
                            widget.setPropertyValue(propResize, Resize.Crop);
                    }
                    catch (NumberFormatException ex)
                    {
                        clean_parse = false;
                        logger.log(Level.WARNING, "Cannot decode legacy resize_behavior");
                    }
                }

                // Transition legacy border that includes a label into group wrapper
                final int border_style = XMLUtil.getChildInteger(xml, "border_style").orElse(0);
                if (border_style == 12 || // TITLE_BAR
                    border_style == 13)   // GROUP_BOX
                {
                    final Style style = GroupWidget.convertLegacyStyle(border_style);
                    createGroupWrapper(widget, xml, style);
                    // Trigger re-parsing the XML from the parent down
                    throw new ParseAgainException("Wrap embedded display in group");
                }
                else
                    BorderSupport.handleLegacyBorder(widget, xml);
            }
            return true;
        }

        /** Create a GroupWidget for the border, with this EmbeddedDisplay inside the Group
         *  @param style
         */
        private void createGroupWrapper(final Widget widget, final Element embedded_xml, final Style style)
        {
            // Create a 'group' widget
            final Document doc = embedded_xml.getOwnerDocument();
            final Element group = doc.createElement(XMLTags.WIDGET);
            group.setAttribute(XMLTags.TYPE, GroupWidget.WIDGET_DESCRIPTOR.getType());

            // Set name, style, and copy location, .. from linking container
            XMLUtil.updateTag(group, XMLTags.NAME, widget.getName());
            XMLUtil.updateTag(group, GroupWidget.propStyle.getName(), Integer.toString(style.ordinal()));
            group.appendChild(doc.importNode(XMLUtil.getChildElement(embedded_xml, XMLTags.X), true));
            group.appendChild(doc.importNode(XMLUtil.getChildElement(embedded_xml, XMLTags.Y), true));
            group.appendChild(doc.importNode(XMLUtil.getChildElement(embedded_xml, XMLTags.WIDTH), true));
            group.appendChild(doc.importNode(XMLUtil.getChildElement(embedded_xml, XMLTags.HEIGHT), true));

            // IMPORTANT: Remove legacy border_style from this widget so when parsed again
            // there is no infinite loop creating more 'group' wrappers.
            Element el = XMLUtil.getChildElement(embedded_xml, "border_style");
            embedded_xml.removeChild(el);

            // Disable the 'custom' border, since the group wrapper now provides the border
            el = XMLUtil.getChildElement(embedded_xml, "border_width");
            if (el != null)
                embedded_xml.removeChild(el);

            // Update name
            XMLUtil.updateTag(embedded_xml, XMLTags.NAME, widget.getName() + "_Content");

            // Adjust X/Y to (0, 0)
            el = XMLUtil.getChildElement(embedded_xml, XMLTags.X);
            if (el != null)
                embedded_xml.removeChild(el);
            el = XMLUtil.getChildElement(embedded_xml, XMLTags.Y);
            if (el != null)
                embedded_xml.removeChild(el);

            // Adjust size to allow for the group's insets
            // .. which are not known until the group is represented.
            // Using a value that looked about right in tests.
            final int x_inset, y_inset;
            switch (style)
            {
            case NONE:   x_inset =  0;  y_inset =  0;  break;
            case LINE:   x_inset =  2;  y_inset =  2;  break;
            case TITLE:  x_inset =  2;  y_inset = 20;  break;
            case GROUP:
            default:     x_inset = 30;  y_inset = 30;  break;
            }
            XMLUtil.updateTag(embedded_xml, XMLTags.WIDTH, Integer.toString(widget.propWidth().getValue() - x_inset));
            XMLUtil.updateTag(embedded_xml, XMLTags.HEIGHT, Integer.toString(widget.propHeight().getValue() - y_inset));

            // Move this widget into the new group
            final Node parent = embedded_xml.getParentNode();
            parent.removeChild(embedded_xml);
            group.appendChild(embedded_xml);

            // .. and add that group to the parent
            parent.appendChild(group);

            // Debug the result
            // XMLUtil.dump(parent);
        }
    }

    private volatile WidgetProperty<String> file;
    private volatile WidgetProperty<Resize> resize;
    private volatile WidgetProperty<String> group_name;
    private volatile WidgetProperty<DisplayModel> embedded_model;
    private volatile WidgetProperty<Boolean> transparent;

    public EmbeddedDisplayWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(file = propFile.createProperty(this, ""));
        properties.add(resize = propResize.createProperty(this, Resize.None));
        properties.add(group_name = propGroupName.createProperty(this, ""));
        properties.add(embedded_model = runtimeModel.createProperty(this, null));
        properties.add(transparent = propTransparent.createProperty(this, false));
        BorderSupport.addBorderProperties(this, properties);
    }

    @Override
    public WidgetProperty<?> getProperty(String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        // Support legacy scripts/rules that access opi_file
        if (name.equals("opi_file"))
            return propFile();
        return super.getProperty(name);
    }

    @Override
    public Macros getEffectiveMacros()
    {
        final Macros macros = new Macros(super.getEffectiveMacros());

        // Legacy "Linking Container" defined a "Linking Container ID" macro.
        macros.add("LCID", getID());

        return macros;
    }

    /** @return 'file' property */
    public WidgetProperty<String> propFile()
    {
        return file;
    }

    /** @return 'resize' property */
    public WidgetProperty<Resize> propResize()
    {
        return resize;
    }

    /** @return 'group_name' property */
    public WidgetProperty<String> propGroupName()
    {
        return group_name;
    }

    /** @return Runtime 'model' property for the embedded display */
    public WidgetProperty<DisplayModel> runtimePropEmbeddedModel()
    {
        return embedded_model;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persisted_version)
            throws Exception
    {
        return new EmbeddedDisplayWidgetConfigurator(persisted_version);
    }
}
