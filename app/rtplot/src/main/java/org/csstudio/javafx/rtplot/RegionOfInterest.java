/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Region of Interest
 *
 *  <p>Region within an {@link RTImagePlot}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RegionOfInterest
{
    private final String name;
    private final Color color;
    private volatile Image image = null;
    private volatile boolean visible, interactive;
    private volatile Rectangle2D region;

    /** Not meant to be called by user.
     *  Call {@link RTImagePlot#addROI()} to create ROI
     */
    public RegionOfInterest(final String name, final Color color,
                            final boolean visible,
                            final boolean interactive,
                            final double x, final double y, final double width, final double height)
    {
        this.name = name;
        this.color = color;
        this.visible = visible;
        this.interactive = interactive;
        this.region = new Rectangle2D(x, y, width, height);
    }

    /** @return Name of the region */
    public String getName()
    {
        return name;
    }

    /** @return Color of the region */
    public Color getColor()
    {
        return color;
    }

    /** @param image Image to show (instead of colored border) */
    public void setImage(final Image image)
    {
        this.image = image;
    }

    /** @return Image to show (instead of colored border) */
    public Image getImage()
    {
        return image;
    }

    /** @return Is region visible? */
    public boolean isVisible()
    {
        return visible;
    }

    /** @param visible Should region be visible? */
    public void setVisible(final boolean visible)
    {
        this.visible = visible;
        // Caller needs to request update of image
    }

    /** @param interactive Should region be movable? */
    public void setInteractive(final boolean interactive)
    {
        this.interactive = interactive;
    }

    /** @return Is region interactive? */
    public boolean isInteractive()
    {
        return interactive;
    }

    /** @return Region of interest within image */
    public Rectangle2D getRegion()
    {
        return region;
    }

    /** @param region Region of interest within image */
    public void setRegion(final Rectangle2D region)
    {
        this.region = region;
        // Caller needs to request update of image
    }

    @Override
    public String toString()
    {
        return "ROI '" + name + "': interactive=" + interactive;
    }
}
