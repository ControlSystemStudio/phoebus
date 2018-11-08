/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.writer.rdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;

import org.epics.vtype.Display;

/** Helper for handling the numeric meta data table.
 *  @author Kay Kasemir
 */
public class NumericMetaDataHelper
{
    private NumericMetaDataHelper()
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
            PreparedStatement del = connection.prepareStatement(
                        sql.numeric_meta_delete_by_channel);
        )
        {
            del.setInt(1, channel.getId());
            del.executeUpdate();
        }
    }

    /** Insert meta data for channel into archive
     *  @param connection Connection
     *  @param channel Channel
     *  @param meta Meta data
     *  @throws Exception on error
     */
    public static void insert(final Connection connection, final SQL sql,
            final RDBWriteChannel channel, final Display meta) throws Exception
    {
        try
        (
            PreparedStatement insert = connection.prepareStatement(sql.numeric_meta_insert);
        )
        {
            insert.setInt(1, channel.getId());
            setDoubleOrNull(insert, 2, meta.getDisplayRange().getMinimum());
            setDoubleOrNull(insert, 3, meta.getDisplayRange().getMaximum());
            setDoubleOrNull(insert, 4, meta.getWarningRange().getMinimum());
            setDoubleOrNull(insert, 5, meta.getWarningRange().getMaximum());
            setDoubleOrNull(insert, 6, meta.getAlarmRange().getMinimum());
            setDoubleOrNull(insert, 7, meta.getAlarmRange().getMaximum());
            final NumberFormat format = meta.getFormat();
            if (format == null)
                insert.setInt(8, 0);
            else
                insert.setInt(8, format.getMinimumFractionDigits());
            // Oracle schema has NOT NULL units...
            String units = meta.getUnit();
            if (units == null  ||  units.length() < 1)
                units = " "; //$NON-NLS-1$
            insert.setString(9, units);
            insert.executeUpdate();
        }
    }

    /** Some dialects like MySQL cannot handle NaN or +-Inf.
     *  Set those numbers as Null in the statement.
     *  @param statement
     *  @param index
     *  @param number
     *  @throws SQLException
     */
    private static void setDoubleOrNull(final PreparedStatement statement, final int index,
            final Double number) throws SQLException
    {
        if (number == null  ||  number.isInfinite()  ||  number.isNaN())
            statement.setNull(index, Types.DOUBLE);
        else
            statement.setDouble(index, number);
    }
}
