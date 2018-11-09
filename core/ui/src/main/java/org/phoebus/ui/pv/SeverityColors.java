/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.pv;

import org.epics.vtype.AlarmSeverity;

import javafx.scene.paint.Color;

/** Suggested colors for representing alarm severities
 *  @author Kay Kasemir
 */
public class SeverityColors
{
    /** Text colors for alarm severity by ordinal */
    public static final Color[] SEVERITY_COLORS = new Color[]
    {
        Color.BLACK,
        new Color(0.8, 0.8, 0.0, 1.0), // Dark Yellow
        Color.RED,
        Color.GRAY,
        Color.DARKMAGENTA
    };

    /** @param severity {@link AlarmSeverity}
     *  @return Suggested text color
     */
    public static Color getTextColor(final AlarmSeverity severity)
    {
        return SEVERITY_COLORS[severity.ordinal()];
    }
}
