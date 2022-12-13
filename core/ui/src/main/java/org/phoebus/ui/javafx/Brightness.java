/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.paint.Color;

/** Helper for dealing with brightness of a color
 *
 *  <p>Can be used to set color of a text to black or white
 *  based on the brightness of the background.
 *
 *  @author Kay Kasemir
 */
public class Brightness
{
    // Brightness weightings from BOY
    // https://github.com/ControlSystemStudio/cs-studio/blob/master/applications/opibuilder/opibuilder-plugins/org.csstudio.swt.widgets/src/org/csstudio/swt/widgets/figures/LEDFigure.java
    // Original RGB was 0..255 with dark/bright threshold 105000
    // JFX color uses RGB 0..1, so threshold becomes 105000/255 ~ 410

    /** Threshold for considering a color 'bright', suggesting black for text */
    public static final double BRIGHT_THRESHOLD = 410;

    /** Brightness differences below this are considered 'similar brightness' */
    public static final double SIMILARITY_THRESHOLD = 350;

    /** @param color Color
     *  @return Weighed brightness of that color
     */
    public static double of(final Color color)
    {
        return color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114;
    }
}
