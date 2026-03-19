/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.junit.jupiter.api.Test;

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
}
