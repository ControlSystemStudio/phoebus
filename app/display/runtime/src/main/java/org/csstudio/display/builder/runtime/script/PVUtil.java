/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VEnum;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.util.time.TimestampFormats;

/** Utility for handling PVs and their values in scripts.
 *
 *  @author Kay Kasemir
 *  @author Xihui Chen - Original org.csstudio.opibuilder.scriptUtil.*
 */
@SuppressWarnings("nls")
public class PVUtil
{
    public static class PVHasNoValueException extends NullPointerException
    {
        public PVHasNoValueException(final String msg)
        {
            super(msg);
        }
    }


    /** Get valid value from PV
     *  @param pv PV
     *  @return Valid value
     *  @throws NullPointerException when PV is not connected, no valid value
     */
    public static VType getVType(final RuntimePV pv) throws NullPointerException
    {
        final VType value = pv.read();
        if (value == null)
            throw new PVHasNoValueException("PV " + pv.getName() + " has no value");
        if (PV.isDisconnected(value))
            throw new PVHasNoValueException("PV " + pv.getName() + " has no valid value");
        return value;
    }

    /** Try to get a 'double' type number from the PV.
     *  @param pv PV
     *  @return Current value.
     *          <code>Double.NaN</code> in case the value type
     *          does not decode into a number,
     *          or PV has no value.
     */
    public static double getDouble(final RuntimePV pv)
    {
        final VType value = pv.read();
        return ValueUtil.getDouble(value);
    }

    /** Try to get an integer from the PV.
     *  @param pv PV
     *  @return Current value as int.  Boolean will turn into 0, 1.
     *  @throws NullPointerException if the PV has no value
     */
    public static int getInt(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getInt(getVType(pv));
    }

    /** Try to get a long integer from the PV.
     *  @param pv PV
     *  @return Current value as long
     *  @throws NullPointerException if the PV has no value
     */
    public static long getLong(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getLong(getVType(pv));
    }

    /** Get value of PV as string.
     *  @param pv PV
     *  @return Current value as string. Boolean will turn into "false"/"true"
     *  @throws NullPointerException if the PV has no value
     */
    public static String getString(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getString(getVType(pv));
    }

    /** Get value of PV as string.
     *
     *  <p>Optionally, byte arrays can be requested as a (long) string,
     *  instead of "[ 1, 2, 3, .. ]"
     *  @param pv PV
     *  @param byte_array_as_string Decode byte arrays as string?
     *  @return Current value as string
     *  @throws NullPointerException if the PV has no value
     */
    public static String getString(final RuntimePV pv, final boolean byte_array_as_string) throws NullPointerException
    {
        return ValueUtil.getString(getVType(pv), byte_array_as_string);
    }

    /** Get labels for a {@link VEnum} value, or headers for a {@link VTable}.
     *  @param pv the PV.
     *  @return Enum labels or empty array if not enum nor table
     *  @throws NullPointerException if the PV has no value
     */
    public static String[] getLabels(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getLabels(getVType(pv));
    }

    /** Try to get a 'double' type array from a PV.
     *  @param pv the PV.
     *  @return Current value as double[].
     *          Will return single-element array for scalar value,
     *          including <code>{ Double.NaN }</code> in case the value type
     *          does not decode into a number.
     *  @throws NullPointerException if the PV has no value
     */
    public static double[] getDoubleArray(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getDoubleArray(getVType(pv));
    }

    /** Try to get a 'long' type array from a PV.
     *  @param pv the PV.
     *  @return Current value as long[].
     *          Will return single-element array for scalar value.
     *  @throws NullPointerException if the PV has no value
     */
    public static long[] getLongArray(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getLongArray(getVType(pv));
    }

    /** Get string array from pv.
     *  @param pv The PV.
     *  @return String array.
     *          For string array, it's the actual strings.
     *          For numeric arrays, the numbers are formatted as strings.
     *          For enum array, the labels are returned.
     *          For scalar PVs, an array with a single string is returned.
     *  @throws NullPointerException if the PV has no value
     */
    public final static String[] getStringArray(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getStringArray(getVType(pv));
    }

    /** Get the timestamp string of the PV.
     *
     *  <p>Time stamp of the PV is formatted using
     *  the common TimestampFormats helper for
     *  the local time zone.
     *
     *  @param pv PV
     *  @return Timestamp string
     *
     *  @see ValueUtil#getTimestamp(VType) to fetch time stamp for custom formatting
     *  @throws NullPointerException if the PV has no value
     */
    public static String getTimeString(final RuntimePV pv) throws NullPointerException
    {
        final Instant time = ValueUtil.getTimestamp(getVType(pv));
        if (time == null)
            return "";
        return TimestampFormats.FULL_FORMAT.format(time);
    }

    /** Get milliseconds since POSIX Epoch, i.e. 1 January 1970 0:00 UTC.
     *  <p>
     *  Note that we always return milliseconds relative to this UTC epoch,
     *  even if the original control system data source might use a different
     *  epoch (example: EPICS uses 1990), because the 1970 epoch is most
     *  compatible with existing programming environments.
     *  @param pv PV
     *  @return milliseconds since 1970.
     *  @throws NullPointerException if the PV has no value
     */
    public final static long getTimeInMilliseconds(final RuntimePV pv) throws NullPointerException
    {
        final Instant time = ValueUtil.getTimestamp(getVType(pv));
        if (time == null)
            return 0;
        return time.toEpochMilli();
    }

    /** Get alarm severity of the PV as an integer value.
     *  @param pv PV
     *  @return 0: OK;  1: Major; 2:Minor, -1: Invalid or Undefined
     */
    public final static int getSeverity(final RuntimePV pv)
    {
        final VType value = pv.read();
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            return alarm.getSeverity().ordinal();
        return -1;
    }

    /** Get alarm severity of the PV as an integer value.
     *
     *  <p>This uses the legacy mapping of alarm severities
     *
     *  @param pv PV
     *  @return 0: OK;  1: Major; 2:Minor, -1: Invalid or Undefined
     *  @deprecated Use getSeverity
     */
    @Deprecated
    public final static int getLegacySeverity(final RuntimePV pv)
    {
        final VType value = pv.read();
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            switch (alarm.getSeverity())
            {
            case NONE:
                return 0;
            case MAJOR:
                return 1;
            case MINOR:
                return 2;
            case UNDEFINED:
            case INVALID:
            default:
                break;
            }
        return -1;
    }

    /** Get alarm severity of the PV as a string
     *  @param pv PV
     *  @return Alarm severity as "NONE", "MINOR", "MAJOR", "INVALID", "UNDEFINED"
     */
    public final static String getSeverityString(final RuntimePV pv)
    {
        final VType value = pv.read();
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            return alarm.getSeverity().name();
        return AlarmSeverity.UNDEFINED.name();
    }

    /** Get alarm status, i.e. text that might describe the alarm severity
     *  @param pv PV
     *  @return Alarm status
     */
    public final static String getStatus(final RuntimePV pv)
    {
        final VType value = pv.read();
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            return alarm.getName();
        return "Disconnected";
    }

    /** Get a table from PV
     *
     *  <p>Ideally, the PV holds a {@link VTable},
     *  and the returned data is then the table's data.
     *
     *  <p>If the PV is a scalar, a table with a single cell is returned.
     *  <p>If the PV is an array, a table with one column is returned.
     *
     *  @param pv PV
     *  @return List of rows, where each row contains either String or Number cells
     *  @throws NullPointerException if the PV has no value
     */
    public static List<List<Object>> getTable(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getTable(getVType(pv));
    }

    /** Get data from a structured value by name.
     *
     *  <p>As with other structure-related get methods, full and partial names may be used.
     *  However, the name must designate a structured field, rather than a scalar data field.
     *  For example, the structure
     *  <pre>
     *  structure value
     *        structure Foo
     *            scalar_t[] a
     *           structure Bar
     *               scalar_t[] a
     *               scalar_t[] x
     *  </pre>
     *  can match "Foo" (which returns data for Foo/a, Bar/a, and x)
     *  or "Foo/Bar" (which returns data for Bar/a and x),
     *  but not "Foo/a" or "Bar/x".
     *  For those, use {@link #getStructureElement(RuntimePV, String, int)}.
     *  Ambiguous names will find the first structure with a matching name.
     *
     *  @param pv RuntimePV that should how a VTable
     *  @param name Name of the substructure to get;
     *              if blank (empty String, ""), the entire structure is returned
     *  @return A List of "rows", where rows are lists of scalar data (Strings or Numbers)
     *          belonging to scalar fields of the matching sub-structure;
     *          if there is no matching sub-structure, the list is empty.
     *  @throws NullPointerException if the PV has no value
     */
    public static List<List<Object>> getStructure(final RuntimePV pv, final String name) throws NullPointerException
    {
    	return ValueUtil.getStructure(getVType(pv), name);
    }

    /** Get a table cell from PV
     *
     *  <p>PV must hold a VTable
     *
     *  @param pv RuntimePV that should how a VTable
     *  @param row Row index, 0..
     *  @param column Column index, 0..
     *  @return Either String or Number for the cell's value, null if invalid row/column
     *  @throws NullPointerException if the PV has no value
     */
    public static Object getTableCell(final RuntimePV pv, final int row, final int column) throws NullPointerException
    {
        return ValueUtil.getTableCell(getVType(pv), row, column);
    }

    /** Get a structure element from a PV by field name.
     *
     *  <p>PV should hold a VTable which represents the structure.
     *
     *  <p> For nested structure elements represented with slash-separated names,
     *  full and partial field names are accepted. For instance, a structure "value"
     *  with the definition
     *  <pre>
     *  structure value
     *      structure Foo
     *          scalar_t[] a
     *          structure Bar
     *              scalar_t[] a
     *              scalar_t[] x
     *  </pre>
     *  has the field "x" with full name "Foo/Bar/x", which can be found with "Foo/Bar/x", "Bar/x", or "x".
     *  Ambiguous names (like "a" in the example above) will find the first field with a matching name.
     *
     *  @param pv RuntimePV that should how a VTable
     *  @param name Structure element name
     *  @return If the value has an elements with a matching name, a List&lt;String&gt; or List&lt;Number&gt;
     *          is returned, depending on the element's data type. If not, and the value is a VTable,
     *          an empty list is returned. Otherwise, a List containing one element, a String representation
     *          of the value.
     *  @throws NullPointerException if the PV has no value
     */
    public static List<Object> getStructureElement(final RuntimePV pv, final String name) throws NullPointerException
    {
    	return ValueUtil.getStructureElement(getVType(pv), name);
    }

    /** Get an element from a PV structure by field name and array index.
     *
     *  <p>If index is valid, this method is equivalent to getStructureElement(pv, name).get(index).
     *
     *  @param pv RuntimePV
     *  @param name Structure element name
     *  @param index Element index in range [0, n-1], where n is the length of the structure element
     *  @return Either String or Number for the cell's value, null if invalid name/index
     *  @throws NullPointerException if the PV has no value
     */
    public static Object getStructureElement(final RuntimePV pv, final String name, final int index) throws NullPointerException
    {
    	return ValueUtil.getStructureElement(getVType(pv), name, index);
    }

    /** Create a PV.
     *
     *  <p>Ideally, scripts use the PVs of widgets or the script 'input' PVs,
     *  where the display runtime handles the PV connection.
     *  In rare cases it might be necessary to create a PV in a script,
     *  in which case the script <b>must</b> then also release the PV.
     *
     *  @param pv_name Name of the PV
     *  @param timeout_ms Connection timeout in milliseconds
     *  @return PV
     *  @throws Exception on error
     *  @see #releasePV(RuntimePV)
     */
    public static RuntimePV createPV(final String pv_name, final int timeout_ms) throws Exception
    {
        final CountDownLatch connected = new CountDownLatch(1);
        final RuntimePVListener await_connection = new RuntimePVListener()
        {
            @Override
            public void valueChanged(final RuntimePV pv, final VType value)
            {
                if (value != null)
                    connected.countDown();
            }
        };

        final RuntimePV pv = PVFactory.getPV(pv_name);
        pv.addListener(await_connection);
        try
        {
            if (! connected.await(timeout_ms, TimeUnit.MILLISECONDS))
            {
                PVFactory.releasePV(pv);
                throw new Exception("Failed to connect to '" + pv_name + "' within " + timeout_ms + " ms");
            }
        }
        finally
        {
            pv.removeListener(await_connection);
        }
        return pv;
    }

    /** @param pv PV to release
     *  @see #createPV(String, int)
     */
    public static void releasePV(final RuntimePV pv)
    {
        PVFactory.releasePV(pv);
    }

    /** Write a value to a PV
     *
     *  <p>Ideally, scripts use the PVs of widgets or the script 'input' PVs,
     *  where the display runtime handles the PV connection.
     *  In rare cases it might be necessary to create a PV in a script,
     *  write a value, and then release the PV, which is done by this method.
     *
     *  @param pv_name Name of the PV
     *  @param value Value to write
     *  @param timeout_ms Connection timeout in milliseconds
     *  @throws Exception If write cannot be performed.
     */
    public final static void writePV(final String pv_name, final Object value, final int timeout_ms) throws Exception
    {
        final RuntimePV pv = createPV(pv_name, timeout_ms);
        try
        {
            pv.write(value);
        }
        finally
        {
            releasePV(pv);
        }
    }
}
