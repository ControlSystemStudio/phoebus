/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import org.phoebus.ui.vtype.ScaleFormat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit tests for {@link RTTank}.
 *
 *  <p>These are basic construction and API smoke tests.
 *  Full visual testing requires a running JavaFX toolkit (see TankDemo).
 *
 *  @author Heredie Delvalle
 */
@SuppressWarnings("nls")
public class RTTankTest
{
    /** RTTank must be constructable without a JavaFX toolkit for
     *  headless CI — Canvas extends Node but the constructor should
     *  not require a Stage.
     */
    @Test
    public void testConstruction()
    {
        // This will throw if the right_scale.setOnRight(true) ordering
        // is broken (NPE on update_throttle).
        final RTTank tank = new RTTank();
        assertThat(tank, not(nullValue()));
    }

    /** setRange should reject invalid ranges */
    @Test
    public void testSetRangeRejectsInvalid()
    {
        final RTTank tank = new RTTank();
        // Should silently ignore these — no exception
        tank.setRange(Double.NaN, 100);
        tank.setRange(0, Double.NaN);
        tank.setRange(100, 100);   // flat
        tank.setRange(100, 0);     // inverted
        tank.setRange(Double.POSITIVE_INFINITY, 100);
        // Initial range of the scale is unchanged
        assertThat(tank.getValueRange().getLow(), equalTo(0.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(10.0));
    }

    /** An inverted scale runs top-down, except on a log scale */
    @Test
    public void testInvertedScale()
    {
        final RTTank tank = new RTTank();
        tank.setRange(1, 100);
        tank.setInverted(true);
        assertThat(tank.getValueRange().getLow(), equalTo(100.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(1.0));

        // Inverted scale keeps the order when the range changes
        tank.setRange(2, 50);
        assertThat(tank.getValueRange().getLow(), equalTo(50.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(2.0));

        tank.setLogScale(true);
        assertThat(tank.getValueRange().getLow(), equalTo(2.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(50.0));

        tank.setLogScale(false);
        assertThat(tank.getValueRange().getLow(), equalTo(50.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(2.0));

        tank.setInverted(false);
        assertThat(tank.getValueRange().getLow(), equalTo(2.0));
        assertThat(tank.getValueRange().getHigh(), equalTo(50.0));
    }

    /** Values map to pixels bottom-up, or top-down when inverted */
    @Test
    public void testDrawnRangeValueToY()
    {
        final Rectangle plot = new Rectangle(0, 10, 20, 100);
        final RTTank.DrawnRange normal = new RTTank.DrawnRange(0, 100, false, true);
        assertThat(normal.valueToY(plot, 0), equalTo(110));
        assertThat(normal.valueToY(plot, 25), equalTo(85));

        final RTTank.DrawnRange inverted = new RTTank.DrawnRange(0, 100, false, false);
        assertThat(inverted.valueToY(plot, 0), equalTo(10));
        assertThat(inverted.valueToY(plot, 25), equalTo(35));

        final RTTank.DrawnRange log = new RTTank.DrawnRange(1, 100, true, true);
        assertThat(log.valueToY(plot, 10), equalTo(60));
        assertThat(log.valueToY(plot, 0), equalTo(-1));
    }

    /** setValue should handle NaN and Infinity */
    @Test
    public void testSetValueEdgeCases()
    {
        final RTTank tank = new RTTank();
        tank.setRange(0, 100);
        // Should not throw
        tank.setValue(Double.NaN);
        tank.setValue(Double.POSITIVE_INFINITY);
        tank.setValue(Double.NEGATIVE_INFINITY);
        tank.setValue(50);
    }

    /** setLimits should accept any combination of NaN values */
    @Test
    public void testSetLimits()
    {
        final RTTank tank = new RTTank();
        // All NaN — no lines drawn
        tank.setLimits(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        // Partial
        tank.setLimits(10, Double.NaN, 90, Double.NaN);
        // All set
        tank.setLimits(10, 20, 80, 90);
    }

    /** Verify the dual-scale toggle does not crash */
    @Test
    public void testDualScale()
    {
        final RTTank tank = new RTTank();
        tank.setScaleVisible(true);
        tank.setRightScaleVisible(true);
        // Both hidden
        tank.setScaleVisible(false);
        tank.setRightScaleVisible(false);
    }

    /** setLabelFormat with SIGNIFICANT should not throw */
    @Test
    public void testSignificantFormat()
    {
        final RTTank tank = new RTTank();
        // Should accept SIGNIFICANT without error
        tank.setLabelFormat(ScaleFormat.SIGNIFICANT, 3);
        tank.setLabelFormat(ScaleFormat.SIGNIFICANT, 1);
        // Switching back to other formats should also work
        tank.setLabelFormat(ScaleFormat.DECIMAL, 2);
        tank.setLabelFormat(ScaleFormat.DEFAULT, 0);
    }
}
