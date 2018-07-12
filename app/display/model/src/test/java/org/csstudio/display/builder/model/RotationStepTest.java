/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.model.properties.RotationStep;
import org.junit.Test;

/** JUnit test of {@link RotationStep}
 *
 *  @author Kay Kasemir
 */
public class RotationStepTest
{
    @Test
    public void testRotationForAngle()
    {
        // Exact matches
        assertThat(RotationStep.forAngle(0.0), equalTo(RotationStep.NONE));
        assertThat(RotationStep.forAngle(90.0), equalTo(RotationStep.NINETY));
        assertThat(RotationStep.forAngle(180.0), equalTo(RotationStep.ONEEIGHTY));
        assertThat(RotationStep.forAngle(270.0), equalTo(RotationStep.MINUS_NINETY));

        // Using the nearest option
        assertThat(RotationStep.forAngle(100.0), equalTo(RotationStep.NINETY));
        assertThat(RotationStep.forAngle(-90.0), equalTo(RotationStep.MINUS_NINETY));
        assertThat(RotationStep.forAngle(-100.0), equalTo(RotationStep.MINUS_NINETY));
        assertThat(RotationStep.forAngle(-10.0 - 100*360.0), equalTo(RotationStep.NONE));
    }
}
