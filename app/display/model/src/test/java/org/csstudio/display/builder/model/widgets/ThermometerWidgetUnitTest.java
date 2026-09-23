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

/** JUnit tests for {@link ThermometerWidget} as a {@link ScaledPVWidget}
 *
 *  <p>The thermometer must keep loading the files written before it had
 *  a scale, and files written now must not confuse an older Phoebus.
 */
@SuppressWarnings("nls")
public class ThermometerWidgetUnitTest
{
    /** Defaults, in particular those that the stock renderer relies on */
    @Test
    public void testDefaults()
    {
        final ThermometerWidget thermo = new ThermometerWidget();

        assertThat(thermo.propWidth().getValue(), equalTo(40));
        assertThat(thermo.propHeight().getValue(), equalTo(160));
        assertThat(thermo.propLimitsFromPV().getValue(), equalTo(true));
        assertThat(thermo.propMinimum().getValue(), equalTo(0.0));
        assertThat(thermo.propMaximum().getValue(), equalTo(100.0));
        assertThat(thermo.propFillColor().getValue(), equalTo(new WidgetColor(60, 255, 60)));

        // Looks like the stock thermometer until a scale is asked for;
        // horizontal labels read naturally next to a vertical tube
        assertThat(thermo.propScaleVisible().getValue(), equalTo(false));
        assertThat(thermo.propPerpendicularTickLabels().getValue(), equalTo(true));
        assertThat(thermo.propShowMinorTicks().getValue(), equalTo(true));
        assertThat(thermo.propShowScaleLabels().getValue(), equalTo(true));
        assertThat(thermo.propOppositeScaleVisible().getValue(), equalTo(false));
        assertThat(thermo.propLogScale().getValue(), equalTo(false));
        assertThat(thermo.propBorderWidth().getValue(), equalTo(0));
        assertThat(thermo.propEmptyColor().getValue(), equalTo(new WidgetColor(250, 250, 250)));
        assertThat(thermo.propBulbSize().getValue(), equalTo(20));
        assertThat(thermo.propFormat().getValue(), equalTo(ScaleFormat.DEFAULT));
        assertThat(thermo.propShowAlarmLimits().getValue(), equalTo(false));
    }

    /** Every name that the property panel hides for the stock renderer must be a real property */
    @Test
    public void testScaleModePropertiesExist()
    {
        final ThermometerWidget thermo = new ThermometerWidget();
        for (String name : ThermometerWidget.SCALE_MODE_PROPS)
            assertTrue(thermo.checkProperty(name).isPresent(), "Unknown property " + name);
    }

    /** A file written before the thermometer had a scale must load as before */
    @Test
    public void testLegacyFileLoads() throws Exception
    {
        final String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<display version=\"2.0.0\">\n" +
            "  <widget type=\"thermometer\" version=\"2.0.0\">\n" +
            "    <name>Thermo</name>\n" +
            "    <pv_name>loc://x</pv_name>\n" +
            "    <fill_color><color red=\"10\" green=\"20\" blue=\"30\"></color></fill_color>\n" +
            "    <limits_from_pv>false</limits_from_pv>\n" +
            "    <minimum>-10.0</minimum>\n" +
            "    <maximum>40.0</maximum>\n" +
            "  </widget>\n" +
            "</display>";
        final ThermometerWidget thermo = (ThermometerWidget) read(xml);

        assertThat(thermo.propFillColor().getValue(), equalTo(new WidgetColor(10, 20, 30)));
        assertThat(thermo.propLimitsFromPV().getValue(), equalTo(false));
        assertThat(thermo.propMinimum().getValue(), equalTo(-10.0));
        assertThat(thermo.propMaximum().getValue(), equalTo(40.0));
        assertThat(thermo.propBulbSize().getValue(), equalTo(20));
    }

    /** Non-default values of the new properties survive save and load */
    @Test
    public void testXmlRoundTrip() throws Exception
    {
        final ThermometerWidget original = new ThermometerWidget();
        original.propScaleVisible().setValue(true);
        original.propOppositeScaleVisible().setValue(true);
        original.propLogScale().setValue(true);
        original.propBorderWidth().setValue(1);
        original.propBulbSize().setValue(35);
        original.propEmptyColor().setValue(new WidgetColor(1, 2, 3));
        original.propFormat().setValue(ScaleFormat.DECIMAL);
        original.propPrecision().setValue(0);
        original.propShowAlarmLimits().setValue(true);
        original.propLevelLoLo().setValue(5.0);

        final String xml = write(original, false);
        assertThat(xml, containsString("<bulb_size>"));
        assertThat(xml, containsString("<scale_visible>"));

        final ThermometerWidget thermo = (ThermometerWidget) read(xml);
        assertThat(thermo.propScaleVisible().getValue(), equalTo(true));
        assertThat(thermo.propOppositeScaleVisible().getValue(), equalTo(true));
        assertThat(thermo.propLogScale().getValue(), equalTo(true));
        assertThat(thermo.propBorderWidth().getValue(), equalTo(1));
        assertThat(thermo.propBulbSize().getValue(), equalTo(35));
        assertThat(thermo.propEmptyColor().getValue(), equalTo(new WidgetColor(1, 2, 3)));
        assertThat(thermo.propFormat().getValue(), equalTo(ScaleFormat.DECIMAL));
        assertThat(thermo.propPrecision().getValue(), equalTo(0));
        assertThat(thermo.propShowAlarmLimits().getValue(), equalTo(true));
        assertThat(thermo.propLevelLoLo().getValue(), equalTo(5.0));
    }

    /** Only changed properties are written, so a file that uses the
     *  pre-existing properties looks the same as before */
    @Test
    public void testLegacyPropertiesWriteNoNewElements() throws Exception
    {
        final ThermometerWidget thermo = new ThermometerWidget();
        thermo.propFillColor().setValue(new WidgetColor(1, 2, 3));
        thermo.propLimitsFromPV().setValue(false);
        thermo.propMinimum().setValue(5.0);
        thermo.propMaximum().setValue(50.0);
        final String xml = write(thermo, true);
        for (String name : List.of("fill_color", "limits_from_pv", "minimum", "maximum"))
            assertThat(xml, containsString("<" + name + ">"));
        for (String name : ThermometerWidget.SCALE_MODE_PROPS)
            assertThat(xml, not(containsString("<" + name + ">")));
    }

    /** A BOY thermometer is imported as before; its generic BOY
     *  'border_width' element does not become a glass outline */
    @Test
    public void testBoyFileLoads() throws Exception
    {
        final String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<display typeId=\"org.csstudio.opibuilder.Display\" version=\"1.0.0\">\n" +
            "  <widget typeId=\"org.csstudio.opibuilder.widgets.thermometer\" version=\"1.0.0\">\n" +
            "    <name>Thermo</name>\n" +
            "    <pv_name>loc://x</pv_name>\n" +
            "    <border_style>0</border_style>\n" +
            "    <border_width>1</border_width>\n" +
            "    <fill_color><color red=\"10\" green=\"20\" blue=\"30\"></color></fill_color>\n" +
            "    <limits_from_pv>false</limits_from_pv>\n" +
            "    <minimum>-10.0</minimum>\n" +
            "    <maximum>40.0</maximum>\n" +
            "  </widget>\n" +
            "</display>";
        final ThermometerWidget thermo = (ThermometerWidget) read(xml);

        assertThat(thermo.propBorderWidth().getValue(), equalTo(0));
        assertThat(thermo.propFillColor().getValue(), equalTo(new WidgetColor(10, 20, 30)));
        assertThat(thermo.propLimitsFromPV().getValue(), equalTo(false));
        assertThat(thermo.propMinimum().getValue(), equalTo(-10.0));
        assertThat(thermo.propMaximum().getValue(), equalTo(40.0));
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
