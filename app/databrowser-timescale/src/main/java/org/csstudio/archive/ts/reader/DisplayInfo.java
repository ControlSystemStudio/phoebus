/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * are made available under the terms of the Eclipse Public License v1.0
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts.reader;

import static org.csstudio.archive.ts.reader.TSArchiveReaderFactory.logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;

/** Helper for reading a channel's metadata
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayInfo
{
    /** Cache of channel ID and display info */
    private final static ConcurrentHashMap<Integer, DisplayInfo> cache = new ConcurrentHashMap<>();

    private final int channel_id;
    private Display display;
    private EnumDisplay labels;

    /** @param channel_id Channel ID
     *  @param reader Reader to use when information is not cached
     *  @return {@link DisplayInfo}, may be from cache. <code>null</code> on error.
     */
    public static DisplayInfo forChannel(final int channel_id, final TSArchiveReader reader)
    {
        return cache.computeIfAbsent(channel_id, id ->
        {
            try
            {
                return new DisplayInfo(id, reader);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot deterimine meta data for channel ID " + channel_id, ex);
                return null;
            }
        });
    }

    public DisplayInfo(final int channel_id, final TSArchiveReader reader) throws Exception
    {
        this.channel_id = channel_id;

        // Try numeric meta data
        display = determineDisplay(reader);
        // Try enumerated meta data
        final List<String> options = determineLabels(reader);
        labels = options == null ? null : EnumDisplay.of(options);

        // Assert that there is always at least numeric metadata
        if (display == null  &&  labels == null)
            display = Display.of(Range.of(0, 10), Range.undefined(), Range.undefined(), Range.undefined(), "", NumberFormats.precisionFormat(0));

        logger.log(Level.INFO, () -> "Read metadata for " + channel_id);
    }

    /** @return {@link Display} info, never <code>null</code> */
    public Display getDisplay()
    {
        return display;
    }

    /** @return {@link EnumDisplay} info, may be <code>null</code> */
    public EnumDisplay getLabels()
    {
        return labels;
    }

    /** @param connection
     *  @param sql
     *  @return Numeric meta data information for the channel or <code>null</code>
     *  @throws Exception on error
     */
    private Display determineDisplay(final TSArchiveReader reader) throws Exception
    {
        final Connection connection = reader.getPool().getConnection();
        try
        (
            final PreparedStatement statement =
                connection.prepareStatement(reader.getSQL().numeric_meta_sel_by_channel);
        )
        {
            statement.setInt(1, channel_id);
            reader.addForCancellation(statement);
            try (final ResultSet result = statement.executeQuery())
            {
                if (result.next())
                {
                    final NumberFormat format = NumberFormats.precisionFormat(result.getInt(7));   // prec
                    return Display.of(Range.of(result.getDouble(1), result.getDouble(2)),
                                      Range.of(result.getDouble(5), result.getDouble(6)),
                                      Range.of(result.getDouble(3), result.getDouble(4)),
                                      Range.of(result.getDouble(1), result.getDouble(2)),
                                      result.getString(8),
                                      format);
                }
            }
            finally
            {
                reader.removeFromCancellation(statement);
            }
        }
        finally
        {
            reader.getPool().releaseConnection(connection);
        }

        return null;
    }

    /** @param connection
     *  @param sql
     *  @return Numeric meta data information for the channel or <code>null</code>
     *  @throws Exception on error
     */
    private List<String> determineLabels(final TSArchiveReader reader) throws Exception
    {
        final Connection connection = reader.getPool().getConnection();
        try
        (
            final PreparedStatement statement = connection.prepareStatement(
                    reader.getSQL().enum_sel_num_val_by_channel);
        )
        {
            statement.setInt(1, channel_id);
            reader.addForCancellation(statement);
            try (final ResultSet result = statement.executeQuery())
            {
                if (result.next())
                {
                    final List<String> labels = new ArrayList<>();
                    do
                    {
                        final int id = result.getInt(1);
                        final String val = result.getString(2);
                        // Expect vals for ids 0, 1, 2, ...
                        if (id != labels.size())
                            throw new Exception("Enum IDs for channel with ID "
                                    + channel_id + " not in sequential order");
                        labels.add(val);
                    }
                    while (result.next());
                    // Anything found?
                    if (labels.isEmpty())
                        return null;
                    return labels;
                }
            }
            finally
            {
                reader.removeFromCancellation(statement);
            }
        }
        finally
        {
            reader.getPool().releaseConnection(connection);
        }
        return null;
    }
}
