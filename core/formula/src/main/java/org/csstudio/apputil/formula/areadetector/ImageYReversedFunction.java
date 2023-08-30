/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.areadetector;

import org.epics.vtype.VImage;

/** A formula function for fetching vertical reversal of VImage
 *  @author Kay Kasemir
 */
public class ImageYReversedFunction extends ImageXReversedFunction
{
    @Override
    public String getName()
    {
        return "imageYReversed";
    }

    @Override
    public String getDescription()
    {
        return "Fetch vertical reversal of image";
    }

    @Override
    protected boolean getImageInfo(final VImage image)
    {
        return image.isYReversed();
    }
}
