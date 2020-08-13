/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFormat;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propInteractive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRotationStep;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propShowUnits;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWrapWords;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.vtype.FormatOption;
import org.w3c.dom.Element;

/** Widget that displays a changing text
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextUpdateWidget extends PVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("textupdate", WidgetCategory.MONITOR,
            "Text Update",
            "/icons/textupdate.png",
            "Displays current value of PV as text",
            Arrays.asList("org.csstudio.opibuilder.widgets.TextUpdate"))
    {
        @Override
        public Widget createWidget()
        {
            return new TextUpdateWidget();
        }
    };

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
            if (xml_version.getMajor() < 2)
            {
                final TextUpdateWidget text_widget = (TextUpdateWidget)widget;
                TextUpdateWidget.readLegacyFormat(xml, text_widget.format, text_widget.precision, text_widget.propPVName());

                // Legacy rotation_angle -> rotation_step
                // BOY counted angle clockwise, we now use mathematical sense of rotation
                XMLUtil.getChildDouble(xml, "rotation_angle")
                       .ifPresent(angle -> text_widget.rotation_step.setValue(RotationStep.forAngle(- angle)));

                // Legacy text update had a "text" property that allowed using
                // it just like a label - no pv_name.
                // Some scripts would even update the 'text' concurrent with a pv_name...
                final Optional<String> text = XMLUtil.getChildString(xml, "text");
                if (text.isPresent()  &&  text.get().length() > 0  &&
                    ((MacroizedWidgetProperty<String>) text_widget.propPVName()).getSpecification().isEmpty())
                {
		    // Skip replacing legacy textupdate to label where pv_name is set as a rule
		    boolean pv_rule = false;
		    for (final Element xmlrl : XMLUtil.getChildElements(xml, "rules"))
		    {
			 for (final Element xmlr : XMLUtil.getChildElements(xmlrl))
			 {
		    	     String prop_id = xmlr.getAttribute("prop_id");
		    	     if (prop_id.contains("pv_name"))
		    	     {
		    		 pv_rule = true;
		    		 break;
		    	     }
			 }
		    }

                    // Replace the widget type with "label"
                    final String type = xml.getAttribute("typeId");
                    if (type != null  &&  type.endsWith("TextUpdate") && pv_rule == false)
                    {
			logger.log(Level.WARNING, "Replacing TextUpdate " + text_widget + " with 'text' but no 'pv_name' with a Label");
                        xml.setAttribute("typeId", "org.csstudio.opibuilder.widgets.Label");
                        // XMLUtil.dump(xml);
                        throw new ParseAgainException("Replace text update with label");
                    }
                }

                BorderSupport.handleLegacyBorder(widget, xml);
            }
            return true;
        }
    }

    /** Read legacy widget's format
     *  @param xml Widget XML
     *  @param format Format property to update
     *  @param precision Precision property to update
     *  @param pv_name PV name property to update
     */
    // package-level access for TextEntryWidget
    static void readLegacyFormat(final Element xml, final WidgetProperty<FormatOption> format,
                                 final WidgetProperty<Integer> precision,
                                 final WidgetProperty<String> pv_name) throws Exception
    {
        XMLUtil.getChildInteger(xml, "format_type").ifPresent(legacy_format ->
        {
            switch (legacy_format)
            {
            case 1: // DECIMAL
                format.setValue(FormatOption.DECIMAL);
                break;
            case 2: // EXP
                format.setValue(FormatOption.EXPONENTIAL);
                break;
            case 3: // HEX (32)
                format.setValue(FormatOption.HEX);
                precision.setValue(8);
                break;
            case 4: // STRING
                format.setValue(FormatOption.STRING);
                break;
            case 5: // HEX64
                format.setValue(FormatOption.HEX);
                precision.setValue(16);
                break;
            case 6: // COMPACT
                format.setValue(FormatOption.COMPACT);
                break;
            case 7: // ENG (since Aug. 2016)
                format.setValue(FormatOption.ENGINEERING);
                break;
            case 8: // SEXA (since Dec. 2016)
                format.setValue(FormatOption.SEXAGESIMAL);
                break;
            case 9: // SEXA_HMS (since Dec. 2016)
                format.setValue(FormatOption.SEXAGESIMAL_HMS);
                break;
            case 10: // SEXA_DMS (since Dec. 2016)
                format.setValue(FormatOption.SEXAGESIMAL_DMS);
                break;
            default:
                format.setValue(FormatOption.DEFAULT);
            }
        });

        // If legacy requested precision-from-PV, mark that in precision
        final Element element = XMLUtil.getChildElement(xml, "precision_from_pv");
        if (element != null  &&  Boolean.parseBoolean(XMLUtil.getString(element)))
            precision.setValue(-1);

        // Remove legacy longString attribute from PV,
        // instead use STRING formatting
        String pv = ((StringWidgetProperty)pv_name).getSpecification();
        if (pv.endsWith(" {\"longString\":true}"))
        {
            pv = pv.substring(0, pv.length() - 20);
            ((StringWidgetProperty)pv_name).setSpecification(pv);
            format.setValue(FormatOption.STRING);
        }
    }

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<FormatOption> format;
    private volatile WidgetProperty<Integer> precision;
    private volatile WidgetProperty<Boolean> show_units;
    private volatile WidgetProperty<HorizontalAlignment> horizontal_alignment;
    private volatile WidgetProperty<VerticalAlignment> vertical_alignment;
    private volatile WidgetProperty<Boolean> wrap_words;
    private volatile WidgetProperty<RotationStep> rotation_step;
    private volatile WidgetProperty<Boolean> interactive;

    public TextUpdateWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
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
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.READ_BACKGROUND)));
        properties.add(transparent = propTransparent.createProperty(this, false));
        properties.add(format = propFormat.createProperty(this, FormatOption.DEFAULT));
        properties.add(precision = propPrecision.createProperty(this, -1));
        properties.add(show_units = propShowUnits.createProperty(this, true));
        properties.add(horizontal_alignment = propHorizontalAlignment.createProperty(this, HorizontalAlignment.LEFT));
        properties.add(vertical_alignment = propVerticalAlignment.createProperty(this, VerticalAlignment.TOP));
        properties.add(wrap_words = propWrapWords.createProperty(this, true));
        properties.add(rotation_step = propRotationStep.createProperty(this, RotationStep.NONE));
        properties.add(interactive = propInteractive.createProperty(this, false));
        BorderSupport.addBorderProperties(this, properties);
    }

    @Override
    public WidgetProperty<?> getProperty(String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        // Support legacy scripts that access enabled
        if (name.equals("enabled"))
            return propVisible();
        return super.getProperty(name);
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
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

    /** @return 'horizontal_alignment' property */
    public WidgetProperty<HorizontalAlignment> propHorizontalAlignment()
    {
        return horizontal_alignment;
    }

    /** @return 'vertical_alignment' property */
    public WidgetProperty<VerticalAlignment> propVerticalAlignment()
    {
        return vertical_alignment;
    }

    /** @return 'wrap_words' property */
    public WidgetProperty<Boolean> propWrapWords()
    {
        return wrap_words;
    }

    /** @return 'rotation_step' property */
    public WidgetProperty<RotationStep> propRotationStep()
    {
        return rotation_step;
    }

    /** @return 'interactive' property */
    public WidgetProperty<Boolean> propInteractive()
    {
        return interactive;
    }
}
