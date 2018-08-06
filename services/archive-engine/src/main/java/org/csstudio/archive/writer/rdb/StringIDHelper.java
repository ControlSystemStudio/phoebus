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
import java.sql.ResultSet;
import java.sql.Statement;

/** Helper for {@link StringID} entries in RDB.
 *
 *  @author Kay Kasemir
 *  @author Laurent Philippe Switch connection to readonly for MySQL load balancing
 *  @author Lana Abadie - Readonly change not possible for PostgreSQL. Disable autocommit as needed.
 */
@SuppressWarnings("nls")
public class StringIDHelper
{
    private final Connection connection;
    private final String table;
    private final String id_column;
    private final String name_column;

    /** Construct helper
     *  @param connection {@link Connection}
     *  @param table Name of RDB table
     *  @param id_column Name of the ID column
     *  @param name_column Name of the Name column
     */
    public StringIDHelper(final Connection connection,
            final String table, final String id_column,
            final String name_column)
    {
        this.connection = connection;
        this.table = table;
        this.id_column = id_column;
        this.name_column = name_column;
    }

    /** Locate StringID by name
     *  @param name Name to locate
     *  @return StringID or <code>null</code> if nothing found
     *  @throws Exception on error
     */
    public StringID find(final String name) throws Exception
    {
        try
        (
            PreparedStatement sel_by_name = connection.prepareStatement(
                        "SELECT " + id_column + " FROM " + table +
                        " WHERE "+ name_column + "=?");
        )
        {
            sel_by_name.setString(1, name);
            final ResultSet result = sel_by_name.executeQuery();
            if (result.next())
            {
                final int id = result.getInt(1);
                result.close();
                return new StringID(id, name);
            }
            else
                result.close();
        }
        return null;
    }

    /** Locate StringID by ID
     *  @param id ID to locate
     *  @return StringID or <code>null</code> if nothing found
     *  @throws Exception on error
     */
    public StringID find(final int id) throws Exception
    {
        try
        (
            PreparedStatement sel_by_id = connection.prepareStatement(
                    "SELECT " + name_column + " FROM " + table +
                    " WHERE "+ id_column + "=?")
        )
        {
            sel_by_id.setInt(1, id);
            final ResultSet result = sel_by_id.executeQuery();
            if (result.next())
            {
                final String text = result.getString(1);
                result.close();
                return new StringID(id, text);
            }
            result.close();
        }
        return null;
    }

    /** Add new name, unless it's already in the RDB.
     *  @param name Name to add
     *  @return StringID found or generated
     *  @throws Exception on error
     */
    public StringID add(final String name) throws Exception
    {
        StringID entry = find(name);
        if (entry != null)
            return entry;
        entry = new StringID(getNextID(), name);
        final boolean autocommitState = connection.getAutoCommit();
        if (autocommitState == true)
            connection.setAutoCommit(false);
        try
        (
            PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + table +
                "(" + id_column + "," + name_column + ") VALUES (?,?)");
        )
        {
            insert.setInt(1, entry.getId());
            insert.setString(2, entry.getName());
            final int rows = insert.executeUpdate();
            if (rows != 1)
                throw new Exception("Insert of " + entry + " changed " +
                        rows + " instead of 1 rows");
            connection.commit();
            return entry;
        }
        catch(Exception ex)
        {
            connection.rollback();
            throw ex;
        }
        finally
        {
            if (autocommitState == true)
                connection.setAutoCommit(true);
        }
    }

    private int getNextID() throws Exception
    {
        try
        (
            final Statement statement = connection.createStatement();
        )
        {
            final ResultSet res = statement.executeQuery(
                    "SELECT MAX(" + id_column + ") FROM " + table);
            if (res.next())
            {
                final int id = res.getInt(1);
                res.close();
                if (id > 0)
                    return id + 1;
            }
            return 1;
        }
    }
}
