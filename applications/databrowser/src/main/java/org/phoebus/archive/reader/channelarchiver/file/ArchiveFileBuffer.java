/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Implements ByteBuffer-like relative get methods for getting binary data
 *  from a file. Created for use with ArchiveFileReader and associated classes.
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArchiveFileBuffer implements AutoCloseable
{
    // XXX: what size? Bigger means less fetching, but too big means memory runs out;
    private final ByteBuffer buffer = ByteBuffer.allocate(65536);
    private FileChannel fileChannel;
    private File file = null;

    public ArchiveFileBuffer()
    {
    }

    public ArchiveFileBuffer(final File file) throws IOException
    {
        setFile(file);
    }

    /** Set file
     *  @param file
     *  @throws IOException
     */
    public void setFile(final File file) throws IOException
    {
        if (! file.equals(this.file))
        {
            this.file = file;
            close();
            fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        }
        buffer.position(0).limit(0);
    }

    public File getFile()
    {
        return file;
    }

    public void prepareGet(int numBytes) throws IOException
    {
        if (buffer.remaining() < numBytes)
        {
            buffer.compact();
            fileChannel.read(buffer);
            buffer.limit(buffer.position()); //use limit to mark extent of read
            buffer.position(0);
        }
    }

    public void get(byte dst []) throws IOException
    {
        prepareGet(dst.length);
        buffer.get(dst);
    }

    public long getUnsignedInt() throws IOException
    {
        prepareGet(4);
        return Integer.toUnsignedLong(buffer.getInt());
    }

    public short getShort() throws IOException
    {
        prepareGet(2);
        return buffer.getShort();
    }

    public float getFloat() throws IOException
    {
        prepareGet(4);
        return buffer.getFloat();
    }

    public double getDouble() throws IOException
    {
        prepareGet(8);
        return buffer.getDouble();
    }

    public byte get() throws IOException
    {
        if (!buffer.hasRemaining())
        {
            buffer.clear();
            buffer.limit(fileChannel.read(buffer));
        }
        return buffer.get();
    }

    //get epicsTime saved in file as java Instant; automatically
    //converts from Channel Archiver epoch (1990) to java epoch (1970)
    public Instant getEpicsTime() throws IOException
    {
        return Instant.ofEpochSecond(getUnsignedInt() + ArchiveFileTime.EPICS_OFFSET, getInt());
    }

    public void skip(int numBytes) throws IOException
    {
        int numAlready = buffer.remaining();
        while (numBytes > numAlready)
        {
            numBytes -= numAlready;
            buffer.clear();
            numAlready = fileChannel.read(buffer);
            buffer.limit(numAlready);
            buffer.position(0);
        }
        buffer.position(buffer.position() + numBytes);
    }

    public void offset(long offset) throws IOException
    {
         if (offset < 0 || offset > fileChannel.size())
        {
            //throw new RuntimeException("Offset is invalid.") ?
            return;
        }
        //check if buffer contains the data
        //(Buffer always represents a contiguous portion of the file's contents)
        long buffer_start_offset = fileChannel.position() - buffer.limit();
        boolean doesNotContain = buffer_start_offset > offset || fileChannel.position() < offset;
        if (doesNotContain)
        {
            fileChannel.position(offset);
            buffer.clear();
            buffer.limit(fileChannel.read(buffer));
            buffer.position(0);
        }
        else
        {
            buffer.position((int)(offset - buffer_start_offset));
        }
    }

    public int getInt() throws IOException
    {
        prepareGet(4);
        return buffer.getInt();
    }

    long offset() throws IOException
    {
        return fileChannel.position() - buffer.limit() + buffer.position();
    }

    public int remaining()
    {
        return buffer.remaining();
    }

    @Override
    public void close() throws IOException
    {
        if (fileChannel != null)
        {
            fileChannel.close();
            fileChannel = null;
        }
    }

    @Override
    public String toString()
    {
        long offset = -1;
        try
        {
            offset = offset();
        }
        catch (IOException e)
        {

        }
        return String.format("buffer@offset=%x(%d): %02x %02x %02x %02x %02x %02x %02x %02x", offset, offset, buffer.get(buffer.position()),
                buffer.get(buffer.position()+1), buffer.get(buffer.position()+2), buffer.get(buffer.position()+3), buffer.get(buffer.position()+4),
                buffer.get(buffer.position()+5), buffer.get(buffer.position()+6), buffer.get(buffer.position()+7));
    }
}