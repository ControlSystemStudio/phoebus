/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Geometry info
 *
 *  Uses a geometry specification <code>{width}x{height}+{x}+{y}</code>,
 *  in a format similar to X11 <code>-geometry ..</code> argument.
 *
 *  For example <code>600x400+100+50</code> describes a window
 *  sized 600 by 400 located at X=100, Y=50.
 *
 *  It is also allowed to provide only <code>{width}x{height}</code>
 *  or <code>+{x}+{y}</code>
 */
 public class Geometry
 {
    // 09 x 09 + 09 + 09, group for each number, with both size and offset being optional
    private static final Pattern SIZE_AND_OFFSET = Pattern.compile("(([0-9]+)x([0-9]+))?(\\+([0-9]+)\\+([0-9]+))?");

    public int width = 1600;
    public int height = 900;
    public int x = 50;
    public int y = 50;

    /** @param spec geometry specification <code>{width}x{height}+{x}+{y}</code> */
    public Geometry(final String spec)
    {
        if (spec != null)
        {
            final Matcher matcher = SIZE_AND_OFFSET.matcher(spec);
            if (matcher.matches())
            {
                if (matcher.group(2) != null)
                    width = Integer.parseInt(matcher.group(2));
                if (matcher.group(3) != null)
                    height = Integer.parseInt(matcher.group(3));
                if (matcher.group(5) != null)
                    x = Integer.parseInt(matcher.group(5));
                if (matcher.group(6) != null)
                    y = Integer.parseInt(matcher.group(6));
            }
        }
    }

    @Override
    public String toString()
    {
        return width + "x" + height + "+" + x + "+" + y;
    }
 }
