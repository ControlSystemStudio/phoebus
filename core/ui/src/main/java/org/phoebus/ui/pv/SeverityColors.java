/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.pv;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.vtype.AlarmSeverity;
import org.phoebus.ui.Preferences;

import javafx.scene.paint.Color;

/** Suggested colors for representing alarm severities
 *  @author Kay Kasemir
 */
public class SeverityColors
{
    /** Text colors for alarm severity by ordinal */
    public static final Color[] SEVERITY_TEXT_COLORS = new Color[]
    {
        colorOf(Preferences.ok_severity_text_color),
        colorOf(Preferences.minor_severity_text_color),
        colorOf(Preferences.major_severity_text_color),
        colorOf(Preferences.invalid_severity_text_color),
        colorOf(Preferences.undefined_severity_text_color)
    };

    /** Background colors for alarm severity by ordinal */
    public static final Color[] SEVERITY_BACKGROUND_COLORS = new Color[]
    {
        colorOf(Preferences.ok_severity_background_color),
        colorOf(Preferences.minor_severity_background_color),
        colorOf(Preferences.major_severity_background_color),
        colorOf(Preferences.invalid_severity_background_color),
        colorOf(Preferences.undefined_severity_background_color)
    };

    /** @param severity {@link AlarmSeverity}
     *  @return Suggested text color
     */
    public static Color getTextColor(final AlarmSeverity severity)
    {
        return SEVERITY_TEXT_COLORS[severity.ordinal()];
    }

    /** @param severity {@link AlarmSeverity}
     *  @return Suggested background color
     */
    public static Color getBackgroundColor(final AlarmSeverity severity)
    {
        return SEVERITY_BACKGROUND_COLORS[severity.ordinal()];
    }

    /** @param rgba RGB or RGBA
     *  @return {@link Color}
     */
    private static Color colorOf(int[] rgba)
    {
        if (rgba.length == 3)
            return Color.rgb(rgba[0], rgba[1], rgba[2]);
        else if (rgba.length == 4)
            return Color.rgb(rgba[0], rgba[1], rgba[2], rgba[3]/256.0);
        Logger.getLogger(SeverityColors.class.getPackageName())
              .log(Level.WARNING,
                   "Invalid severity text color " + Arrays.toString(rgba) +
                   ", expecting R, G, B or R, G, B, A");

        return Color.PINK;
    }
}
