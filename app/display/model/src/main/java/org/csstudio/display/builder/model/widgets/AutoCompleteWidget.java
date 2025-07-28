/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

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
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

/**
 * A widget that provides auto‐completion functionality for a text field.
 * Users can supply a list of items to be used as suggestions. As the user types,
 * the widget filters those items according to configurable properties (e.g., filter mode,
 * case sensitivity), then displays up to a maximum number of suggestions.
 */
@SuppressWarnings("nls")
public class AutoCompleteWidget extends WritablePVWidget {

    /**
     * Widget descriptor
     */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("autocomplete", WidgetCategory.CONTROL,
            "Auto Complete",
            "/icons/autocomplete.png",
            "Text input with auto-completion from a large list of options",
            List.of("org.csstudio.opibuilder.widgets.AutoComplete")) {
            @Override
            public Widget createWidget() {
                return new AutoCompleteWidget();
            }
        };

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

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<String> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Boolean> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;
    private volatile WidgetProperty<Integer> max_suggestions;
    private volatile WidgetProperty<Integer> min_characters;
    private volatile WidgetProperty<Boolean> case_sensitive;
    private volatile WidgetProperty<String> placeholder;
    private volatile WidgetProperty<String> filter_mode;
    private volatile WidgetProperty<Boolean> allow_custom;
    private volatile List<String> itemsList = List.of();

    /**
     * Constructor of AutoComplete Widget
     */
    public AutoCompleteWidget() {
        super(WIDGET_DESCRIPTOR.getType(), 200, 30);
    }

    /**
     * Create customizable properties of the Widget (eg. filter mode, style, etc.).
     *
     * @param properties List to which properties must be added
     */
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties) {
        super.defineProperties(properties);
        properties.add(
            font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this,
            WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this,
            WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));

        properties.add(items = propItems.createProperty(this, ""));
        properties.add(items_from_pv = propItemsFromPV.createProperty(this, true));

        properties.add(max_suggestions = propMaxSuggestions.createProperty(this, 10));
        properties.add(min_characters = propMinCharacters.createProperty(this, 1));
        properties.add(case_sensitive = propCaseSensitive.createProperty(this, false));
        properties.add(placeholder = propPlaceholder.createProperty(this, "Type to search..."));
        properties.add(filter_mode = propFilterMode.createProperty(this,
            "fuzzy"));

        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(confirm_dialog = propConfirmDialog.createProperty(this, false));
        properties.add(confirm_message = propConfirmMessage.createProperty(this,
            "Are you sure you want to do this?"));
        properties.add(password = propPassword.createProperty(this, ""));
        properties.add(allow_custom = propCustom.createProperty(this, false));
    }

    /**
     * @return 'foreground_color' property
     */
    public WidgetProperty<WidgetColor> propForegroundColor() {
        return foreground;
    }

    /**
     * @return 'background_color' property
     */
    public WidgetProperty<WidgetColor> propBackgroundColor() {
        return background;
    }

    /**
     * @return 'font' property
     */
    public WidgetProperty<WidgetFont> propFont() {
        return font;
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
     * @return 'enabled' property
     */
    public WidgetProperty<Boolean> propEnabled() {
        return enabled;
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
     * @return 'max_suggestions' property
     */
    public WidgetProperty<Integer> propMaxSuggestions() {
        return max_suggestions;
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
