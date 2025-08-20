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

/** JUnit test of TextEntry widget
 */
@SuppressWarnings("nls")
public class TextEntryWidgetTest
{
    private TextEntryWidget newWidgetWithItems(List<String> items)
    {
        final TextEntryWidget w = new TextEntryWidget();
        w.setItems(items);
        return w;
    }

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

    @Test
    public void testFuzzyMatching()
    {
        final TextEntryWidget w = newWidgetWithItems(List.of("cartwheel", "carthorse", "cow", "chart"));

        w.propMinCharacters().setValue(3);
        w.propCaseSensitive().setValue(false);
        w.propFilterMode().setValue("fuzzy");

        // 'crt' matches any item that contains 'c', then later 'r', then later 't'
        final List<String> result = w.getFilteredSuggestions("crt");
        assertThat(result, equalTo(List.of("cartwheel", "carthorse", "chart")));
    }

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
