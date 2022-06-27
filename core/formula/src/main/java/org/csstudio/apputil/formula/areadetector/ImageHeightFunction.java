/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.areadetector;

import org.epics.vtype.VImage;

/** A formula function for fetching height of VImage
 *  @author Kay Kasemir
 */
public class ImageHeightFunction extends ImageWidthFunction
{
    @Override
    public String getName()
    {
        return "imageHeight";
    }

    @Override
    public String getDescription()
    {
        return "Fetch height of image";
    }

    @Override
    protected int getImageInfo(final VImage image)
    {
        return image.getHeight();
    }
}
