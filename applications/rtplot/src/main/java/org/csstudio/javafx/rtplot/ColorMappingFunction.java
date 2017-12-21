/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** Color mapping function
 *
 *  <p>Was introduced for optimization of ImagePlot,
 *  avoiding the creation of AWT Color classes
 *  by directly updating image buffer with ARGB values.
 *
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface ColorMappingFunction
{
    /** Gray scale color mapping */
    public final static ColorMappingFunction GRAYSCALE = value ->
    {
        final int level = (int) (value * 255 + 0.5);
        return getRGB(new int[] { level, level, level });
    };

    /** Returns the RGB value representing a color.
     *
     *  @param value Value 0.0 to 1.0 for which to determine the color
     *  @return RGB value of the color. Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     */
    public int getRGB(double value);

    /** Helper for creating RGB integer from components
     *  @param rgb [ red, green, blue ]
     *  @param green
     *  @param blue
     *  @return RGB value (including alpha set to fully opaque)
     */
    public static int getRGB(int[] rgb)
    {
        return 0xFF000000              |
               ((rgb[0] & 0xFF) << 16)  |
               ((rgb[1] & 0xFF) <<  8)  |
               ((rgb[2] & 0xFF));
    }
}
