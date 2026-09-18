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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    /** The thermometer layout must produce a usable geometry for any size:
     *  tube and bulb inside the canvas, tube above the bulb, no exception */
    @Test
    public void testThermometerGeometry()
    {
        final RTTank tank = new RTTank();
        tank.setThermometerStyle(true);
        for (int[] size : new int[][] { { 1, 1 }, { 12, 40 }, { 24, 60 }, { 30, 30 }, { 40, 160 }, { 400, 600 } })
            for (int bulb : new int[] { 0, 20, 50, 500 })
                for (int padding : new int[] { 0, 20 })
                {
                    tank.setBulbSize(bulb);
                    tank.setInnerPadding(padding);
                    final Rectangle bounds = new Rectangle(0, 0, size[0], size[1]);
                    final RTTank.ThermoGeom geom = tank.thermoGeometry(bounds, new RTTank.ScaleSpace(30, 0, 5, 5));
                    final String what = size[0] + "x" + size[1] + " bulb " + bulb + " padding " + padding;
                    assertTrue(geom.tubeBottom() >= geom.tubeTop(), what + ": tube ends above its top");
                    assertTrue(geom.tubeWidth() >= 1, what + ": no tube");
                    assertTrue(geom.bulbRadius() > geom.tubeWidth() / 2, what + ": bulb narrower than tube");
                    // A canvas smaller than the minimum tube and bulb overflows, larger ones must not
                    if (size[0] >= 40 + 2 * padding  &&  size[1] >= 60 + 2 * padding)
                    {
                        assertTrue(geom.bulbCenterY() + geom.bulbRadius() <= bounds.height, what + ": bulb below the canvas");
                        assertTrue(geom.centerX() - geom.bulbRadius() >= 0, what + ": bulb left of the canvas");
                        assertTrue(geom.centerX() + geom.bulbRadius() <= bounds.width, what + ": bulb right of the canvas");
                        assertTrue(geom.tubeTop() >= 0, what + ": tube above the canvas");
                    }
                }
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
