/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.junit.jupiter.api.Test;
import org.phoebus.ui.color.WidgetColor;
import org.phoebus.ui.vtype.ScaleFormat;

/** JUnit tests for {@link ProgressBarWidget} as a {@link ScaledPVWidget}
 *
 *  <p>The progress bar must keep loading the files written before it had
 *  a scale, and files written now must not confuse an older Phoebus.
 */
@SuppressWarnings("nls")
public class ProgressBarWidgetUnitTest
{
    /** Defaults, in particular those that the stock renderer relies on */
    @Test
    public void testDefaults()
    {
        final ProgressBarWidget bar = new ProgressBarWidget();

        assertThat(bar.propWidth().getValue(), equalTo(100));
        assertThat(bar.propHeight().getValue(), equalTo(20));
        assertThat(bar.propHorizontal().getValue(), equalTo(true));
        assertThat(bar.propLimitsFromPV().getValue(), equalTo(true));
        assertThat(bar.propMinimum().getValue(), equalTo(0.0));
        assertThat(bar.propMaximum().getValue(), equalTo(100.0));
        assertThat(bar.propLogScale().getValue(), equalTo(false));
        assertThat(bar.propFillColor().getValue(), equalTo(new WidgetColor(60, 255, 60)));
        assertThat(bar.propBackgroundColor().getValue(), equalTo(new WidgetColor(250, 250, 250)));

        // A bar looks like a bar until a scale is asked for
        assertThat(bar.propScaleVisible().getValue(), equalTo(false));
        assertThat(bar.propShowMinorTicks().getValue(), equalTo(true));
        assertThat(bar.propShowScaleLabels().getValue(), equalTo(true));
        assertThat(bar.propOppositeScaleVisible().getValue(), equalTo(false));
        assertThat(bar.propPerpendicularTickLabels().getValue(), equalTo(false));
        assertThat(bar.propBorderWidth().getValue(), equalTo(0));
        assertThat(bar.propInnerPadding().getValue(), equalTo(3));
        assertThat(bar.propFormat().getValue(), equalTo(ScaleFormat.DEFAULT));
        assertThat(bar.propShowAlarmLimits().getValue(), equalTo(false));
    }

    /** Every name that the property panel hides for the stock renderer must be a real property */
    @Test
    public void testScaleModePropertiesExist()
    {
        final ProgressBarWidget bar = new ProgressBarWidget();
        for (String name : ProgressBarWidget.SCALE_MODE_PROPS)
            assertTrue(bar.checkProperty(name).isPresent(), "Unknown property " + name);
    }

    /** A file written before the progress bar had a scale must load as before */
    @Test
    public void testLegacyFileLoads() throws Exception
    {
        final String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<display version=\"2.0.0\">\n" +
            "  <widget type=\"progressbar\" version=\"2.0.0\">\n" +
            "    <name>Bar</name>\n" +
            "    <pv_name>loc://x</pv_name>\n" +
            "    <fill_color><color red=\"10\" green=\"20\" blue=\"30\"></color></fill_color>\n" +
            "    <background_color><color red=\"1\" green=\"2\" blue=\"3\"></color></background_color>\n" +
            "    <limits_from_pv>false</limits_from_pv>\n" +
            "    <minimum>5.0</minimum>\n" +
            "    <maximum>50.0</maximum>\n" +
            "    <log_scale>true</log_scale>\n" +
            "    <horizontal>false</horizontal>\n" +
            "  </widget>\n" +
            "</display>";
        final ProgressBarWidget bar = (ProgressBarWidget) read(xml);

        assertThat(bar.propFillColor().getValue(), equalTo(new WidgetColor(10, 20, 30)));
        assertThat(bar.propBackgroundColor().getValue(), equalTo(new WidgetColor(1, 2, 3)));
        assertThat(bar.propLimitsFromPV().getValue(), equalTo(false));
        assertThat(bar.propMinimum().getValue(), equalTo(5.0));
        assertThat(bar.propMaximum().getValue(), equalTo(50.0));
        assertThat(bar.propLogScale().getValue(), equalTo(true));
        assertThat(bar.propHorizontal().getValue(), equalTo(false));
        // Scale properties keep their defaults
        assertThat(bar.propScaleVisible().getValue(), equalTo(false));
    }

    /** Non-default values of the new properties survive save and load */
    @Test
    public void testXmlRoundTrip() throws Exception
    {
        final ProgressBarWidget original = new ProgressBarWidget();
        original.propScaleVisible().setValue(true);
        original.propShowScaleLabels().setValue(false);
        original.propOppositeScaleVisible().setValue(true);
        original.propPerpendicularTickLabels().setValue(true);
        original.propBorderWidth().setValue(2);
        original.propInnerPadding().setValue(7);
        original.propFormat().setValue(ScaleFormat.EXPONENTIAL);
        original.propPrecision().setValue(1);
        original.propShowAlarmLimits().setValue(true);
        original.propAlarmLimitsFromPV().setValue(false);
        original.propLevelHigh().setValue(80.0);
        original.propMinimum().setValue(100.0);
        original.propMaximum().setValue(0.0);

        final String xml = write(original, false);
        assertThat(xml, containsString("<scale_visible>"));
        assertThat(xml, containsString("<tank_border_width>"));
        assertThat(xml, containsString("<inner_padding>"));

        final ProgressBarWidget bar = (ProgressBarWidget) read(xml);
        assertThat(bar.propScaleVisible().getValue(), equalTo(true));
        assertThat(bar.propShowScaleLabels().getValue(), equalTo(false));
        assertThat(bar.propOppositeScaleVisible().getValue(), equalTo(true));
        assertThat(bar.propPerpendicularTickLabels().getValue(), equalTo(true));
        assertThat(bar.propBorderWidth().getValue(), equalTo(2));
        assertThat(bar.propInnerPadding().getValue(), equalTo(7));
        assertThat(bar.propFormat().getValue(), equalTo(ScaleFormat.EXPONENTIAL));
        assertThat(bar.propPrecision().getValue(), equalTo(1));
        assertThat(bar.propShowAlarmLimits().getValue(), equalTo(true));
        assertThat(bar.propAlarmLimitsFromPV().getValue(), equalTo(false));
        assertThat(bar.propLevelHigh().getValue(), equalTo(80.0));
        assertThat(bar.propMinimum().getValue(), equalTo(100.0));
        assertThat(bar.propMaximum().getValue(), equalTo(0.0));
    }

    /** Only changed properties are written, so a file that uses the
     *  pre-existing properties looks the same as before */
    @Test
    public void testLegacyPropertiesWriteNoNewElements() throws Exception
    {
        final ProgressBarWidget bar = new ProgressBarWidget();
        bar.propFillColor().setValue(new WidgetColor(1, 2, 3));
        bar.propMinimum().setValue(5.0);
        bar.propMaximum().setValue(50.0);
        bar.propLogScale().setValue(true);
        bar.propHorizontal().setValue(false);
        final String xml = write(bar, true);
        for (String name : List.of("fill_color", "minimum", "maximum", "log_scale", "horizontal"))
            assertThat(xml, containsString("<" + name + ">"));
        for (String name : ProgressBarWidget.SCALE_MODE_PROPS)
            assertThat(xml, not(containsString("<" + name + ">")));
    }

    /** A BOY progress bar is imported with its scale and levels, and the
     *  generic BOY 'border_width' element does not become a bar border */
    @Test
    public void testBoyFileLoads() throws Exception
    {
        final String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<display typeId=\"org.csstudio.opibuilder.Display\" version=\"1.0.0\">\n" +
            "  <widget typeId=\"org.csstudio.opibuilder.widgets.progressbar\" version=\"1.0.0\">\n" +
            "    <name>Bar</name>\n" +
            "    <pv_name>loc://x</pv_name>\n" +
            "    <x>10</x>\n" +
            "    <y>10</y>\n" +
            "    <width>200</width>\n" +
            "    <height>60</height>\n" +
            "    <border_style>0</border_style>\n" +
            "    <border_width>1</border_width>\n" +
            "    <show_scale>false</show_scale>\n" +
            "    <show_markers>true</show_markers>\n" +
            "    <show_label>false</show_label>\n" +
            "    <level_hi>80.0</level_hi>\n" +
            "    <level_hihi>90.0</level_hihi>\n" +
            "    <level_lo>20.0</level_lo>\n" +
            "    <level_lolo>10.0</level_lolo>\n" +
            "    <limits_from_pv>false</limits_from_pv>\n" +
            "    <minimum>0.0</minimum>\n" +
            "    <maximum>50.0</maximum>\n" +
            "  </widget>\n" +
            "</display>";
        final ProgressBarWidget bar = (ProgressBarWidget) read(xml);

        assertThat(bar.propBorderWidth().getValue(), equalTo(0));
        assertThat(bar.propScaleVisible().getValue(), equalTo(false));
        assertThat(bar.propLevelHigh().getValue(), equalTo(80.0));
        assertThat(bar.propLevelHiHi().getValue(), equalTo(90.0));
        assertThat(bar.propLevelLow().getValue(), equalTo(20.0));
        assertThat(bar.propLevelLoLo().getValue(), equalTo(10.0));
        assertThat(bar.propLimitsFromPV().getValue(), equalTo(false));
        assertThat(bar.propMaximum().getValue(), equalTo(50.0));
        // BOY reserved 25 px above the bar for the markers
        assertThat(bar.propY().getValue(), equalTo(35));
        assertThat(bar.propHeight().getValue(), equalTo(35));
    }

    private static String write(final Widget widget, final boolean skipDefaults) throws Exception
    {
        final DisplayModel model = new DisplayModel();
        model.runtimeChildren().addChild(widget);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final boolean saved = ModelWriter.skip_defaults;
        ModelWriter.skip_defaults = skipDefaults;
        try (ModelWriter writer = new ModelWriter(out))
        {
            writer.writeModel(model);
        }
        finally
        {
            ModelWriter.skip_defaults = saved;
        }
        return out.toString();
    }

    private static Widget read(final String xml) throws Exception
    {
        final ModelReader reader = new ModelReader(new ByteArrayInputStream(xml.getBytes()));
        return reader.readModel().getChildren().get(0);
    }
}
