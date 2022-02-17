/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore;

import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListShort;
import org.epics.util.array.ListUByte;
import org.epics.util.array.ListUInteger;
import org.epics.util.array.ListULong;
import org.epics.util.array.ListUShort;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VUByte;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUInt;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULong;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShort;
import org.epics.vtype.VUShortArray;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * UnitTest for {@link SafeMultiply} APIs
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SafeMultiplyTest {
    private Alarm alarm_1;
    private Time time_1;
    private Display display_1;

    private Alarm alarm_2;
    private Time time_2;
    private Display display_2;

    @Before
    public void init() {
        alarm_1 = Alarm.none();
        time_1 = Time.now();
        display_1 = Display.none();

        alarm_2 = Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.DEVICE, "test");
        time_2 = Time.of(Instant.MAX);
        display_2 = Display.none();
    }

    @Test
    public void TestMultiplyDouble() {
        assertEquals(0d, SafeMultiply.multiply(3d, 0d));
        assertEquals(18d, SafeMultiply.multiply(3d, 6d));
        assertEquals(-18d, SafeMultiply.multiply(-3d, 6d));
        assertEquals(18d, SafeMultiply.multiply(-3d, -6d));
        assertEquals(Double.MAX_VALUE, SafeMultiply.multiply(Double.MAX_VALUE, 6d));
        assertEquals(-Double.MAX_VALUE, SafeMultiply.multiply(-Double.MAX_VALUE, 6d));
    }

    @Test
    public void TestMultiplyFloat() {
        assertEquals(0f, SafeMultiply.multiply(3f, 0f));
        assertEquals(18f, SafeMultiply.multiply(3f, 6f));
        assertEquals(-18f, SafeMultiply.multiply(-3f, 6f));
        assertEquals(18f, SafeMultiply.multiply(-3f, -6f));
        assertEquals(Float.MAX_VALUE, SafeMultiply.multiply(Float.MAX_VALUE, 6f));
        assertEquals(-Float.MAX_VALUE, SafeMultiply.multiply(-Float.MAX_VALUE, 6f));
    }

    @Test
    public void TestMultiplyULong() {
        final BigInteger ULONG_MAX = BigInteger.valueOf(9223372036854775807L).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(1));

        assertEquals(ULong.valueOf(0), SafeMultiply.multiply(ULong.valueOf(3), ULong.valueOf(0)));
        assertEquals(ULong.valueOf(18), SafeMultiply.multiply(ULong.valueOf(3), ULong.valueOf(6)));
        assertEquals(ULong.valueOf(ULONG_MAX.longValue()), SafeMultiply.multiply(ULong.valueOf(Long.MAX_VALUE), ULong.valueOf(6)));
    }

    @Test
    public void TestMultiplyLong() {
        assertEquals(0L, SafeMultiply.multiply(3L, 0L));
        assertEquals(18L, SafeMultiply.multiply(3L, 6L));
        assertEquals(-18L, SafeMultiply.multiply(-3L, 6L));
        assertEquals(18L, SafeMultiply.multiply(-3L, -6L));
        assertEquals(Long.MAX_VALUE, SafeMultiply.multiply(Long.MAX_VALUE, 6L));
        assertEquals(Long.MIN_VALUE, SafeMultiply.multiply(Long.MIN_VALUE, 6L));
    }

    @Test
    public void TestMultiplyUInteger() {
        final long UINT_MAX = 4294967295L;

        assertEquals(UInteger.valueOf(0), SafeMultiply.multiply(UInteger.valueOf(3), UInteger.valueOf(0)));
        assertEquals(UInteger.valueOf(18), SafeMultiply.multiply(UInteger.valueOf(3), UInteger.valueOf(6)));
        assertEquals(UInteger.valueOf((int) UINT_MAX), SafeMultiply.multiply(UInteger.valueOf(Integer.MAX_VALUE), UInteger.valueOf(6)));
    }

    @Test
    public void TestMultiplyInteger() {
        assertEquals(0, SafeMultiply.multiply(3, 0));
        assertEquals(18, SafeMultiply.multiply(3, 6));
        assertEquals(-18, SafeMultiply.multiply(-3, 6));
        assertEquals(18, SafeMultiply.multiply(-3, -6));
        assertEquals(Integer.MAX_VALUE, SafeMultiply.multiply(Integer.MAX_VALUE, 6));
        assertEquals(Integer.MIN_VALUE, SafeMultiply.multiply(Integer.MIN_VALUE, 6));
    }

    @Test
    public void TestMultiplyUShort() {
        final int USHORT_MAX = 65535;

        assertEquals(UShort.valueOf(Short.parseShort("0")), SafeMultiply.multiply(UShort.valueOf(Short.parseShort("3")), UShort.valueOf(Short.parseShort("0"))));
        assertEquals(UShort.valueOf(Short.parseShort("18")), SafeMultiply.multiply(UShort.valueOf(Short.parseShort("3")), UShort.valueOf(Short.parseShort("6"))));
        assertEquals(UShort.valueOf((short) USHORT_MAX), SafeMultiply.multiply(UShort.valueOf(Short.MAX_VALUE), UShort.valueOf(Short.parseShort("6"))));
    }

    @Test
    public void TestMultiplyShort() {
        assertEquals(Short.valueOf("0"), SafeMultiply.multiply(Short.valueOf("3"), Short.valueOf("0")));
        assertEquals(Short.valueOf("18"), SafeMultiply.multiply(Short.valueOf("3"), Short.valueOf("6")));
        assertEquals(Short.valueOf("-18"), SafeMultiply.multiply(Short.valueOf("-3"), Short.valueOf("6")));
        assertEquals(Short.valueOf("18"), SafeMultiply.multiply(Short.valueOf("-3"), Short.valueOf("-6")));
        assertEquals(Short.MAX_VALUE, SafeMultiply.multiply(Short.MAX_VALUE, Short.valueOf("6")));
        assertEquals(Short.MIN_VALUE, SafeMultiply.multiply(Short.MIN_VALUE, Short.valueOf("6")));
    }

    @Test
    public void TestMultiplyUByte() {
        final int UBYTE_MAX = 255;

        assertEquals(UByte.valueOf(Byte.parseByte("0")), SafeMultiply.multiply(UByte.valueOf(Byte.parseByte("3")), UByte.valueOf(Byte.parseByte("0"))));
        assertEquals(UByte.valueOf(Byte.parseByte("18")), SafeMultiply.multiply(UByte.valueOf(Byte.parseByte("3")), UByte.valueOf(Byte.parseByte("6"))));
        assertEquals(UByte.valueOf((byte) UBYTE_MAX), SafeMultiply.multiply(UByte.valueOf(Byte.MAX_VALUE), UByte.valueOf(Byte.parseByte("6"))));
    }

    @Test
    public void TestMultiplyByte() {
        assertEquals(Byte.valueOf("0"), SafeMultiply.multiply(Byte.valueOf("3"), Byte.valueOf("0")));
        assertEquals(Byte.valueOf("18"), SafeMultiply.multiply(Byte.valueOf("3"), Byte.valueOf("6")));
        assertEquals(Byte.valueOf("-18"), SafeMultiply.multiply(Byte.valueOf("-3"), Byte.valueOf("6")));
        assertEquals(Byte.valueOf("18"), SafeMultiply.multiply(Byte.valueOf("-3"), Byte.valueOf("-6")));
        assertEquals(Byte.MAX_VALUE, SafeMultiply.multiply(Byte.MAX_VALUE, Byte.valueOf("6")));
        assertEquals(Byte.MIN_VALUE, SafeMultiply.multiply(Byte.MIN_VALUE, Byte.valueOf("6")));
    }

    @Test
    public void TestMultiplyWithDouble() {
        assertEquals(18d, SafeMultiply.multiply(6d, 3d));
        assertEquals(-18d, SafeMultiply.multiply(6d, -3d));
        assertEquals(-18d, SafeMultiply.multiply(-6d, 3d));

        assertEquals(18f, SafeMultiply.multiply(6f, 3d));
        assertEquals(-18f, SafeMultiply.multiply(6f, -3d));
        assertEquals(-18f, SafeMultiply.multiply(-6f, 3d));

        assertEquals(ULong.valueOf(0), SafeMultiply.multiply(ULong.valueOf(0), 3d));
        assertEquals(ULong.valueOf(18), SafeMultiply.multiply(ULong.valueOf(6), 3d));

        assertEquals(18L, SafeMultiply.multiply(6L, 3d));
        assertEquals(-18L, SafeMultiply.multiply(6L, -3d));
        assertEquals(-18L, SafeMultiply.multiply(-6L, 3d));

        assertEquals(UInteger.valueOf(0), SafeMultiply.multiply(UInteger.valueOf(0), 3d));
        assertEquals(UInteger.valueOf(18), SafeMultiply.multiply(UInteger.valueOf(6), 3d));

        assertEquals(18, SafeMultiply.multiply(6, 3d));
        assertEquals(-18, SafeMultiply.multiply(6, -3d));
        assertEquals(-18, SafeMultiply.multiply(-6, 3d));

        assertEquals(UShort.valueOf(Short.parseShort("0")), SafeMultiply.multiply(UShort.valueOf(Short.parseShort("0")), 3d));
        assertEquals(UShort.valueOf(Short.parseShort("18")), SafeMultiply.multiply(UShort.valueOf(Short.parseShort("6")), 3d));

        assertEquals(Short.parseShort("18"), SafeMultiply.multiply(Short.parseShort("6"), 3d));
        assertEquals(Short.parseShort("-18"), SafeMultiply.multiply(Short.parseShort("6"), -3d));
        assertEquals(Short.parseShort("-18"), SafeMultiply.multiply(Short.parseShort("-6"), 3d));

        assertEquals(UByte.valueOf(Byte.parseByte("0")), SafeMultiply.multiply(UByte.valueOf(Byte.parseByte("0")), 3d));
        assertEquals(UByte.valueOf(Byte.parseByte("18")), SafeMultiply.multiply(UByte.valueOf(Byte.parseByte("6")), 3d));

        assertEquals(Byte.parseByte("18"), SafeMultiply.multiply(Byte.parseByte("6"), 3d));
        assertEquals(Byte.parseByte("-18"), SafeMultiply.multiply(Byte.parseByte("6"), -3d));
        assertEquals(Byte.parseByte("-18"), SafeMultiply.multiply(Byte.parseByte("-6"), 3d));
    }

    @Test
    public void TestMultiplyVDouble() {
        VDouble goodResult_1 = VDouble.of(Double.valueOf(0), alarm_1, time_1, display_1);
        VDouble goodResult_2 = VDouble.of(Double.valueOf(18), alarm_1, time_1, display_1);
        VDouble goodResult_3 = VDouble.of(Double.valueOf(-18), alarm_1, time_1, display_1);

        VDouble number_1 = VDouble.of(Double.valueOf(3), alarm_1, time_1, display_1);
        VDouble number_2 = VDouble.of(Double.valueOf(-3), alarm_1, time_1, display_1);

        VDouble multiplier_1 = VDouble.of(Double.valueOf(0), alarm_2, time_2, display_2);
        VDouble multiplier_2 = VDouble.of(Double.valueOf(6), alarm_2, time_2, display_2);
        VDouble multiplier_3 = VDouble.of(Double.valueOf(-6), alarm_2, time_2, display_2);

        VDouble posMax = VDouble.of(Double.MAX_VALUE, alarm_1, time_1, display_1);
        VDouble negMax = VDouble.of(-Double.MAX_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestMultiplyVFloat() {
        VFloat goodResult_1 = VFloat.of(Float.valueOf(0), alarm_1, time_1, display_1);
        VFloat goodResult_2 = VFloat.of(Float.valueOf(18), alarm_1, time_1, display_1);
        VFloat goodResult_3 = VFloat.of(Float.valueOf(-18), alarm_1, time_1, display_1);

        VFloat number_1 = VFloat.of(Float.valueOf(3), alarm_1, time_1, display_1);
        VFloat number_2 = VFloat.of(Float.valueOf(-3), alarm_1, time_1, display_1);

        VFloat multiplier_1 = VFloat.of(Float.valueOf(0), alarm_2, time_2, display_2);
        VFloat multiplier_2 = VFloat.of(Float.valueOf(6), alarm_2, time_2, display_2);
        VFloat multiplier_3 = VFloat.of(Float.valueOf(-6), alarm_2, time_2, display_2);

        VFloat posMax = VFloat.of(Float.MAX_VALUE, alarm_1, time_1, display_1);
        VFloat negMax = VFloat.of(-Float.MAX_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestMultiplyVULong() {
        final BigInteger ULONG_MAX = BigInteger.valueOf(9223372036854775807L).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(1));

        VULong goodResult_1 = VULong.of(ULong.valueOf(0), alarm_1, time_1, display_1);
        VULong goodResult_2 = VULong.of(ULong.valueOf(18), alarm_1, time_1, display_1);

        VULong number = VULong.of(ULong.valueOf(3), alarm_1, time_1, display_1);

        VULong multiplier_1 = VULong.of(ULong.valueOf(0), alarm_2, time_2, display_2);
        VULong multiplier_2 = VULong.of(ULong.valueOf(6), alarm_2, time_2, display_2);

        VULong maxValue = VULong.of(ULONG_MAX, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(maxValue, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, true));
        result = SafeMultiply.multiply(multiplier_2, maxValue);
        assertFalse(Utilities.areVTypesIdentical(maxValue, result, true));
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, false));
    }

    @Test
    public void TestMultiplyVLong() {
        VLong goodResult_1 = VLong.of(Long.valueOf(0), alarm_1, time_1, display_1);
        VLong goodResult_2 = VLong.of(Long.valueOf(18), alarm_1, time_1, display_1);
        VLong goodResult_3 = VLong.of(Long.valueOf(-18), alarm_1, time_1, display_1);

        VLong number_1 = VLong.of(Long.valueOf(3), alarm_1, time_1, display_1);
        VLong number_2 = VLong.of(Long.valueOf(-3), alarm_1, time_1, display_1);

        VLong multiplier_1 = VLong.of(Long.valueOf(0), alarm_2, time_2, display_2);
        VLong multiplier_2 = VLong.of(Long.valueOf(6), alarm_2, time_2, display_2);
        VLong multiplier_3 = VLong.of(Long.valueOf(-6), alarm_2, time_2, display_2);

        VLong posMax = VLong.of(Long.MAX_VALUE, alarm_1, time_1, display_1);
        VLong negMax = VLong.of(Long.MIN_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestMultiplyVUInt() {
        final long UINT_MAX = 4294967295L;

        VUInt goodResult_1 = VUInt.of(UInteger.valueOf(0), alarm_1, time_1, display_1);
        VUInt goodResult_2 = VUInt.of(UInteger.valueOf(18), alarm_1, time_1, display_1);

        VUInt number = VUInt.of(UInteger.valueOf(3), alarm_1, time_1, display_1);

        VUInt multiplier_1 = VUInt.of(UInteger.valueOf(0), alarm_2, time_2, display_2);
        VUInt multiplier_2 = VUInt.of(UInteger.valueOf(6), alarm_2, time_2, display_2);

        VUInt maxValue = VUInt.of(UINT_MAX, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(maxValue, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, true));
        result = SafeMultiply.multiply(multiplier_2, maxValue);
        assertFalse(Utilities.areVTypesIdentical(maxValue, result, true));
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, false));
    }

    @Test
    public void TestMultiplyVInt() {
        VInt goodResult_1 = VInt.of(0, alarm_1, time_1, display_1);
        VInt goodResult_2 = VInt.of(18, alarm_1, time_1, display_1);
        VInt goodResult_3 = VInt.of(-18, alarm_1, time_1, display_1);

        VInt number_1 = VInt.of(3, alarm_1, time_1, display_1);
        VInt number_2 = VInt.of(-3, alarm_1, time_1, display_1);

        VInt multiplier_1 = VInt.of(0, alarm_2, time_2, display_2);
        VInt multiplier_2 = VInt.of(6, alarm_2, time_2, display_2);
        VInt multiplier_3 = VInt.of(-6, alarm_2, time_2, display_2);

        VInt posMax = VInt.of(Integer.MAX_VALUE, alarm_1, time_1, display_1);
        VInt negMax = VInt.of(Integer.MIN_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestMultiplyVUShort() {
        final int USHORT_MAX = 65535;

        VUShort goodResult_1 = VUShort.of(UShort.valueOf(Short.parseShort("0")), alarm_1, time_1, display_1);
        VUShort goodResult_2 = VUShort.of(UShort.valueOf(Short.parseShort("18")), alarm_1, time_1, display_1);

        VUShort number = VUShort.of(UShort.valueOf(Short.parseShort("3")), alarm_1, time_1, display_1);

        VUShort multiplier_1 = VUShort.of(UShort.valueOf(Short.parseShort("0")), alarm_2, time_2, display_2);
        VUShort multiplier_2 = VUShort.of(UShort.valueOf(Short.parseShort("6")), alarm_2, time_2, display_2);

        VUShort maxValue = VUShort.of(USHORT_MAX, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(maxValue, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, true));
        result = SafeMultiply.multiply(multiplier_2, maxValue);
        assertFalse(Utilities.areVTypesIdentical(maxValue, result, true));
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, false));
    }

    @Test
    public void TestMultiplyVShort() {
        VShort goodResult_1 = VShort.of(Short.valueOf("0"), alarm_1, time_1, display_1);
        VShort goodResult_2 = VShort.of(Short.valueOf("18"), alarm_1, time_1, display_1);
        VShort goodResult_3 = VShort.of(Short.valueOf("-18"), alarm_1, time_1, display_1);

        VShort number_1 = VShort.of(Short.valueOf("3"), alarm_1, time_1, display_1);
        VShort number_2 = VShort.of(Short.valueOf("-3"), alarm_1, time_1, display_1);

        VShort multiplier_1 = VShort.of(Short.valueOf("0"), alarm_2, time_2, display_2);
        VShort multiplier_2 = VShort.of(Short.valueOf("6"), alarm_2, time_2, display_2);
        VShort multiplier_3 = VShort.of(Short.valueOf("-6"), alarm_2, time_2, display_2);

        VShort posMax = VShort.of(Short.MAX_VALUE, alarm_1, time_1, display_1);
        VShort negMax = VShort.of(Short.MIN_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestMultiplyVUByte() {
        final int UBYTE_MAX = 255;

        VUByte goodResult_1 = VUByte.of(UByte.valueOf(Byte.parseByte("0")), alarm_1, time_1, display_1);
        VUByte goodResult_2 = VUByte.of(UByte.valueOf(Byte.parseByte("18")), alarm_1, time_1, display_1);

        VUByte number = VUByte.of(UByte.valueOf(Byte.parseByte("3")), alarm_1, time_1, display_1);

        VUByte multiplier_1 = VUByte.of(UByte.valueOf(Byte.parseByte("0")), alarm_2, time_2, display_2);
        VUByte multiplier_2 = VUByte.of(UByte.valueOf(Byte.parseByte("6")), alarm_2, time_2, display_2);

        VUByte maxValue = VUByte.of(UBYTE_MAX, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(maxValue, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, true));
        result = SafeMultiply.multiply(multiplier_2, maxValue);
        assertFalse(Utilities.areVTypesIdentical(maxValue, result, true));
        assertTrue(Utilities.areVTypesIdentical(maxValue, result, false));
    }

    @Test
    public void TestMultiplyVByte() {
        VByte goodResult_1 = VByte.of(Byte.valueOf("0"), alarm_1, time_1, display_1);
        VByte goodResult_2 = VByte.of(Byte.valueOf("18"), alarm_1, time_1, display_1);
        VByte goodResult_3 = VByte.of(Byte.valueOf("-18"), alarm_1, time_1, display_1);

        VByte number_1 = VByte.of(Byte.valueOf("3"), alarm_1, time_1, display_1);
        VByte number_2 = VByte.of(Byte.valueOf("-3"), alarm_1, time_1, display_1);

        VByte multiplier_1 = VByte.of(Byte.valueOf("0"), alarm_2, time_2, display_2);
        VByte multiplier_2 = VByte.of(Byte.valueOf("6"), alarm_2, time_2, display_2);
        VByte multiplier_3 = VByte.of(Byte.valueOf("-6"), alarm_2, time_2, display_2);

        VByte posMax = VByte.of(Byte.MAX_VALUE, alarm_1, time_1, display_1);
        VByte negMax = VByte.of(Byte.MIN_VALUE, alarm_1, time_1, display_1);

        // Check if value is correct and Alarm, Time, and Display are the same as the first parameter
        VNumber result = SafeMultiply.multiply(number_1, multiplier_1);
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, true));
        result = SafeMultiply.multiply(multiplier_1, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_1, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_1, result, false));

        result = SafeMultiply.multiply(number_1, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_1);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_3);
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, true));
        result = SafeMultiply.multiply(multiplier_3, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_2, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_2, result, false));

        result = SafeMultiply.multiply(number_2, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, true));
        result = SafeMultiply.multiply(multiplier_2, number_2);
        assertFalse(Utilities.areVTypesIdentical(goodResult_3, result, true));
        assertTrue(Utilities.areVTypesIdentical(goodResult_3, result, false));

        result = SafeMultiply.multiply(posMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(posMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, posMax);
        assertFalse(Utilities.areVTypesIdentical(posMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(posMax, result, false));

        result = SafeMultiply.multiply(negMax, multiplier_2);
        assertTrue(Utilities.areVTypesIdentical(negMax, result, true));
        result = SafeMultiply.multiply(multiplier_2, negMax);
        assertFalse(Utilities.areVTypesIdentical(negMax, result, true));
        assertTrue(Utilities.areVTypesIdentical(negMax, result, false));
    }

    @Test
    public void TestVDoubleArray() {
        final double multiplier = 2d;

        final List<Double> list = Arrays.asList(0.1d, 2.3d, 4.5d, 6.7d);
        ListDouble listDouble = new ListDouble() {
            @Override
            public double getDouble(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VDoubleArray doubleArray = VDoubleArray.of(listDouble, alarm_1, time_1, display_1);
        VNumberArray multipliedDoubleArray = SafeMultiply.multiply(doubleArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Double.compare((Double) SafeMultiply.multiply(list.get(index), multiplier), multipliedDoubleArray.getData().getDouble(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + doubleArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(doubleArray, multiplier));
    }

    @Test
    public void TestVFloatArray() {
        final double multiplier = 2d;

        final List<Float> list = Arrays.asList(0.1f, 2.3f, 4.5f, 6.7f);
        ListFloat listFloat = new ListFloat() {
            @Override
            public float getFloat(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VFloatArray floatArray = VFloatArray.of(listFloat, alarm_1, time_1, display_1);
        VNumberArray multipliedFloatArray = SafeMultiply.multiply(floatArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Float.compare((Float) SafeMultiply.multiply(list.get(index), multiplier), multipliedFloatArray.getData().getFloat(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + floatArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(floatArray, multiplier));
    }

    @Test
    public void TestVULongArray() {
        final double multiplier = 2d;

        final List<ULong> list = Arrays.asList(ULong.valueOf(0L), ULong.valueOf(1L), ULong.valueOf(2L), ULong.valueOf(3L));
        ListULong listULong = new ListULong() {
            @Override
            public long getLong(int i) {
                return list.get(i).longValue();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VULongArray ulongArray = VULongArray.of(listULong, alarm_1, time_1, display_1);
        VNumberArray multipliedULongArray = SafeMultiply.multiply(ulongArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Long.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).longValue(), multipliedULongArray.getData().getLong(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + ulongArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(ulongArray, multiplier));
    }

    @Test
    public void TestVLongArray() {
        final double multiplier = 2d;

        final List<Long> list = Arrays.asList(0L, 1L, 2L, 3L);
        ListLong listLong = new ListLong() {
            @Override
            public long getLong(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VLongArray longArray = VLongArray.of(listLong, alarm_1, time_1, display_1);
        VNumberArray multipliedLongArray = SafeMultiply.multiply(longArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Long.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).longValue(), multipliedLongArray.getData().getLong(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + longArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(longArray, multiplier));
    }

    @Test
    public void TestVUIntegerArray() {
        final double multiplier = 2d;

        final List<UInteger> list = Arrays.asList(UInteger.valueOf(0), UInteger.valueOf(1), UInteger.valueOf(2), UInteger.valueOf(3));
        ListUInteger listUInteger = new ListUInteger() {
            @Override
            public int getInt(int i) {
                return list.get(i).intValue();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VUIntArray integerArray = VUIntArray.of(listUInteger, alarm_1, time_1, display_1);
        VNumberArray multipliedUIntegerArray = SafeMultiply.multiply(integerArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Integer.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).intValue(), multipliedUIntegerArray.getData().getInt(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + integerArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(integerArray, multiplier));
    }

    @Test
    public void TestVIntegerArray() {
        final double multiplier = 2d;

        final List<Integer> list = Arrays.asList(0, 1, 2, 3);
        ListInteger listInteger = new ListInteger() {
            @Override
            public int getInt(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VIntArray intArray = VIntArray.of(listInteger, alarm_1, time_1, display_1);
        VNumberArray multipliedIntegerArray = SafeMultiply.multiply(intArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Integer.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).intValue(), multipliedIntegerArray.getData().getInt(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + intArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(intArray, multiplier));
    }

    @Test
    public void TestVUShortArray() {
        final double multiplier = 2d;

        final List<UShort> list = Arrays.asList(UShort.valueOf(Short.parseShort("0")), UShort.valueOf(Short.parseShort("1")), UShort.valueOf(Short.parseShort("2")), UShort.valueOf(Short.parseShort("3")));
        ListUShort listUShort = new ListUShort() {
            @Override
            public short getShort(int i) {
                return list.get(i).shortValue();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VUShortArray shortArray = VUShortArray.of(listUShort, alarm_1, time_1, display_1);
        VNumberArray multipliedUShortArray = SafeMultiply.multiply(shortArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Short.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).shortValue(), multipliedUShortArray.getData().getShort(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + shortArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(shortArray, multiplier));
    }

    @Test
    public void TestVShortArray() {
        final double multiplier = 2d;

        final List<Short> list = Arrays.asList(Short.parseShort("0"), Short.parseShort("1"), Short.parseShort("2"), Short.parseShort("3"));
        ListShort listShort = new ListShort() {
            @Override
            public short getShort(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VShortArray shortArray = VShortArray.of(listShort, alarm_1, time_1, display_1);
        VNumberArray multipliedShortArray = SafeMultiply.multiply(shortArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Short.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).shortValue(), multipliedShortArray.getData().getShort(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + shortArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(shortArray, multiplier));
    }

    @Test
    public void TestVUByteArray() {
        final double multiplier = 2d;

        final List<UByte> list = Arrays.asList(UByte.valueOf(Byte.parseByte("0")), UByte.valueOf(Byte.parseByte("1")), UByte.valueOf(Byte.parseByte("2")), UByte.valueOf(Byte.parseByte("3")));
        ListUByte listUByte = new ListUByte() {
            @Override
            public byte getByte(int i) {
                return list.get(i).byteValue();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VUByteArray byteArray = VUByteArray.of(listUByte, alarm_1, time_1, display_1);
        VNumberArray multipliedUByteArray = SafeMultiply.multiply(byteArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Byte.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).byteValue(), multipliedUByteArray.getData().getByte(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + byteArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(byteArray, multiplier));
    }

    @Test
    public void TestVByteArray() {
        final double multiplier = 2d;

        final List<Byte> list = Arrays.asList(Byte.parseByte("0"), Byte.parseByte("1"), Byte.parseByte("2"), Byte.parseByte("3"));
        ListByte listByte = new ListByte() {
            @Override
            public byte getByte(int i) {
                return list.get(i);
            }

            @Override
            public int size() {
                return list.size();
            }
        };
        VByteArray byteArray = VByteArray.of(listByte, alarm_1, time_1, display_1);
        VNumberArray multipliedByteArray = SafeMultiply.multiply(byteArray, multiplier);

        for (int index = 0; index < list.size(); index++) {
            assertTrue(Byte.compareUnsigned(SafeMultiply.multiply(list.get(index), multiplier).byteValue(), multipliedByteArray.getData().getByte(index)) == 0);
        }

        System.out.println("multiplier: " + multiplier);
        System.out.println("  Original: " + byteArray);
        System.out.println("Multiplied: " + SafeMultiply.multiply(byteArray, multiplier));
    }
}
