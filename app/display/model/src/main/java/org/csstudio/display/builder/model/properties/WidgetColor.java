/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Description of a color
 *  @author Kay Kasemir
 */
// Implementation avoids AWT, SWT, JavaFX color
@SuppressWarnings("nls")
public class WidgetColor
{
    protected final int red, green, blue, alpha;

    /** Construct RGB color
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     *  @param alpha Alpha component, range {@code 0} (transparent) to {@code 255} (opaque)
     */
    public WidgetColor(final int red, final int green, final int blue, final int alpha)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    /** Construct RGB color
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     */
    public WidgetColor(final int red, final int green, final int blue)
    {
        this(red, green, blue, 255);
    }

    /** @return Red component, range {@code 0-255} */
    public int getRed()
    {
        return red;
    }

    /** @return Green component, range {@code 0-255} */
    public int getGreen()
    {
        return green;
    }

    /** @return Blue component, range {@code 0-255} */
    public int getBlue()
    {
        return blue;
    }

    /** @return Alpha, range {@code 0} (transparent) to {@code 255} (opaque) */
    public int getAlpha()
    {
        return alpha;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = blue;
        result = prime * result + green;
        result = prime * result + red;
        result = prime * result + alpha;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof WidgetColor))
            return false;
        final WidgetColor other = (WidgetColor) obj;
        return blue == other.blue   &&
               green == other.green &&
               red == other.red     &&
               alpha == other.alpha;
    }

    @Override
    public String toString()
    {
        if (alpha != 255)
            return "RGB(" + red + "," + green + "," + blue + "," + alpha + ")";
        return "RGB(" + red + "," + green + "," + blue + ")";
    }
}
