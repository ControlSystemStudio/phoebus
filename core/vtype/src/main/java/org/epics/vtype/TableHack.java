/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.epics.vtype;

import java.lang.reflect.Method;

/** Hack for reading the write-only EPICS 7.0.2 VTable
 *
 *  TODO Remove when VTable 7.0.2 again readable
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
@Deprecated
public class TableHack
{
    public static int getColumnCount(final VTable table)
    {
        try
        {
            final Method method = table.getClass().getMethod("getColumnCount");
            method.setAccessible(true);
            return (int) method.invoke(table);
        }
        catch (Throwable ex)
        {
            throw new Error("EPICS 7.0.2 doesn't want you to read the data", ex);
        }
    }

    public static String getColumnName(final VTable table, final int c)
    {
        try
        {
            final Method method = table.getClass().getMethod("getColumnName", Integer.TYPE);
            method.setAccessible(true);
            return (String) method.invoke(table, c);
        }
        catch (Throwable ex)
        {
            throw new Error("EPICS 7.0.2 doesn't want you to read the data", ex);
        }
    }

    public static int getRowCount(final VTable table)
    {
        try
        {
            final Method method = table.getClass().getMethod("getRowCount");
            method.setAccessible(true);
            return (int) method.invoke(table);
        }
        catch (Throwable ex)
        {
            throw new Error("EPICS 7.0.2 doesn't want you to read the data", ex);
        }
    }

    public static Object getColumnData(final VTable table, final int c)
    {
        try
        {
            final Method method = table.getClass().getMethod("getColumnData", Integer.TYPE);
            method.setAccessible(true);
            return method.invoke(table, c);
        }
        catch (Throwable ex)
        {
            throw new Error("EPICS 7.0.2 doesn't want you to read the data", ex);
        }
    }

    public static Class<?> getColumnType(VTable table, int c)
    {
        try
        {
            final Method method = table.getClass().getMethod("getColumnType", Integer.TYPE);
            method.setAccessible(true);
            return (Class<?>) method.invoke(table, c);
        }
        catch (Throwable ex)
        {
            throw new Error("EPICS 7.0.2 doesn't want you to read the data", ex);
        }
    }
}
