/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * Copyright (c) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newPVNamePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmDialog;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmMessage;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFormat;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propShowUnits;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWrapWords;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import java.util.stream.Collectors;
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
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.vtype.FormatOption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a changing text
 *  @author Kay Kasemir
 *  @author Thales
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

    /** 'multi_line' */
    public static final WidgetPropertyDescriptor<Boolean> propMultiLine =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "multi_line", Messages.WidgetProperties_MultiLine);



    /**
     * 'items' property: list of items (string properties) for auto-completion
     */
    public static final WidgetPropertyDescriptor<String> propItems =
        newPVNamePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "autocompleteitems", "Suggestions");

    /**
     * 'max_suggestions' property: maximum number of suggestions to display
     */
    private static final WidgetPropertyDescriptor<Integer> propMaxSuggestions =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "max_suggestions", "Max Suggestions");

    /**
     * 'min_chars' property: minimum characters to trigger autocomplete
     */
    private static final WidgetPropertyDescriptor<Integer> propMinCharacters =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "min_characters", "Min Characters");

    /**
     * 'case_sensitive' property: whether matching is case sensitive
     */
    private static final WidgetPropertyDescriptor<Boolean> propCaseSensitive =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "case_sensitive", "Case Sensitive");

    /**
     * 'placeholder' property: placeholder text when empty
     */
    private static final WidgetPropertyDescriptor<String> propPlaceholder =
        newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "placeholder", "Placeholder Text");

    /**
     * 'allowcustom' property: allow custom values
     */
    private static final WidgetPropertyDescriptor<Boolean> propCustom =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "allow_custom",
            "Allow custom values");

    /**
     * 'filter_mode' property: how to filter suggestions (starts_with, contains, fuzzy)
     */
    private static final WidgetPropertyDescriptor<String> propFilterMode =
        newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "filter_mode", "Filter Mode");


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
                TextEntryWidget.readLegacyFormat(xml, text_widget.format, text_widget.precision, text_widget.propPVName());

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

    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<FormatOption> format;
    private volatile WidgetProperty<Integer> precision;
    private volatile WidgetProperty<Boolean> show_units;
    private volatile WidgetProperty<Boolean> wrap_words;
    private volatile WidgetProperty<Boolean> multi_line;
    private volatile WidgetProperty<HorizontalAlignment> horizontal_alignment;
    private volatile WidgetProperty<VerticalAlignment> vertical_alignment;
    private volatile WidgetProperty<String> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<Boolean> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;
    private volatile WidgetProperty<Integer> min_characters;
    private volatile WidgetProperty<Boolean> case_sensitive;
    private volatile WidgetProperty<String> placeholder;
    private volatile WidgetProperty<String> filter_mode;
    private volatile WidgetProperty<Boolean> allow_custom;
    private volatile List<String> itemsList = List.of();

    /** Constructor */
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
        properties.add(horizontal_alignment = propHorizontalAlignment.createProperty(this, HorizontalAlignment.LEFT));
        properties.add(vertical_alignment = propVerticalAlignment.createProperty(this, VerticalAlignment.MIDDLE));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(wrap_words = propWrapWords.createProperty(this, false));
        properties.add(multi_line = propMultiLine.createProperty(this, false));
        BorderSupport.addBorderProperties(this, properties);

        properties.add(items = propItems.createProperty(this, ""));
        properties.add(items_from_pv = propItemsFromPV.createProperty(this, false));
        properties.add(min_characters = propMinCharacters.createProperty(this, 1));
        properties.add(case_sensitive = propCaseSensitive.createProperty(this, false));
        properties.add(placeholder = propPlaceholder.createProperty(this, "Type to search..."));
        properties.add(filter_mode = propFilterMode.createProperty(this, "fuzzy"));
        properties.add(allow_custom = propCustom.createProperty(this, false));

        properties.add(confirm_dialog = propConfirmDialog.createProperty(this, false));
        properties.add(confirm_message = propConfirmMessage.createProperty(this, "Are you sure you want to do this?"));
        properties.add(password = propPassword.createProperty(this, ""));
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

    /**
     * @return 'items' property
     */
    public WidgetProperty<String> propItems() {
        return items;
    }

    /**
     * Convenience routine for script to fetch items
     *
     * @return Items currently available for auto-completion
     */
    public Collection<String> getItems() {
        return itemsList;
    }

    /**
     * Set the items list for auto-completion
     *
     * @param items List of items to set
     */
    public void setItems(List<String> items) {
        this.itemsList = Objects.requireNonNullElseGet(items, List::of);
    }

    /**
     * @return 'items_from_PV' property
     */
    public WidgetProperty<Boolean> propItemsFromPV() {
        return items_from_pv;
    }

    /**
     * @return 'confirm_dialog' property
     */
    public WidgetProperty<Boolean> propConfirmDialog() {
        return confirm_dialog;
    }

    /**
     * @return 'confirm_message' property
     */
    public WidgetProperty<String> propConfirmMessage() {
        return confirm_message;
    }

    /**
     * @return 'password' property
     */
    public WidgetProperty<String> propPassword() {
        return password;
    }

    /**
     * @return 'min_characters' property
     */
    public WidgetProperty<Integer> propMinCharacters() {
        return min_characters;
    }

    /**
     * @return 'case_sensitive' property
     */
    public WidgetProperty<Boolean> propCaseSensitive() {
        return case_sensitive;
    }

    /**
     * @return 'placeholder' property
     */
    public WidgetProperty<String> propPlaceholder() {
        return placeholder;
    }

    /**
     * @return 'filter_mode' property
     */
    public WidgetProperty<String> propFilterMode() {
        return filter_mode;
    }

    /**
     * @return 'allow_custom' property
     */
    public WidgetProperty<Boolean> propCustom() {
        return allow_custom;
    }

    /**
     * Filter items based on input text and return top N matches
     *
     * @param inputText Text to filter by
     * @return List of filtered suggestions (max N items)
     */
    public List<String> getFilteredSuggestions(final String inputText) {
        if (inputText == null || inputText.length() < min_characters.getValue()) {
            return List.of();
        }

        final String searchText = case_sensitive.getValue() ? inputText : inputText.toLowerCase();
        final String mode = filter_mode.getValue();

        return getItems().stream()
            .filter(item -> {
                final String itemText = case_sensitive.getValue() ? item : item.toLowerCase();
                return switch (mode) {
                    case "starts_with" -> itemText.startsWith(searchText);
                    case "fuzzy" -> fuzzyMatch(itemText, searchText);
                    default -> itemText.contains(searchText);
                };
            })
            .collect(Collectors.toList());
    }

    /**
     * Simple fuzzy matching algorithm for filtering option.
     *
     * @param text    Text to search in
     * @param pattern Pattern to search for
     * @return true if pattern fuzzy matches text
     */
    private boolean fuzzyMatch(final String text, final String pattern) {
        int textIndex = 0;
        int patternIndex = 0;

        while (textIndex < text.length() && patternIndex < pattern.length()) {
            if (text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                patternIndex++;
            }
            textIndex++;
        }

        return patternIndex == pattern.length();
    }
}
