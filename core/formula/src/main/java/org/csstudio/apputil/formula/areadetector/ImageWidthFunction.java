/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.areadetector;

import org.epics.vtype.Display;
import org.epics.vtype.VImage;
import org.epics.vtype.VInt;
import org.epics.vtype.VType;

/** A formula function for fetching width of VImage
 *  @author Kay Kasemir
 */
public class ImageWidthFunction extends ImageValueFunction
{
    @Override
    public String getName()
    {
        return "imageWidth";
    }

    @Override
    public String getDescription()
    {
        return "Fetch width of image";
    }

    /** Fetch info (width, height, ...) from image
     *
     *  Subclass can override
     *
     *  @param image Image
     *  @return info
     */
    protected int getImageInfo(final VImage image)
    {
        return image.getWidth();
    }

    @Override
    protected VType getImageData(final VImage image)
    {
        return VInt.of(getImageInfo(image), image.getAlarm(), image.getTime(), Display.none());
    }
}
