/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Display;

/** Lazily reads CtrlInfo (Display- or Enum-related information) for Channel Archiver
 *  data files.
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class CtrlInfoReader
{
    private final long offset;
    private Display display; //display, for number/display types
    private List<String> labels; //labels, for enum types

    public CtrlInfoReader(long offset)
    {
        this.offset = offset;
        display = null;
        labels = null;
    }

    public void read(ArchiveFileBuffer buffer) throws IOException
    {
        long oldOffset = buffer.offset();
        buffer.offset(offset);
        short size = buffer.getShort();
        short type = buffer.getShort();
        switch(type)
        {
            case 0: //invalid
                break;
            case 1: //Numeric
                double upperDisplayLimit = buffer.getFloat();
                double lowerDisplayLimit = buffer.getFloat();
                double lowerWarningLimit = buffer.getFloat();
                double lowerAlarmLimit = buffer.getFloat();
                double upperWarningLimit = buffer.getFloat();
                double upperAlarmLimit = buffer.getFloat();
                int precision = buffer.getInt();
                size -= 32; //two shorts, 6 floats, and a 32-bit int
                byte unitsBytes [] = new byte [size];
                buffer.get(unitsBytes);
                String units = new String(unitsBytes).split("\0", 2)[0];
                display = Display.of(Range.of(lowerDisplayLimit, upperDisplayLimit),
                                     Range.of(lowerAlarmLimit, upperAlarmLimit),
                                     Range.of(lowerWarningLimit, upperWarningLimit),
                                     Range.undefined(),
                                     units, NumberFormats.precisionFormat(precision));
                break;
            case 2: //Enum
                short num_states = buffer.getShort();
                buffer.skip(2); //skip pad
                size -= 4;
                byte stateNamesBytes [] = new byte [size];
                buffer.get(stateNamesBytes);
                String stateNames [] = new String(stateNamesBytes).split("\0", num_states + 1);
                labels = Arrays.asList(stateNames).subList(0, num_states);
                break;
        }
        buffer.offset(oldOffset);
    }

    public Display getDisplay(ArchiveFileBuffer buffer) throws IOException
    {
        if (display == null)
            read(buffer);
        return display;
    }

    public List<String> getLabels(ArchiveFileBuffer buffer) throws IOException
    {
        if (labels == null)
            read(buffer);
        return labels;
    }

    public boolean isOffset(long offset)
    {
        return offset == this.offset;
    }
}
