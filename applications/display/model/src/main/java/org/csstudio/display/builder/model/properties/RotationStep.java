/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

/** Rotation, restricted to 90 degree steps
 *  @author Kay Kasemir
 */
public enum RotationStep
{
    NONE(Messages.Rotation_0, 0.0),
    NINETY(Messages.Rotation_90, 90.0),
    ONEEIGHTY(Messages.Rotation_180, 180.0),
    MINUS_NINETY(Messages.Rotation_270, 270.0);

    private final String label;
    private final double angle;

    private RotationStep(final String label, final double angle)
    {
        this.label = label;
        this.angle = angle;
    }

    /** Map angle to the closest rotation step
     *  @param angle Angle in degrees, counter-clockwise
     *  @return {@link RotationStep}
     */
    public static RotationStep forAngle(final double angle)
    {
        // Translate to 0 .. 360
        int norm = (int) Math.round(angle % 360.0);
        if (norm < 0)
            norm += 360;
        // Map angle to one of the possible options
        if (norm >=  90-45  &&  norm <  90+45)
            return NINETY;
        if (norm >= 180-45  &&  norm < 180+45)
            return ONEEIGHTY;
        if (norm >= 270-45  &&  norm < 270+45)
            return MINUS_NINETY;
        return NONE;
    }

    /** @return Rotation angle in degrees */
    public double getAngle()
    {
        return angle;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
