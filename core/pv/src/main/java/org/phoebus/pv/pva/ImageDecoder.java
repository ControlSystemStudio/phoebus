/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVAStructureArray;
import org.epics.pva.data.PVAUnion;
import org.epics.pva.data.PVAny;
import org.epics.pvdata.pv.PVInt;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VImage;
import org.epics.vtype.VImageDataType;
import org.epics.vtype.VImageType;
import org.epics.vtype.VType;

/** VImage from NT ND Array data
 *  @author Kay Kasemir
 *  @author Amanda Carpenter - Original code to handle unsigned VImageDataType, detect VImageType
 */
@SuppressWarnings("nls")
public class ImageDecoder
{
    // Could just use VImageType.values()[i+1] instead of color_mode_types[i], but
    // (a) there are more VImageType values than defined color modes, and
    // (b) the NTNDArray specification for color mode might eventually change
    private static final VImageType color_mode_types[] = { VImageType.TYPE_MONO, VImageType.TYPE_BAYER,
            VImageType.TYPE_RGB1, VImageType.TYPE_RGB2, VImageType.TYPE_RGB3, VImageType.TYPE_YUV444,
            VImageType.TYPE_YUV422, VImageType.TYPE_YUV411 };

    public static VType decode(final PVAStructure struct) throws Exception
    {
        // Get dimensions
        final PVAStructureArray dim = struct.get("dimension");
        if (dim == null)
            throw new Exception("Missing 'dimension'");

        // The 'dimension' field must be present, but may be empty,
        // for example in never-processed area detector image
        final int dimensions[];
        if (dim.get().length < 2)
            dimensions = new int[] { 0, 0 };
        else
        {   // Fetching by field name in case structure changes
            final int n_dims = dim.get().length;
            dimensions = new int[n_dims];
            for (int i = 0; i < n_dims; ++i)
            {
                final PVAInt size = dim.get()[i].get("size");
                dimensions[i] = size.get();
            }
        }

        final PVAUnion value_field = struct.get("value");
        final PVAData value = value_field.get();

        // Try to get value of color mode attribute
        // TODO: bayer color mode requires a bayerPattern attribute as well...
        final PVAStructureArray attribute_field = struct.get("attribute");
        int colorMode = -1;
        if (attribute_field != null)
            for (PVAStructure attribute : attribute_field.get())
            {
                final PVAString name_field = attribute.get("name");
                if (name_field != null && "colorMode".equalsIgnoreCase(name_field.get()))
                {
                    final PVAny color_mode_field = attribute.get("value");
                    final PVAData cm = color_mode_field.get();
                    if (cm instanceof PVInt)
                    {
                        colorMode = ((PVInt) cm).get();
                        break;
                    }
                    // else: log warning, or throw exception?
                }
            }
        if (colorMode == -1)
        { // Unsuccessful, so try to guess color mode from dimensions
            if (dimensions.length < 3)
                colorMode = 0; // mono
            else if (dimensions.length > 3)
                colorMode = -1; // custom
            else if (dimensions[0] == 3)
                colorMode = 2; // RGB1
            else if (dimensions[1] == 3)
                colorMode = 3; // RGB2
            else if (dimensions[2] == 3)
                colorMode = 4; // RGB3
        }
        else if (colorMode <= 4 && colorMode >= 2 && dimensions.length != 3)
        {
            throw new Exception("Color mode " + colorMode + " (" + color_mode_types[colorMode] +
                    ") requires 3 dimensions, got " + dimensions.length);
        }

        final VImageType image_type;
        if (colorMode >= color_mode_types.length || colorMode < 0)
            image_type = VImageType.TYPE_CUSTOM;
        else
            image_type = color_mode_types[colorMode];

        // Init. width, height
        final int width, height;
        switch (image_type)
        {
            case TYPE_RGB1:
                width = dimensions[1];
                height = dimensions[2];
                break;
            case TYPE_RGB2:
                width = dimensions[0];
                height = dimensions[2];
                break;
            case TYPE_RGB3:
                width = dimensions[0];
                height = dimensions[1];
                break;
            default:
                width = dimensions[0];
                height = dimensions[1];
        }

        // Get data and data type
        final ListNumber data;
        final VImageDataType data_type;
        if (value instanceof PVAByteArray)
        {
            final PVAByteArray values = (PVAByteArray) value;
            data = ArrayByte.of(values.get());
            if (values.isUnsigned())
                data_type = VImageDataType.pvUByte;
            else
                data_type = VImageDataType.pvByte;
        }
        else if (value instanceof PVAShortArray)
        {
            final PVAShortArray values = (PVAShortArray) value;
            data = ArrayShort.of(values.get());
            if (values.isUnsigned())
                data_type = VImageDataType.pvUShort;
            else
                data_type = VImageDataType.pvShort;
        }
        else if (value instanceof PVAIntArray)
        {
            final PVAIntArray values = (PVAIntArray) value;
            data = ArrayInteger.of(values.get());
            if (values.isUnsigned())
                data_type = VImageDataType.pvUInt;
            else
                data_type = VImageDataType.pvInt;
        }
        else if (value instanceof PVALongArray)
        {
            final PVALongArray values = (PVALongArray) value;
            data = ArrayLong.of(values.get());
            if (values.isUnsigned())
                data_type = VImageDataType.pvULong;
            else
                data_type = VImageDataType.pvLong;
        }
        else if (value instanceof PVAFloatArray)
        {
            final PVAFloatArray values = (PVAFloatArray) value;
            data = ArrayFloat.of(values.get());
            data_type = VImageDataType.pvFloat;
        }
        else if (value instanceof PVADoubleArray)
        {
            final PVADoubleArray values = (PVADoubleArray) value;
            data = ArrayDouble.of(values.get());
            data_type = VImageDataType.pvDouble;
        }
        else if (value == null)
        {
            data = ArrayByte.of();
            data_type = VImageDataType.pvUByte;
        }
        else
            throw new Exception("Cannot decode NTNDArray type of value " + value + ", sized " + width + " x " + height);

        final Alarm alarm = Decoders.decodeAlarm(struct);
        final Time time = Decoders.decodeTime(struct);
        return VImage.of(height, width, data, data_type, image_type, alarm, time);
    }
}
