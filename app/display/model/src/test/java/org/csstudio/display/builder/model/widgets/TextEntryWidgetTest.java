/*******************************************************************************
 * Copyright (c) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * JUnit test suite for TextEntryWidget functionality.
 * This test class validates the TextEntryWidget's filtering and suggestion
 * capabilities, including various search modes, case sensitivity options,
 * minimum character requirements, and XML serialization/deserialization.
 */
@SuppressWarnings("nls")
public class TextEntryWidgetTest
{
    /**
     * Creates a new TextEntryWidget instance with the specified suggestion items.
     *
     * @param items the list of suggestion items to set on the widget
     * @return a new TextEntryWidget configured with the provided items
     */
    private TextEntryWidget newWidgetWithItems(List<String> items)
    {
        final TextEntryWidget w = new TextEntryWidget();
        w.setItems(items);
        return w;
    }

    /**
     * Tests case-insensitive substring matching functionality.
     * Verifies that the widget correctly filters suggestions using case-insensitive
     * "contains" mode. The test ensures that partial matches are found regardless
     * of character case in both the input and the suggestion items.
     */
    @Test
    public void testContainsCaseInsensitive()
    {
        final TextEntryWidget w = newWidgetWithItems(List.of("Alpha", "beta", "ALPHABET", "gamma"));

        w.propMinCharacters().setValue(2);
        w.propCaseSensitive().setValue(false);
        w.propFilterMode().setValue("contains");

        final List<String> result = w.getFilteredSuggestions("ha");
        assertThat(result, equalTo(List.of("Alpha", "ALPHABET")));
    }

    /**
     * Tests case-sensitive prefix matching functionality.
     * Verifies that the widget correctly filters suggestions using case-sensitive
     * "starts with" mode. Only items that begin with the exact case-matched
     * input string should be included in the results.
     */
    @Test
    public void testStartsWithCaseSensitive()
    {
        final TextEntryWidget w = newWidgetWithItems(List.of("Foo", "Foobar", "foo", "FOO", "bar"));

        w.propMinCharacters().setValue(1);
        w.propCaseSensitive().setValue(true);
        w.propFilterMode().setValue("starts_with");

        final List<String> result = w.getFilteredSuggestions("Fo");
        assertThat(result, equalTo(List.of("Foo", "Foobar")));
    }

    /**
     * Tests fuzzy matching functionality.
     * Verifies that the widget correctly performs fuzzy matching where input
     * characters can match non-consecutive characters in suggestion items.
     * The fuzzy algorithm should find items containing all input characters
     * in the correct order, but not necessarily consecutively.
     */
    @Test
    public void testFuzzyMatching()
    {
        final TextEntryWidget w = newWidgetWithItems(List.of("cartwheel", "carthorse", "cow", "chart"));

        w.propMinCharacters().setValue(3);
        w.propCaseSensitive().setValue(false);
        w.propFilterMode().setValue("fuzzy");

        final List<String> result = w.getFilteredSuggestions("crt");
        assertThat(result, equalTo(List.of("cartwheel", "carthorse", "chart")));
    }

    /**
     * Tests minimum character requirement enforcement.
     * Verifies that the widget respects the minimum character setting and
     * returns no suggestions when the input length is below the configured
     * minimum threshold.
     */
    @Test
    public void testMinCharactersBlocksShortInput()
    {
        final TextEntryWidget w = newWidgetWithItems(List.of("one", "two", "three"));

        w.propMinCharacters().setValue(3);
        w.propCaseSensitive().setValue(false);
        w.propFilterMode().setValue("contains");

        final List<String> result = w.getFilteredSuggestions("ab");
        assertThat(result, equalTo(List.of()));
    }

    /**
     * Tests XML serialization and deserialization round-trip to ensure all
     * key TextEntryWidget properties are correctly preserved.
     * This test:
     * - Creates a TextEntryWidget with custom property values including
     *   suggestion items, filtering options, and UI settings
     * - Serializes it to XML using ModelWriter
     * - Deserializes it back using ModelReader
     * - Verifies that all critical properties retain their values after
     *   the round-trip operation
     * The test covers persistence of suggestion items, minimum character
     * requirements, case sensitivity, filter mode, placeholder text,
     * and custom input settings.
     */
    @Test
    public void testXMLRoundtripPreservesKeyProperties() throws Exception
    {
        TextEntryWidget w = new TextEntryWidget();
        w.setItems(List.of("one", "Two", "three"));
        w.propItems().setValue("one\nTwo\nthree");
        w.propMinCharacters().setValue(2);
        w.propCaseSensitive().setValue(true);
        w.propFilterMode().setValue("starts_with");
        w.propPlaceholder().setValue("Enter value…");
        w.propCustom().setValue(true);

        final String xml = ModelWriter.getXML(List.of(w));

        final DisplayModel model = ModelReader.parseXML(xml);
        final List<Widget> widgets = model.getChildren();
        assertThat(widgets.size(), equalTo(1));
        assertThat(widgets.get(0), instanceOf(TextEntryWidget.class));

        w = (TextEntryWidget) widgets.get(0);

        assertThat(w.propItems().getValue(), equalTo("one\nTwo\nthree"));
        assertThat(w.propMinCharacters().getValue(), equalTo(2));
        assertThat(w.propCaseSensitive().getValue(), equalTo(true));
        assertThat(w.propFilterMode().getValue(), equalTo("starts_with"));
        assertThat(w.propPlaceholder().getValue(), equalTo("Enter value…"));
        assertThat(w.propCustom().getValue(), equalTo(true));
    }
}
