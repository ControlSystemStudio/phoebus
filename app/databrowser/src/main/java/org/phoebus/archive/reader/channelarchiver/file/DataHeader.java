/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.io.File;
import java.time.Instant;
import java.util.logging.Level;

import org.phoebus.archive.reader.channelarchiver.file.ArchiveFileSampleReader.DbrType;

/** Data Header
 *
 *  <p>Header for block of samples in data file.
 *
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
class DataHeader
{
    public final File file;
    public final long offset;
    public final File nextFile;
    public final long nextOffset;
    public final Instant nextTime; //start time for next file

    //public final Instant startTime;
    //public final Instant endTime;

    public final CtrlInfoReader info;
    public final DbrType dbrType; //dbr_time_xxx type of data
    public final short dbrCount; //count of data (i.e. number of dbr_xxx_t values per dbr_time_xxx sample)
    public final long numSamples; //number of dbr_time_xxx samples in the buffer which follows

    private DataHeader(final File file, final long offset,
            final File nextFile, final long nextOffset,
            final Instant nextTime, final CtrlInfoReader info,
            final DbrType dbrType, final short dbrCount, final long numSamples)
    {
        this.file = file;
        this.offset = offset;
        this.nextFile = nextFile;
        this.nextOffset = nextOffset;
        this.nextTime = nextTime;

        //this.startTime = startTime;
        //this.endTime = endTime;

        this.info = info;
        this.dbrType = dbrType;
        this.dbrCount = dbrCount;
        this.numSamples = numSamples;
    }

    @Override
    public String toString()
    {
        return String.format("Data Buffer '%s' @ 0x%X: next '%s' @ 0x%X, type %s[%d], %d samples",
                             file.getName(), offset,
                             nextFile.getName(), nextOffset,
                             dbrType.toString(), dbrCount, numSamples);
    }

    //assumes buffer is already opened and positioned
    public static DataHeader readDataHeader(ArchiveFileBuffer buffer, CtrlInfoReader info) throws Exception
    {
        final File file = buffer.getFile();
        final long offset = buffer.offset();

        // first part of data file header:
        //    4 bytes directory_offset (skipped)
        //    "        next_offset (offset of next entry in its file)
        //    "        prev_offset (offset of previous entry in its file)
        //    "        cur_offset (used by FileAllocator writing file)
        //    "        num_samples (number of samples in the buffer)
        //    "        ctrl_info_offset (offset in this file of control info header (units, limits, etc.))
        //    "        buff_size (bytes allocated for this entry, including header)
        //    "        buff_free (number of un-used, allocated bytes for this header)
        //  2 bytes DbrType (type of data stored in buffer)
        //    2 bytes    DbrCount (count of values for each buffer element, i.e. 1 for scalar types, >1 for array types)
        //    4 bytes padding (used to align the period)
        //    8 bytes (double) period
        //    8 bytes (epicsTimeStamp) begin_time
        //    "                        next_file_time
        //    "                        end_time
        //    char [40] prev_file
        //    char [40] next_file
        // --> Total of 152 bytes in data header
        buffer.skip(4);
        final long nextOffset = buffer.getUnsignedInt();
        buffer.skip(4);
        buffer.skip(4);
        final long numSamples = buffer.getUnsignedInt();
        final long ctrlInfoOffset = buffer.getUnsignedInt();
        final long buff_size = buffer.getUnsignedInt();
        final long buff_free = buffer.getUnsignedInt();
        final short dbrTypeCode = buffer.getShort();
        final short dbrCount = buffer.getShort();
        buffer.skip(4);
        buffer.skip(8);
        final Instant beginTime = buffer.getEpicsTime();
        final Instant nextTime = buffer.getEpicsTime();
        final Instant endTime = buffer.getEpicsTime();

        buffer.skip(40);
        final byte nameBytes [] = new byte [40];
        buffer.get(nameBytes);

        if (!info.isOffset(ctrlInfoOffset))
            info = new CtrlInfoReader(ctrlInfoOffset);
        final DbrType dbrType = DbrType.forValue(dbrTypeCode);

        // compute amount of data in this data file entry: (bytes allocated) - (bytes free) - (bytes in header)
        final long buffDataSize = buff_size - buff_free - 152;
        // System.out.println(buffDataSize);

        // Size of samples:
        // 12 bytes for status/severity/timestamp,
        // padding, value, padding
        final long dbr_size = 12 + dbrType.padding + dbrCount * dbrType.valueSize + dbrType.getValuePad(dbrCount);
        final long expected = dbr_size * numSamples;
        // System.out.println(dbr_size);
        // System.out.println(expected);

        if (expected != buffDataSize)
            throw new Exception("Expected " + expected + " byte buffer, got " + buffDataSize);

        String nextFilename = nextOffset != 0 ? new String(nameBytes).split("\0", 2)[0] : "*";
        File nextFile = new File(buffer.getFile().getParentFile(), nextFilename);

        logger.log(Level.FINE, "Datablock\n" +
                "Buffer  : '" + file + "' @ 0x" + Long.toHexString(offset) + "\n" +
                "Time    : " + beginTime + "\n" +
                "...     : " + endTime);

        return new DataHeader(file, offset, nextFile, nextOffset, nextTime, info, dbrType, dbrCount, numSamples);
    }
}