/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import static org.csstudio.archive.Engine.logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.logging.Level;

/** Enumeration Strings for a channel.
 *  <p>
 *  Presented as an array of strings for the enumerated values 0, 1, 2, ...
 *  The case where no enum strings are defined is represented by
 *  <code>null</code> EnumStrings.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EnumMetaDataHelper
{
    private EnumMetaDataHelper()
    {
        // prevent instantiation
    }

    /** Delete meta data for channel
     *  @param connection Connection
     *  @param sql SQL statements
     *  @param channel Channel
     *  @throws Exception on error
     */
    public static void delete(final Connection connection, final SQL sql,
            final RDBWriteChannel channel) throws Exception
    {
        // Delete any existing entries
        try
        (
            PreparedStatement del = connection.prepareStatement(sql.enum_delete_by_channel);
        )
        {
            del.setInt(1, channel.getId());
            del.executeUpdate();
        }
    }

    /** Insert meta data for channel into archive
     *  @param connection Connection
     *  @param sql SQL statements
     *  @param channel Channel
     *  @param states Enumeration labels
     *  @throws Exception on error
     */
    public static void insert(final Connection connection, final SQL sql, final RDBWriteChannel channel,
            final List<String> states) throws Exception
    {
        // Define the new ones
        try
        (
            PreparedStatement insert = connection.prepareStatement(sql.enum_insert_channel_num_val);
        )
        {
            for (int i=0; i<states.size(); ++i)
            {
                insert.setInt(1, channel.getId());
                insert.setInt(2, i);
                // Oracle doesn't allow empty==null state strings.
                String state = states.get(i);
                if (state == null  ||  state.length() < 1)
                {   // Patch as "<#>"
                    state = "<" + i + ">";
                    logger.log(Level.WARNING,
                        "Channel {0} has undefined state {1}",
                        new Object[] { channel.getName(), state });
                }
                insert.setString(3, state);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
