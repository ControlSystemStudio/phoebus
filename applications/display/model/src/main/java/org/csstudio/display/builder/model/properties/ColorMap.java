/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Map of values to colors
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ColorMap
{
    private final int[][] sections;
    private final int[][] colors = new int[256][];

    /** Initialize color map based on 'sections'.
     *
     *  <p>Each sections is a 4-tuple with these elements:
     *  <ul>
     *  <li>section[i][0]: Intensity level 0...255
     *  <li>section[i][1]: 'Red' 0..255
     *  <li>section[i][2]: 'Green' 0..255
     *  <li>section[i][3]: 'Blue' 0..255
     *  </ul>
     *  The color of a 'section' is used for the given
     *  Intensity level up to the intensity level of the
     *  next section.
     *  The first section, section[0], must have Intensity 0.
     *  The last section must have Intensity 255.
     *  Sections must be ordered by Intensity.
     *
     *  @param sections Sections of the map.
     *  @throws IllegalArgumentException on error
     */
    public ColorMap(final int sections[][]) throws IllegalArgumentException
    {
        if (sections.length < 2)
            throw new IllegalArgumentException("Need at least 2 sections for '0' and '255'");
        if (sections[0][0] != 0)
            throw new IllegalArgumentException("Intensity of first section must be 0");
        if (sections[sections.length-1][0] != 255)
            throw new IllegalArgumentException("Intensity of last section must be 255");

        this.sections = sections;
        for (int i = 0; i<sections.length-1; ++i)
        {
            final int start_intensity = sections[i][0];
            final int end_intensity = sections[i+1][0];
            for (int c=start_intensity; c<end_intensity; ++c)
                colors[c] = new int[]
                            {
                                interpolate(start_intensity, sections[i][1], end_intensity, sections[i+1][1], c),  // red
                                interpolate(start_intensity, sections[i][2], end_intensity, sections[i+1][2], c),  // green
                                interpolate(start_intensity, sections[i][3], end_intensity, sections[i+1][3], c)   // blue
                            };
        }
        // Loop goes just up to last section, not including last section
        final int last = sections.length - 1;
        colors[255] = new int[] { sections[last][1], sections[last][2], sections[last][3] };
    }

    /** @return Sections, i.e. 4-tuples with these elements:
     *  <ul>
     *  <li>section[i][0]: Intensity level 0...255
     *  <li>section[i][1]: 'Red' 0..255
     *  <li>section[i][2]: 'Green' 0..255
     *  <li>section[i][3]: 'Blue' 0..255
     *  </ul>
     */
    public int[][] getSections()
    {
        return sections;
    }

    /** @param intensity Intensity 0 to 255
     *  @return RGB for that intensity
     */
    public int[] getColor(final int intensity)
    {
        if (intensity <= 0)
            return colors[0];
        if (intensity >= 255)
            return colors[255];
        return colors[intensity];
    }

    /** @param intensity Intensity 0.0 to 1.0
     *  @return RGB for that intensity
     */
    public int[] getColor(final double intensity)
    {
        // Rounds to nearest of the 255 colors.
        // Could interpolate between them..
        return getColor((int) (intensity * 255.0 + 0.5));
    }

    /** Linear interpolation between two points (x/y)
     *
     * @param x0 One endpoint
     * @param y0
     * @param x0 Other endpoint
     * @param y1
     * @param x Desired location
     * @return Value at that location
     */
    private static int interpolate(final int x0,  final int y0, final int x1, final int y1, final int x)
    {
        // Avoid div/0
        if (x0 == x1)
            return y0;
        return y0 +  (y1 - y0) * (x - x0) / (x1 - x0);
    }
}
