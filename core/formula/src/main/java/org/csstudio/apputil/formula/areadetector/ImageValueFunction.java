/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.areadetector;

import java.util.Arrays;
import java.util.List;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Display;
import org.epics.vtype.VImage;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;

/** A formula function for fetching VImage array data
 *  @author Kay Kasemir
 */
public class ImageValueFunction implements FormulaFunction
{
    @Override
    public String getCategory()
    {
        return "areaDetector";
    }

    @Override
    public String getName()
    {
        return "imageValue";
    }

    @Override
    public String getDescription()
    {
        return "Fetch array data of image";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("image");
    }

    /** Fetch info from image
     *  @param image Image
     *  @return VType to return from function
     */
    protected VType getImageData(final VImage image)
    {
        return VNumberArray.of(image.getData(), image.getAlarm(), image.getTime(), Display.none());
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        if (args.length != 1 || ! (args[0] instanceof VImage))
            throw new Exception("Function " + getName() +
                    " takes VImage but received " + Arrays.toString(args));
        return getImageData((VImage) args[0]);
    }
}
