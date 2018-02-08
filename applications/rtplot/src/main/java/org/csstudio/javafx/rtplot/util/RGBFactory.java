/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

/** Attempt to generate many 'unique' colors.
 *  @author Kay Kasemir
 */
public class RGBFactory
{
    /** Subset of suggested colors */
    public static List<Color> PALETTE;

    static
    {
        PALETTE = new ArrayList<>(12);
        final RGBFactory rgb = new RGBFactory();
        for (int i=0; i<12; ++i)
            PALETTE.add(rgb.next());
    }

    private double hue = -120.0;
    private double saturation = 1.0;
    private double brightness = 1.0;

    public synchronized Color next()
    {   // Change hue, which gives the most dramatic difference
        hue += 120.0;
        if (hue >= 360)
        {   // Run different set of hues
            hue = (((int) hue)+30) % 360;
            if (hue == 120.0)
            {   // Cycle darker colors
                brightness -= 0.5;
                if (brightness <= 0.0)
                {   // All the same starting at a weaker and darker color
                    // This scheme will then be repeated...
                    hue = 0.0;
                    saturation = 0.7;
                    brightness = 0.9;
                }
            }
        }
        return Color.hsb(hue, saturation, brightness);
    }
}
