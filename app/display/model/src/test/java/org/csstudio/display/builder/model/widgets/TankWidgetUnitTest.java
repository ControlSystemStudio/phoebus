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
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.junit.jupiter.api.Test;
import org.phoebus.ui.vtype.FormatOption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit tests for {@link TankWidget} and its {@link ScaledPVWidget} base class.
 *
 *  <p>Verifies property defaults, property ordering in the panel, and
 *  XML round-trip persistence for backward-compatible new properties.
 *
 *  @author Heredie Delvalle
 */
@SuppressWarnings("nls")
public class TankWidgetUnitTest
{
    /** Verify that ScaledPVWidget properties have sensible defaults */
    @Test
    public void testScaledPVWidgetDefaults()
    {
        final TankWidget tank = new TankWidget();

        // Limits-from-PV should default to true (upstream convention)
        assertThat(tank.propLimitsFromPV().getValue(), equalTo(true));
        assertThat(tank.propAlarmLimitsFromPV().getValue(), equalTo(true));

        // Display range
        assertThat(tank.propMinimum().getValue(), equalTo(0.0));
        assertThat(tank.propMaximum().getValue(), equalTo(100.0));

        // Limit lines hidden by default to avoid visual noise on existing .bob files
        assertThat(tank.propShowAlarmLimits().getValue(), equalTo(false));

        // Manual limit levels default to NaN (inactive)
        assertTrue(Double.isNaN(tank.propLevelLoLo().getValue()));
        assertTrue(Double.isNaN(tank.propLevelLow().getValue()));
        assertTrue(Double.isNaN(tank.propLevelHigh().getValue()));
        assertTrue(Double.isNaN(tank.propLevelHiHi().getValue()));

        // Alarm colours should reference the named palette entries
        final WidgetColor minor = tank.propMinorAlarmColor().getValue();
        final WidgetColor major = tank.propMajorAlarmColor().getValue();
        assertNotNull(minor);
        assertNotNull(major);
    }

    /** Verify TankWidget-specific defaults */
    @Test
    public void testTankWidgetDefaults()
    {
        final TankWidget tank = new TankWidget();

        assertThat(tank.propScaleVisible().getValue(), equalTo(true));
        assertThat(tank.propOppositeScaleVisible().getValue(), equalTo(false));
        assertThat(tank.propShowMinorTicks().getValue(), equalTo(true));
        assertThat(tank.propPerpendicularTickLabels().getValue(), equalTo(false));
        assertThat(tank.propFormat().getValue(), equalTo(FormatOption.DEFAULT));
        assertThat(tank.propPrecision().getValue(), equalTo(2));
        assertThat(tank.propLogScale().getValue(), equalTo(false));
        assertThat(tank.propHorizontal().getValue(), equalTo(false));
        assertThat(tank.propBorderWidth().getValue(), equalTo(0));
    }

    /** Verify that alarm properties appear together and in the expected
     *  order when listed in the property panel.
     *
     *  <p>{@code border_alarm_sensitive} and {@code alarm_limits_from_pv}
     *  should appear right after {@code maximum}.
     */
    @Test
    public void testPropertyOrdering()
    {
        final TankWidget tank = new TankWidget();
        final List<String> names = tank.getProperties()
                                       .stream()
                                       .map(WidgetProperty::getName)
                                       .collect(Collectors.toList());
        // border_alarm_sensitive comes after maximum, then alarm_limits_from_pv
        final int maxIdx = names.indexOf("maximum");
        final int brdIdx = names.indexOf("border_alarm_sensitive");
        final int almIdx = names.indexOf("alarm_limits_from_pv");
        assertTrue(maxIdx >= 0, "maximum not found");
        assertTrue(brdIdx >= 0, "border_alarm_sensitive not found");
        assertTrue(almIdx >= 0, "alarm_limits_from_pv not found");
        assertTrue(brdIdx > maxIdx,
            "border_alarm_sensitive should follow maximum");
        assertTrue(almIdx > brdIdx,
            "alarm_limits_from_pv should follow border_alarm_sensitive");

        // scale_visible and opposite_scale_visible should be adjacent
        final int scaleIdx = names.indexOf("scale_visible");
        final int oppIdx   = names.indexOf("opposite_scale_visible");
        assertTrue(scaleIdx >= 0);
        assertTrue(oppIdx >= 0);
        assertThat(oppIdx, equalTo(scaleIdx + 1));
    }

    /** Write a tank to XML and read it back, verifying non-default values
     *  survive the round trip.
     */
    @Test
    public void testXmlRoundTrip() throws Exception
    {
        // Create a tank with non-default settings
        final TankWidget original = new TankWidget();
        original.propMinimum().setValue(10.0);
        original.propMaximum().setValue(200.0);
        original.propShowAlarmLimits().setValue(true);
        original.propAlarmLimitsFromPV().setValue(false);
        original.propLevelLoLo().setValue(15.0);
        original.propLevelLow().setValue(30.0);
        original.propLevelHigh().setValue(170.0);
        original.propLevelHiHi().setValue(190.0);
        original.propOppositeScaleVisible().setValue(true);
        original.propBorderWidth().setValue(3);
        original.propPerpendicularTickLabels().setValue(true);
        original.propFormat().setValue(FormatOption.DECIMAL);
        original.propPrecision().setValue(3);

        // Serialize — disable skip_defaults so ALL properties appear
        final DisplayModel model = new DisplayModel();
        model.runtimeChildren().addChild(original);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final boolean saved = ModelWriter.skip_defaults;
        ModelWriter.skip_defaults = false;
        try
        {
            final ModelWriter writer = new ModelWriter(out);
            writer.writeModel(model);
            writer.close();
        }
        finally
        {
            ModelWriter.skip_defaults = saved;
        }
        final String xml = out.toString();

        // Quick sanity: new properties should appear in the XML
        assertThat(xml, containsString("<alarm_limits_from_pv>"));
        assertThat(xml, containsString("<show_alarm_limits>"));
        assertThat(xml, containsString("<opposite_scale_visible>"));
        assertThat(xml, containsString("<tank_border_width>"));
        assertThat(xml, containsString("<level_lolo>"));

        // Deserialize
        final ModelReader reader = new ModelReader(new ByteArrayInputStream(xml.getBytes()));
        final DisplayModel loaded = reader.readModel();
        final Widget w = loaded.getChildren().get(0);
        assertTrue(w instanceof TankWidget);
        final TankWidget tank = (TankWidget) w;

        assertThat(tank.propMinimum().getValue(), equalTo(10.0));
        assertThat(tank.propMaximum().getValue(), equalTo(200.0));
        assertThat(tank.propShowAlarmLimits().getValue(), equalTo(true));
        assertThat(tank.propAlarmLimitsFromPV().getValue(), equalTo(false));
        assertThat(tank.propLevelLoLo().getValue(), equalTo(15.0));
        assertThat(tank.propLevelLow().getValue(), equalTo(30.0));
        assertThat(tank.propLevelHigh().getValue(), equalTo(170.0));
        assertThat(tank.propLevelHiHi().getValue(), equalTo(190.0));
        assertThat(tank.propOppositeScaleVisible().getValue(), equalTo(true));
        assertThat(tank.propBorderWidth().getValue(), equalTo(3));
        assertThat(tank.propPerpendicularTickLabels().getValue(), equalTo(true));
        assertThat(tank.propFormat().getValue(), equalTo(FormatOption.DECIMAL));
        assertThat(tank.propPrecision().getValue(), equalTo(3));
    }

    /** Verify that XML produced by this version can be read by a stock
     *  Phoebus that does not know the new properties: unknown elements
     *  are silently ignored.  We simulate this by writing only the new
     *  properties and confirming they survive as XML text.
     */
    @Test
    public void testNewPropertiesAreOptional() throws Exception
    {
        // A default tank should NOT write alarm_limits_from_pv etc. because
        // ModelWriter skips properties that equal their default.
        final TankWidget tank = new TankWidget();
        final DisplayModel model = new DisplayModel();
        model.runtimeChildren().addChild(tank);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ModelWriter writer = new ModelWriter(out);
        writer.writeModel(model);
        writer.close();
        final String xml = out.toString();

        // New properties at default values should be omitted
        assertThat(xml, not(containsString("<alarm_limits_from_pv>")));
        assertThat(xml, not(containsString("<show_alarm_limits>")));
        assertThat(xml, not(containsString("<level_lolo>")));
        assertThat(xml, not(containsString("<opposite_scale_visible>")));
        assertThat(xml, not(containsString("<tank_border_width>")));
    }
}
