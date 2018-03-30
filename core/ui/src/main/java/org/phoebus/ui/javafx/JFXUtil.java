/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javafx.scene.paint.Color;

/** JavaFX Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXUtil
{
    public static final Logger logger = Logger.getLogger(JFXUtil.class.getPackageName());

    private static final ConcurrentHashMap<Color, String> webRGBCache = new ConcurrentHashMap<>();

    /** Convert color into web-type RGB text
     *  @param color JavaFX {@link Color}
     *  @return RGB text of the form "#FF8080"
     */
    public static String webRGB(final Color color)
    {
        return webRGBCache.computeIfAbsent(color, col ->
        {
            // Compare com.sun.javafx.scene.control.skinUtils.formatHexString
            final int r = (int)Math.round(col.getRed() * 255.0);
            final int g = (int)Math.round(col.getGreen() * 255.0);
            final int b = (int)Math.round(col.getBlue() * 255.0);
            return String.format((Locale) null, "#%02X%02X%02X", r, g, b);
        });
    }
}
