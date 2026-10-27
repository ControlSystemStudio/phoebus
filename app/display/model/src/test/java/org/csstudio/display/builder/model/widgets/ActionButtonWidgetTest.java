/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
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
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.junit.jupiter.api.Test;
import org.phoebus.ui.color.NamedWidgetColor;
import org.phoebus.ui.color.NamedWidgetColors;
import org.phoebus.ui.color.WidgetColorService;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *Unit tests for {@link ActionButtonWidget}.
 */
@SuppressWarnings("nls")
public class ActionButtonWidgetTest
{
    /**
     * Verify ActionButton default property values.
     */
    @Test
    public void testActionButtonWidgetDefaults()
    {
        final ActionButtonWidget button = new ActionButtonWidget();

        assertThat(button.propText().getValue(), equalTo("No actions"));
        assertThat(button.propFont().getValue(), equalTo(WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        assertThat(button.propForegroundColor().getValue(), equalTo(WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        assertThat(button.propBackgroundColor().getValue(), equalTo(WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        assertThat(button.propTransparent().getValue(), equalTo(false));
        assertThat(button.propHorizontalAlignment().getValue(), equalTo(HorizontalAlignment.CENTER));
        assertThat(button.propVerticalAlignment().getValue(), equalTo(VerticalAlignment.MIDDLE));
        assertThat(button.propRotationStep().getValue(), equalTo(RotationStep.NONE));
        assertThat(button.propEnabled().getValue(), equalTo(true));
        assertThat(button.runtimePropPVWritable().getValue(), equalTo(true));
        assertThat(button.propConfirmDialog().getValue(), equalTo(false));
        assertThat(button.propConfirmMessage().getValue(), equalTo("Are your sure you want to do this?"));
        assertThat(button.propPassword().getValue(), equalTo(""));
        assertThat(button.propCornerRadius().getValue(), equalTo(3));
    }

    /**
     * Test that changed (i.e. non-default) properties persist
     * in the XML write/read.
     *
     * @throws Exception if cannot be converted
     */
    @Test
    public void testNonDefaultPropertiesInXml() throws Exception
    {
        // Test (non-default) values
        String text = "Testing";
        NamedWidgetColor backgroundColor = WidgetColorService.getColor(NamedWidgetColors.WRITE_BACKGROUND);
        NamedWidgetColor foregroundColor = WidgetColorService.getColor(NamedWidgetColors.ACTIVE_TEXT);
        NamedWidgetFont font = WidgetFontService.get(NamedWidgetFonts.DEFAULT_BOLD);
        HorizontalAlignment hAlign = HorizontalAlignment.LEFT;
        VerticalAlignment vAlign = VerticalAlignment.TOP;
        RotationStep rotation = RotationStep.NINETY;
        String confirmMessage = "Is this OK?";
        String password = "pw";
        int radii = 10;

        ActionButtonWidget button = new ActionButtonWidget();

        button.propText().setValue(text);
        button.propFont().setValue(font);
        button.propForegroundColor().setValue(foregroundColor);
        button.propBackgroundColor().setValue(backgroundColor);
        button.propTransparent().setValue(true);
        button.propHorizontalAlignment().setValue(hAlign);
        button.propVerticalAlignment().setValue(vAlign);
        button.propRotationStep().setValue(rotation);
        button.propEnabled().setValue(false);
        button.propConfirmDialog().setValue(true);
        button.propConfirmMessage().setValue(confirmMessage);
        button.propPassword().setValue(password);
        button.propCornerRadius().setValue(radii);

        final String xml = ModelWriter.getXML(List.of(button));

        final DisplayModel model = ModelReader.parseXML(xml);
        final List<Widget> widgets = model.getChildren();
        assertThat(widgets.size(), equalTo(1));
        assertThat(widgets.getFirst(), instanceOf(ActionButtonWidget.class));
        button = (ActionButtonWidget) widgets.getFirst();

        assertThat(button.propText().getValue(), equalTo(text));
        assertThat(button.propFont().getValue(), equalTo(font));
        assertThat(button.propForegroundColor().getValue(), equalTo(foregroundColor));
        assertThat(button.propBackgroundColor().getValue(), equalTo(backgroundColor));
        assertThat(button.propTransparent().getValue(), equalTo(true));
        assertThat(button.propHorizontalAlignment().getValue(), equalTo(hAlign));
        assertThat(button.propVerticalAlignment().getValue(), equalTo(vAlign));
        assertThat(button.propRotationStep().getValue(), equalTo(rotation));
        assertThat(button.propEnabled().getValue(), equalTo(false));
        assertThat(button.propConfirmDialog().getValue(), equalTo(true));
        assertThat(button.propConfirmMessage().getValue(), equalTo(confirmMessage));
        assertThat(button.propPassword().getValue(), equalTo(password));
        assertThat(button.propCornerRadius().getValue(), equalTo(radii));
    }

    /**
     * Test that the properties with default values do not get saved
     * to the XML.
     *
     * @throws Exception if cannot be converted
     */
    @Test
    public void testOptionalPropertiesInXml() throws Exception
    {
        ActionButtonWidget button = new ActionButtonWidget();
        final String xml = ModelWriter.getXML(List.of(button));

        assertThat(xml, not(containsString("<enabled>")));
        assertThat(xml, not(containsString("<text>")));
        assertThat(xml, not(containsString("<font>")));
        assertThat(xml, not(containsString("<foreground>")));
        assertThat(xml, not(containsString("<background>")));
        assertThat(xml, not(containsString("<transparent>")));
        assertThat(xml, not(containsString("<horizontal_alignment>")));
        assertThat(xml, not(containsString("<vertical_alignment>")));
        assertThat(xml, not(containsString("<rotation_step>")));
        assertThat(xml, not(containsString("<pv_writable>")));
        assertThat(xml, not(containsString("<confirm_dialog>")));
        assertThat(xml, not(containsString("<confirm_message>")));
        assertThat(xml, not(containsString("<password>")));
        assertThat(xml, not(containsString("<corner_radius>")));
    }
}
