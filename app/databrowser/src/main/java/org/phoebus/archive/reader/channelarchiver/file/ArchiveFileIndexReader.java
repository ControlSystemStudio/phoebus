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
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.channelarchiver.file.RTreeNode.RTreeNodeWithIndex;

/** Helper class for reading ChannelArchiver index files
 *
 *  (both master index files and sub-archive index files).
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArchiveFileIndexReader implements AutoCloseable
{
    private final ArchiveFileBuffer buffer;
    private final File indexParent;
    private final HashMap<String, TreeAnchor> anchors;

    /** Anchor of an RTree
     *
     */
    private class TreeAnchor
    {
        public final String name;
        public final long root;
        public final int numRecords;

        public TreeAnchor(final String name, final long offset) throws IOException
        {
            this.name = name;
            buffer.offset(offset);
            this.root = buffer.getUnsignedInt();
            this.numRecords = (int) buffer.getUnsignedInt();
        }

        @Override
        public String toString()
        {
            return "RTree(" + name + "): " + numRecords + " records @ 0x" + Long.toHexString(root);
        }
    }

    public ArchiveFileIndexReader(final File indexFile) throws IOException
    {
        buffer = new ArchiveFileBuffer(indexFile);
        indexParent = indexFile.getParentFile();
        anchors = getAnchors();
        logger.fine(() -> "Opened " + indexFile + ", " + anchors.size() + " channels");
    }

    private Queue<Long> readHashTable() throws IOException
    {
        buffer.offset(4);
        long start = buffer.getUnsignedInt();
        long size = buffer.getUnsignedInt();

        Queue<Long> ret = new ArrayDeque<Long>();
        buffer.offset(start);
        while (--size > 0)
        {
            long res = buffer.getUnsignedInt();
            if (res != 0)
                ret.add(res);
        }
        return ret;
    }

    private HashMap<String, TreeAnchor> getAnchors() throws IOException
    {
        //Hash table entries are stored as follows:
        // long next - offset of next hash entry on the table
        // long ID - offset of RTree anchor for channel name
        // short name_len - length of channel name
        // short id_text_len - length of channel id text
        // char name [name_name] - channel name (without '/0' terminator)
        // char id_text [id_text_len] - id text (without '/0' terminator)
        HashMap<String, TreeAnchor> ret = new HashMap<>();
        Queue<Long> offsets = readHashTable();
        while (!offsets.isEmpty())
        {
            long offset = offsets.poll();
            buffer.offset(offset);

            // read next, "id" (offset to RTree), and name_len
            offset = buffer.getUnsignedInt();
            long anchor_offset = buffer.getUnsignedInt();
            short nameLen = buffer.getShort();
            buffer.skip(2);

            byte name_buf [] = new byte [nameLen];
            buffer.get(name_buf);
            final String name = new String(name_buf);
            ret.put(name, new TreeAnchor(name, anchor_offset));
            if (offset != 0)
                offsets.add(offset);
        }
        return ret;
    }

    /** Get all data file entries (filename + offset)
     *  for the given channel name and the given start and end times.
     *
     *  <p>If the index file has data for the given channel name, but no data for any time
     *  at or before the given end time, an empty list will be returned.
     *
     *  @param channelName Channel name
     *  @param startTime
     *  @param endTime
     *  @return data file entries (file + offset) for the given time range
     *  @throws UnknownChannelException If the index has no data for the given channel name.
     *  @throws Exception on error
     */
    public List<DataFileEntry> getEntries(final String channelName, final Instant startTime, final Instant endTime) throws Exception, UnknownChannelException
    {
        final TreeAnchor anchor = anchors.get(channelName);
        if (anchor == null)
            throw new UnknownChannelException(channelName);

        final RTreeNodeWithIndex node_and_index = searchRTreeNodes(anchor.root, anchor.numRecords, startTime);
        if (node_and_index == null)
            return Collections.emptyList();

        final List<DataFileEntry> entries = new ArrayList<>();
        RTreeNode node = node_and_index.node;
        int index = node_and_index.record_index;

        while (! node.records[index].start.isAfter(endTime))
        {
            if (! node.records[index].isEmpty())
            {
                // A record might point to a chain of data blocks
                final List<DataFileEntry>  sub_entries = readDatablocks(node.records[index].child);
                // Use only the first data block, not the 'shadowed' data blocks below
                entries.add(sub_entries.get(0));
            }
            // Get next record from this node
            if (index < node.getM() - 1)
                ++index;
            else
            {
                // Go up to the parent node, maybe several levels
                while (true)
                {
                    if (node.parent == 0)
                        return entries;
                    final RTreeNode parent = new RTreeNode(buffer, node.parent, node.getM());
                    index = parent.findRecordForChild(node.offset) + 1;
                    if (index < node.getM()  &&   ! parent.records[index].isEmpty())
                    {   // From the _next_ parent record, descent into first child
                        node = new RTreeNode(buffer, parent.records[index].child, node.getM());
                        index = 0;
                        break;
                    }
                    // else: Need to go up to parent's parent, and find the next record there
                    node = parent;
                }
                // Keep descending via leftmost child to leaf
                while (! node.isLeaf)
                    node = new RTreeNode(buffer, node.records[index].child, node.getM());
            }
        }

        return entries;
    }

    /**
     * Finds the leaf-node record whose start time is the largest start time
     * at or below the given Instant. Returns the offset of that record's child,
     * if the offset is non-zero; or else the first non-zero child offset of the
     * records that follow, if there is one; or zero if no non-zero child offsets
     * can be found.
     * @param root Offset in index file of RTree's root node
     * @param numRecords Number of records per RTree node
     * @param time Start time to search for
     * @return Offset of datablock which is at or before, or 0 if there is no data block
     * @throws Exception on error
     */
    public RTreeNodeWithIndex searchRTreeNodes(final long root, final int numRecords, final Instant time) throws Exception
    {
        RTreeNode node = new RTreeNode(buffer, root, numRecords);
        while (true)
        {
            // System.out.println(node);
            int i;
            long child = 0;
            for (i=numRecords-1;  i>=0;  --i)
            {
                child = node.records[i].child;
                if (child != 0  &&
                    node.records[i].start.compareTo(time) <= 0)
                    break;
            }
            if (child == 0)
                return null;
            // If nothing found before the start time, use first record
            if (i < 0)
                i = 0;
            if (node.isLeaf)
                return new RTreeNodeWithIndex(node, i);
            node = new RTreeNode(buffer, child, numRecords);
        }
    }

    /**
     * Given the offset of an RTree datablock, gets the filename and offset
     * associated with that datablock, and the same for any child datablocks.
     * The returned values correspond to the same time frame, but should be in
     * different sub-archives, if there are more than one.
     * @param offset Offset of RTree datablock (child_id of leaf node's record)
     * @return List<DataFileEntry> corresponding to the respective files and offsets of
     *             all datablocks associated with the RTree record which contains the given offset
     * @throws IOException
     */
    private List<DataFileEntry> readDatablocks(long offset) throws IOException
    {
        //Datablocks (RTree entries) are stored as follows:
        // long next_ID - offset of next datablock
        // long data_offset - offset of data block in data file
        // short name_size - size of name
        // char name [name_size] - file name for data file (w/o '\0')
        final List<DataFileEntry> ret = new ArrayList<>();
        while (offset != 0)
        {
            buffer.offset(offset);

            offset = buffer.getUnsignedInt();
            final long dataOffset = buffer.getUnsignedInt();
            final int nameSize = buffer.getShort();
            final byte name [] = new byte [nameSize];
            buffer.get(name);
            ret.add(new DataFileEntry(new File(indexParent, new String(name)), dataOffset));
        }
        return ret;
    }

    @Override
    public void close() throws IOException
    {
        buffer.close();
    }

    public java.util.Set<String> getChannelNames()
    {
        return anchors.keySet();
    }
}
