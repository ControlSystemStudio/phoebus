/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.areadetector;

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShortArray;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VImage;
import org.epics.vtype.VImageDataType;
import org.epics.vtype.VImageType;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** A formula function for creating an VImage
 *  @author Kunal Shroff
 */
public class ImageOfFunction implements FormulaFunction
{
    @Override
    public String getCategory()
    {
        return "areaDetector";
    }

    @Override
    public String getName()
    {
        return "imageOf";
    }

    @Override
    public String getDescription()
    {
        return "Create an VImage from array data";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("imageData", "height", "width",
                "imageDataType (pvInt, pvUInt, pvDouble)", "vImageType (TYPE_MONO, TYPE_BAYER, TYPE_RGB1)",
                "xoffset", "yoffset", "xreversed", "yreversed");
    }

    @Override
    public VType compute(final VType... args) throws Exception
    {
        if (args.length != 9)
            throw new Exception("Function " + getName() +
                    " takes 9 arguments but received " + args.length);

        VType value = args[0];

        int height = Double.valueOf(VTypeHelper.toDouble(args[1])).intValue();
        int width = Double.valueOf(VTypeHelper.toDouble(args[2])).intValue();


        final VImageDataType imageDataType =
                VImageDataType.getVImageDataType(VTypeHelper.toString(args[3]));
        if(imageDataType == null) {
            throw new Exception("Failed to parse the image data type from the provided argument " + args[3]);
        }

        // Get data and data type
        VImageType vImageType = getVImageType(VTypeHelper.toString(args[4]));
        if(imageDataType == null) {
            throw new Exception("Failed to parse the image type from  " + args[4]);
        }
        final VNumberArray data;
        final Alarm alarm;
        final Time time;
        if (VTypeHelper.isNumericArray(value)) {
            data = (VNumberArray) value;
            alarm = data.getAlarm();
            time = data.getTime();
        } else {
            throw new Exception("Failed to parse the image data array.");
        }

        int xoffset = Double.valueOf(VTypeHelper.toDouble(args[5])).intValue();
        int yoffset = Double.valueOf(VTypeHelper.toDouble(args[6])).intValue();

        boolean xreversed = Boolean.parseBoolean(VTypeHelper.toString(args[7]));
        boolean yreversed = Boolean.parseBoolean(VTypeHelper.toString(args[8]));

        return VImage.of(height, width, xoffset, yoffset, xreversed, yreversed, data.getData(), imageDataType, vImageType, alarm, time);
    }


    /**
     * Get the VImageType for a string defining the type.
     *
     * @param imageType a character string defining the type
     * @return the VImageType or null if an illegal type
     */
    public static VImageType getVImageType(String imageType) {
        String type = imageType.toUpperCase();
        if(type.endsWith("CUSTOM")) return VImageType.TYPE_CUSTOM;
        if(type.endsWith("MONO")) return VImageType.TYPE_MONO;
        if(type.endsWith("BAYER")) return VImageType.TYPE_BAYER;
        if(type.endsWith("RGB1")) return VImageType.TYPE_RGB1;
        if(type.endsWith("RGB2")) return VImageType.TYPE_RGB2;
        if(type.endsWith("RGB3")) return VImageType.TYPE_RGB3;
        if(type.endsWith("YUV444")) return VImageType.TYPE_YUV444;
        if(type.endsWith("YUV422")) return VImageType.TYPE_YUV422;
        if(type.endsWith("YUV411")) return VImageType.TYPE_YUV411;
        return null;
    }
}
