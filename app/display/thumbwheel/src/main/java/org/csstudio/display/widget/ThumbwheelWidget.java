package org.csstudio.display.widget;

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
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.WritablePVWidget;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;

public class ThumbwheelWidget extends WritablePVWidget {

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("thumbwheel",
                    WidgetCategory.CONTROL,
                    "Thumbwheel",
                    "/icons/thumbwheel.gif",
                    "Display a thumbwheel",
                    Arrays.asList("org.csstudio.opibuilder.widgets.ThumbWheel"))
            {
                @Override
                public Widget createWidget()
                {
                    return new ThumbwheelWidget();
                }
            };

    public static final WidgetPropertyDescriptor<WidgetColor> propIncrementColor = newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "increment_color", "Increment Buttons Color");
    public static final WidgetPropertyDescriptor<WidgetColor> propDecrementColor = newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "decrement_color", "Decrement Buttons Color");
    public static final WidgetPropertyDescriptor<Boolean> propGraphicVisible = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "graphic_visible", "Graphic Visible");
    public static final WidgetPropertyDescriptor<Integer> propIntegerDigits = newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "integer_digits", "Integer Digits");
    public static final WidgetPropertyDescriptor<Integer> propDecimalDigits = newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "decimal_digits", "Decimal Digits");
    public static final WidgetPropertyDescriptor<Boolean> propNegativeNumbers = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "negative_numbers", "Enable Negative Numbers");
    public static final WidgetPropertyDescriptor<WidgetColor> propInvalidColor = newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "invalid_color", "Invalid Color");
    public static final WidgetPropertyDescriptor<Boolean> propSpinnerShaped = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "spinner_shaped", "Spinner Shaped");

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> increment_color;
    private volatile WidgetProperty<WidgetColor> decrement_color;
    private volatile WidgetProperty<Integer> integer_digits;
    private volatile WidgetProperty<Integer> decimal_digits;
    private volatile WidgetProperty<Boolean> negative_numbers;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Boolean> graphic_visible;
    private volatile WidgetProperty<WidgetColor> invalid_color;
    private volatile WidgetProperty<Boolean> spinner_shaped;

    public ThumbwheelWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 100);
    }

    @Override
    protected void defineProperties(List<WidgetProperty<?>> properties) {
        super.defineProperties(properties);
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.WRITE_BACKGROUND)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(increment_color = propIncrementColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(decrement_color = propDecrementColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(invalid_color = propInvalidColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID)));

        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(decimal_digits = propDecimalDigits.createProperty(this, 2));
        properties.add(integer_digits = propIntegerDigits.createProperty(this, 3));
        properties.add(negative_numbers = propNegativeNumbers.createProperty(this, true));

        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(graphic_visible = propGraphicVisible.createProperty(this, true));
        properties.add(spinner_shaped = propSpinnerShaped.createProperty(this, false));
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'increment_color' property */
    public WidgetProperty<WidgetColor> propIncrementColor()
    {
        return increment_color;
    }

    /** @return 'decrement_color' property */
    public WidgetProperty<WidgetColor> propDecrementColor()
    {
        return decrement_color;
    }

    /** @return 'integer_digits' property */
    public WidgetProperty<Integer> propIntegerDigits()
    {
        return integer_digits;
    }

    /** @return 'decimal_digits' property */
    public WidgetProperty<Integer> propDecimalDigits()
    {
        return decimal_digits;
    }

    /** @return 'negative_numbers' property */
    public WidgetProperty<Boolean> propNegativeNumbers() {
        return negative_numbers;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'graphic_visible' property */
    public WidgetProperty<Boolean> propGraphicVisible()
    {
        return graphic_visible;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'invalid_color' property */
    public WidgetProperty<WidgetColor> propInvalidColor()
    {
        return invalid_color;
    }

    /** @return 'spinner_shaped' property */
    public WidgetProperty<Boolean> propSpinnerShaped()
    {
        return spinner_shaped;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new LegacyWidgetConfigurator(persisted_version);
    }

    private static class LegacyWidgetConfigurator extends WidgetConfigurator
    {
        public LegacyWidgetConfigurator(Version xml_version) {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(ModelReader model_reader, Widget widget, Element widget_xml) throws Exception {
            if (!super.configureFromXML(model_reader, widget, widget_xml))
                return false;

            final Optional<String> integerDigits = XMLUtil.getChildString(widget_xml, "integerDigits");
            if(integerDigits.isPresent())
            {
                int integer_digits = Integer.parseInt(integerDigits.get());
                widget.getProperty(propIntegerDigits).setValue(integer_digits);
            }
            final Optional<String> decimalDigits = XMLUtil.getChildString(widget_xml, "decimalDigits");
            if(decimalDigits.isPresent())
            {
                int decimal_digits = Integer.parseInt(decimalDigits.get());
                widget.getProperty(propDecimalDigits).setValue(decimal_digits);
            }
            return true;
        }
    }
}
