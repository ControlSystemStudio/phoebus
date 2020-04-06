/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFormat;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propShowUnits;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWrapWords;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
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
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.vtype.FormatOption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a changing text
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextEntryWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("textentry", WidgetCategory.CONTROL,
            "Text Entry",
            "/icons/textentry.png",
            "Text field that writes entered values to PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.TextInput",
                          "org.csstudio.opibuilder.widgets.NativeText"))
    {
        @Override
        public Widget createWidget()
        {
            return new TextEntryWidget();
        }
    };

    public static final WidgetPropertyDescriptor<Boolean> propMultiLine =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "multi_line", Messages.WidgetProperties_MultiLine);

    private static class CustomWidgetConfigurator extends WidgetConfigurator
    {
        public CustomWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;
            if (xml_version.getMajor() < 3)
            {
                final TextEntryWidget text_widget = (TextEntryWidget)widget;
                TextUpdateWidget.readLegacyFormat(xml, text_widget.format, text_widget.precision, text_widget.propPVName());

                Optional<String> text = XMLUtil.getChildString(xml, "multiline_input");
                if (text.isPresent()  &&  Boolean.parseBoolean(text.get()))
                    text_widget.propMultiLine().setValue(true);

                // Legacy 'selector'
                final int selector = XMLUtil.getChildInteger(xml, "selector_type").orElse(0);
                if (selector == 1)
                    addFileSelector(text_widget, xml);
                else if (selector == 2)
                    addDateTimeSelector(text_widget, xml);

                // There's no transparent option for the text entry.
                // Simulate by using transparent background color.
                XMLUtil.getChildBoolean(xml, "transparent").ifPresent(transparent ->
                {
                    if (transparent)
                        text_widget.propBackgroundColor().setValue(new WidgetColor(0, 0, 0, 0));
                });

                // Legacy text entry sometimes would with "text" property and no pv_name,
                // used as a Label
                text = XMLUtil.getChildString(xml, "text");
                if (text.isPresent()  &&  text.get().length() > 0  &&
                    ((MacroizedWidgetProperty<String>) text_widget.propPVName()).getSpecification().isEmpty())
                {
                    logger.log(Level.WARNING, "Replacing TextEntry " + text_widget + " with 'text' but no 'pv_name' with a Label");

                    // Replace the widget type with "label"
                    final String type = xml.getAttribute("typeId");
                    // Might be NativeText or TextInput
                    if (type != null  &&  type.contains("Text"))
                    {
                        // BOY 'TextInput' was at 2.0.0 or higher.
                        // Down-grade to label 1.0.0 to handle legacy border etc.
                        // for that version, not mistaking it for a Label version >= 2.0.0
                        xml.setAttribute("typeId", "org.csstudio.opibuilder.widgets.Label");
                        xml.setAttribute("version", "1.0.0");
                        // XMLUtil.dump(xml);
                        throw new ParseAgainException("Replace text entry with label");
                    }
                }

                BorderSupport.handleLegacyBorder(widget, xml);
            }
            return true;
        }

        private void addFileSelector(final TextEntryWidget text_widget, final Element xml) throws Exception
        {   // Create FileSelectorWidget (RCP only, so cannot access its source code here)
            final Document doc = xml.getOwnerDocument();

            final Element file_selector = doc.createElement(XMLTags.WIDGET);
            file_selector.setAttribute(XMLTags.TYPE, "fileselector");

            // Enforce String format
            text_widget.propFormat().setValue(FormatOption.STRING);

            // FileSelectorWidget happens to be about 40 pixels wide,
            // shrink text entry by that amount
            text_widget.propWidth().setValue(text_widget.propWidth().getValue() - 40);

            // Position at right end of TextEntry
            // Requires numbers, not macros in X and WIDTH (where BOY didn't support macros anyway)
            Element prop = doc.createElement(XMLTags.X);
            prop.appendChild(doc.createTextNode(Integer.toString(text_widget.propX().getValue() + text_widget.propWidth().getValue())));
            file_selector.appendChild(prop);

            prop = doc.createElement(XMLTags.Y);
            prop.appendChild(doc.createTextNode(((MacroizedWidgetProperty<?>)text_widget.propY()).getSpecification()));
            file_selector.appendChild(prop);

            prop = doc.createElement(XMLTags.HEIGHT);
            prop.appendChild(doc.createTextNode(((MacroizedWidgetProperty<?>)text_widget.propHeight()).getSpecification()));
            file_selector.appendChild(prop);

            prop = doc.createElement(XMLTags.PV_NAME);
            prop.appendChild(doc.createTextNode(((MacroizedWidgetProperty<?>)text_widget.propPVName()).getSpecification()));
            file_selector.appendChild(prop);

            // Filespace: Workspace, file system (same ordinals as BOY)
            final int file_source = XMLUtil.getChildInteger(xml, "file_source").orElse(0);
            prop = doc.createElement("filespace");
            prop.appendChild(doc.createTextNode(Integer.toString(file_source)));
            file_selector.appendChild(prop);

            // BOY ordinals: Full path, Name&ext, Name, Directory
            // Component: Full path, Directory, Name&ext, Base Name
            int part = XMLUtil.getChildInteger(xml, "file_return_part").orElse(0);
            final String[] legacy_file_part_2_component = new String[] { "0", "2", "3", "1" };
            if (part >= legacy_file_part_2_component.length)
                part = 0;

            prop = doc.createElement("component");
            prop.appendChild(doc.createTextNode(legacy_file_part_2_component[part]));
            file_selector.appendChild(prop);

            xml.getParentNode().appendChild(file_selector);
        }

        private void addDateTimeSelector(final TextEntryWidget text_widget, final Element xml)
        {
            // XXX Implement a Date/TimeWidget
            logger.log(Level.WARNING, text_widget + ": Support for Date/Time selector not implemented");
            // Enforce String format
            text_widget.propFormat().setValue(FormatOption.STRING);
        }
    }

    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<FormatOption> format;
    private volatile WidgetProperty<Integer> precision;
    private volatile WidgetProperty<Boolean> show_units;
    private volatile WidgetProperty<Boolean> wrap_words;
    private volatile WidgetProperty<Boolean> multi_line;

    public TextEntryWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    public Version getVersion()
    {   // Legacy used 2.0.0 for text input
        return new Version(3, 0, 0);
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persisted_version) throws Exception
    {
        return new CustomWidgetConfigurator(persisted_version);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.WRITE_BACKGROUND)));
        properties.add(format = propFormat.createProperty(this, FormatOption.DEFAULT));
        properties.add(precision = propPrecision.createProperty(this, -1));
        properties.add(show_units = propShowUnits.createProperty(this, true));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(wrap_words = propWrapWords.createProperty(this, false));
        properties.add(multi_line = propMultiLine.createProperty(this, false));
        BorderSupport.addBorderProperties(this, properties);
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background_color' property*/
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'format' property */
    public WidgetProperty<FormatOption> propFormat()
    {
        return format;
    }

    /** @return 'precision' property */
    public WidgetProperty<Integer> propPrecision()
    {
        return precision;
    }

    /** @return 'show_units' property */
    public WidgetProperty<Boolean> propShowUnits()
    {
        return show_units;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'wrap_words' property */
    public WidgetProperty<Boolean> propWrapWords()
    {
        return wrap_words;
    }

    /** @return 'multi_line' property */
    public WidgetProperty<Boolean> propMultiLine()
    {
        return multi_line;
    }
}
