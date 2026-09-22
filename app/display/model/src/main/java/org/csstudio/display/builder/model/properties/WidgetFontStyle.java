/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Description of a font style
 *  @author Kay Kasemir
 */
public enum WidgetFontStyle
{
    // Ordinals match legacy style bits based on SWT.BOLD, SWT.ITALIC
    /** Regular */
    REGULAR(Messages.FontStyle_Regular),
    /** Bold */
    BOLD(Messages.FontStyle_Bold),
    /** Italic */
    ITALIC(Messages.FontStyle_Italic),
    /** Bold and italic */
    BOLD_ITALIC(Messages.FontStyle_BoldItalic);

    private final String name;

    WidgetFontStyle(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Returns the WidgetFontStyle corresponding to the given name.
     * If the name does not match any enum constant, returns REGULAR.
     *
     * @param name The name of the font style
     * @return The corresponding WidgetFontStyle, or REGULAR if not found
     */
    public static WidgetFontStyle safeValueOf(String name) {
        try {
            return WidgetFontStyle.valueOf(name);
        } catch (IllegalArgumentException e) {
            Logger.getLogger(WidgetFontStyle.class.getName()).log(Level.WARNING, "Cannot find WidgetFontStyle", e);
            return REGULAR;
        }
    }
}
