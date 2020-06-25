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

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ArrayUByte;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.util.array.ArrayUShort;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.util.number.UnsignedConversions;
import org.epics.vtype.Alarm;
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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * {@link SafeMultiply} class handles overflow and underflow of multiplication
 * between number types of some extensions of {@link Number} and {@link VNumber}.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SafeMultiply {
    /**
     * Multiply two same {@link Number} objects.
     *
     * @param a a {@link Number} object
     * @param b a {@link Number} object
     * @return a*b {@link Number} object bound by its maximum and minimum values.
     */
    public static Number multiply(Number a, Number b) throws UnsupportedOperationException {
        if (a instanceof Double && b instanceof Double) {
            return multiply((Double) a, (Double) b);
        } else if (a instanceof Float && b instanceof Float) {
            return multiply((Float) a, (Float) b);
        } else if (a instanceof ULong && b instanceof ULong) {
            return multiply((ULong) a, (ULong) b);
        } else if (a instanceof Long && b instanceof Long) {
            return multiply((Long) a, (Long) b);
        } else if (a instanceof UInteger && b instanceof UInteger) {
            return multiply((UInteger) a, (UInteger) b);
        } else if (a instanceof Integer && b instanceof Integer) {
            return multiply((Integer) a, (Integer) b);
        } else if (a instanceof UShort && b instanceof UShort) {
            return multiply((UShort) a, (UShort) b);
        } else if (a instanceof Short && b instanceof Short) {
            return multiply((Short) a, (Short) b);
        } else if (a instanceof UByte && b instanceof UByte) {
            return multiply((UByte) a, (UByte) b);
        } else if (a instanceof Byte && b instanceof Byte) {
            return multiply((Byte) a, (Byte) b);
        }

        if (!a.getClass().equals(b.getClass())) {
            throw new UnsupportedOperationException("Multiplication between different types is allowed only when second argument is Double!");
        } else {
            throw new UnsupportedOperationException("Provided Number extension " + a.getClass() + " is unsupported!");
        }
    }

    /**
     * Multiply {@link Number} and {@link Double} objects.
     *
     * @param a a {@link Number} object
     * @param b a {@link Double} object
     * @return a*b of {@param a} class object bound by {@param a}'s maximum and minimum values.
     */
    public static Number multiply(Number a, Double b) throws UnsupportedOperationException {
        if (a instanceof Double) {
            return multiply((Double) a, b);
        } else if (a instanceof Float) {
            return multiply((Float) a, b);
        } else if (a instanceof ULong) {
            return multiply((ULong) a, b);
        } else if (a instanceof Long) {
            return multiply((Long) a, b);
        } else if (a instanceof UInteger) {
            return multiply((UInteger) a, b);
        } else if (a instanceof Integer) {
            return multiply((Integer) a, b);
        } else if (a instanceof UShort) {
            return multiply((UShort) a, b);
        } else if (a instanceof Short) {
            return multiply((Short) a, b);
        } else if (a instanceof UByte) {
            return multiply((UByte) a, b);
        } else if (a instanceof Byte) {
            return multiply((Byte) a, b);
        }

        throw new UnsupportedOperationException("Provided Number extension " + a.getClass() + " is unsupported!");
    }

    /**
     * Multiply two {@link Double} numbers.
     *
     * @param a a {@link Double} object
     * @param b a {@link Double} object
     * @return a*b {@link Double} object bound by -Double.MAX_VALUE and Double.MAX_VALUE
     */
    private static Double multiply(Double a, Double b) {
        if (a == 0 || b == 0) {
            return 0.0;
        }

        double bigNumber = Double.max(Math.abs(a), Math.abs(b));
        double smallNumber = Double.min(Math.abs(a), Math.abs(b));

        double maxMultiplier = Double.MAX_VALUE/bigNumber;
        if (smallNumber < maxMultiplier) {
            return a*b;
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return -Double.MAX_VALUE;
            } else {
                return Double.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply two {@link Float} numbers.
     *
     * @param a a {@link Float} object
     * @param b a {@link Float} object
     * @return a*b {@link Float} object bound by -Float.MAX_VALUE and Float.MAX_VALUE
     */
    private static Float multiply(Float a, Float b) {
        if (a == 0 || b == 0) {
            return 0.0F;
        }

        float bigNumber = Float.max(Math.abs(a), Math.abs(b));
        float smallNumber = Float.min(Math.abs(a), Math.abs(b));

        float maxMultiplier = Float.MAX_VALUE/bigNumber;
        if (smallNumber < maxMultiplier) {
            return a*b;
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return -Float.MAX_VALUE;
            } else {
                return Float.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply {@link Float} and {@link Double} numbers.
     *
     * @param number a {@link Float} object
     * @param multiplier a {@link Double} object
     * @return a*b {@link Float} object bound by -Float.MAX_VALUE and Float.MAX_VALUE
     */
    private static Float multiply(Float number, Double multiplier) {
        if (number == 0 || multiplier == 0) {
            return 0.0F;
        }

        double bigNumber = Double.max(Math.abs(number), Math.abs(multiplier));
        double smallNumber = Double.min(Math.abs(number), Math.abs(multiplier));

        double maxMultiplier = Float.MAX_VALUE/bigNumber;
        if (smallNumber < maxMultiplier) {
            return Double.valueOf(number*multiplier).floatValue();
        } else {
            if ((number > 0 && multiplier < 0) || (number < 0 && multiplier > 0)) {
                return -Float.MAX_VALUE;
            } else {
                return Float.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply two {@link Long} numbers.
     *
     * @param a a {@link ULong} object
     * @param b a {@link ULong} object
     * @return a*b {@link ULong} object bound by Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static ULong multiply(ULong a, ULong b) {
        if (a.bigIntegerValue().compareTo(BigInteger.ZERO) == 0|| b.bigIntegerValue().compareTo(BigInteger.ZERO) == 0) {
            return ULong.valueOf(0);
        }

        final BigInteger ULONG_MAX = BigInteger.valueOf(9223372036854775807L).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(1));

        final BigInteger unsignedA = a.bigIntegerValue();
        final BigInteger unsignedB = b.bigIntegerValue();

        final BigInteger multipliedValue = unsignedA.multiply(unsignedB);

        return ULong.valueOf(multipliedValue.min(ULONG_MAX).longValue());
    }

    /**
     * Multiply {@link Long} and {@link Double} numbers.
     *
     * @param a a {@link ULong} object
     * @param b a {@link Double} object
     * @return a*b {@link ULong} object bound by 0 and ULONG_MAX(18,446,744,073,709,551,615)
     */
    private static ULong multiply(ULong a, Double b) {
        if (a.bigIntegerValue().compareTo(BigInteger.ZERO) == 0|| b <= 0) {
            return ULong.valueOf(0);
        }

        final BigInteger ULONG_MAX = BigInteger.valueOf(9223372036854775807L).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(1));

        final double unsignedA = a.bigIntegerValue().doubleValue();

        final Double multipliedValue = multiply(unsignedA, b);

        return ULong.valueOf(ULONG_MAX.min(BigDecimal.valueOf(multipliedValue).toBigInteger()).longValue());
    }

    /**
     * Multiply two {@link Long} numbers.
     *
     * @param a a {@link Long} object
     * @param b a {@link Long} object
     * @return a*b {@link Long} object bound by Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static Long multiply(Long a, Long b) {
        if (a == 0 || b == 0) {
            return 0L;
        }

        BigInteger bigNumber = BigInteger.valueOf(a).abs().max(BigInteger.valueOf(b).abs());
        BigInteger smallNumber = BigInteger.valueOf(a).abs().min(BigInteger.valueOf(b).abs());

        BigInteger maxMultiplier = BigInteger.valueOf(Long.MAX_VALUE).divide(bigNumber);
        if (smallNumber.compareTo(maxMultiplier) < 0) {
            return a*b;
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return Long.MIN_VALUE;
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply {@link Long} and {@link Double} numbers.
     *
     * @param number a {@link Long} object
     * @param multiplier a {@link Double} object
     * @return a*b {@link Long} object bound by Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static Long multiply(Long number, Double multiplier) {
        if (number == 0 || multiplier == 0) {
            return 0L;
        }

        double bigNumber = Double.max(Math.abs(number), Math.abs(multiplier));
        double smallNumber = Double.min(Math.abs(number), Math.abs(multiplier));

        double maxMultiplier = Long.MAX_VALUE/bigNumber;
        if (smallNumber < maxMultiplier) {
            return Double.valueOf(number*multiplier).longValue();
        } else {
            if ((number > 0 && multiplier < 0) || (number < 0 && multiplier > 0)) {
                return Long.MIN_VALUE;
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply two {@link UInteger} numbers.
     *
     * @param a a {@link UInteger} object
     * @param b a {@link UInteger} object
     * @return a*b {@link UInteger} object bound by 0 and UINT_MAX(4,294,967,295)
     */
    private static UInteger multiply(UInteger a, UInteger b) {
        if (a.longValue() == 0 || b.longValue() == 0) {
            return UInteger.valueOf(0);
        }

        final long UINT_MAX = 4294967295L;

        final long unsignedA = UnsignedConversions.toLong(a.intValue());
        final long unsignedB = UnsignedConversions.toLong(b.intValue());

        final long multipliedValue = multiply(unsignedA, unsignedB);

        return UInteger.valueOf(Long.valueOf(Long.min(multipliedValue, UINT_MAX)).intValue());
    }

    /**
     * Multiply two {@link UInteger} numbers.
     *
     * @param a a {@link UInteger} object
     * @param b a {@link Double} object
     * @return a*b {@link UInteger} object bound by 0 and UINT_MAX(4,294,967,295)
     */
    private static UInteger multiply(UInteger a, Double b) {
        if (a.longValue() == 0 || b <= 0) {
            return UInteger.valueOf(0);
        }

        final long UINT_MAX = 4294967295L;

        final long unsignedA = UnsignedConversions.toLong(a.intValue());

        final long multipliedValue = multiply(unsignedA, b);

        return UInteger.valueOf(Long.valueOf(Long.min(multipliedValue, UINT_MAX)).intValue());
    }

    /**
     * Multiply two {@link Integer} numbers.
     *
     * @param a a {@link Integer} object
     * @param b a {@link Integer} object
     * @return a*b {@link Integer} object bound by Integer.MIN_VALUE and Integer.MAX_VALUE
     */
    private static Integer multiply(Integer a, Integer b) {
        if (a == 0 || b == 0) {
            return 0;
        }

        long bigNumber = Long.max(Math.abs((long) a), Math.abs((long) b));
        long smallNumber = Long.min(Math.abs((long) a), Math.abs((long) b));

        long maxMultiplier = Integer.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return a * b;
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return Integer.MIN_VALUE;
            } else {
                return Integer.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply {@link Integer} and {@link Double} numbers.
     *
     * @param number a {@link Integer} object
     * @param multiplier a {@link Double} object
     * @return a*b {@link Integer} object bound by Integer.MIN_VALUE and Integer.MAX_VALUE
     */
    private static Integer multiply(Integer number, Double multiplier) {
        if (number == 0 || multiplier == 0) {
            return 0;
        }

        double bigNumber = Double.max(Math.abs(number), Math.abs(multiplier));
        double smallNumber = Double.min(Math.abs(number), Math.abs(multiplier));

        double maxMultiplier = Integer.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return Double.valueOf(number*multiplier).intValue();
        } else {
            if ((number > 0 && multiplier < 0) || (number < 0 && multiplier > 0)) {
                return Integer.MIN_VALUE;
            } else {
                return Integer.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply two {@link UShort} numbers.
     *
     * @param a a {@link UShort} object
     * @param b a {@link UShort} object
     * @return a*b {@link UShort} object bound by 0 and USHORT_MAX(65535)
     */
    private static UShort multiply(UShort a, UShort b) {
        if (a.intValue() == 0 || b.intValue() == 0) {
            return UShort.valueOf(Short.parseShort("0"));
        }

        final int USHORT_MAX = 65535;

        final int unsignedA = UnsignedConversions.toInt(a.shortValue());
        final int unsignedB = UnsignedConversions.toInt(b.shortValue());

        final int multipliedValue = multiply(unsignedA, unsignedB);

        return UShort.valueOf(Integer.valueOf(Integer.min(multipliedValue, USHORT_MAX)).shortValue());
    }

    /**
     * Multiply two {@link UShort} numbers.
     *
     * @param a a {@link UShort} object
     * @param b a {@link Double} object
     * @return a*b {@link UShort} object bound by 0 and USHORT_MAX(65535)
     */
    private static UShort multiply(UShort a, Double b) {
        if (a.intValue() == 0 || b <= 0) {
            return UShort.valueOf(Short.parseShort("0"));
        }

        final int USHORT_MAX = 65535;

        final int unsignedA = UnsignedConversions.toInt(a.shortValue());

        final int multipliedValue = multiply(unsignedA, b);

        return UShort.valueOf(Integer.valueOf(Integer.min(multipliedValue, USHORT_MAX)).shortValue());
    }

    /**
     * Multiply two {@link Short} numbers.
     *
     * @param a a {@link Short} object
     * @param b a {@link Short} object
     * @return a*b {@link Short} object bound by Short.MIN_VALUE and Short.MAX_VALUE
     */
    private static Short multiply(Short a, Short b) {
        if (a == 0 || b == 0) {
            return 0;
        }

        int bigNumber = Integer.max(Math.abs(a), Math.abs(b));
        int smallNumber = Integer.min(Math.abs(a), Math.abs(b));

        int maxMultiplier = Short.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return Integer.valueOf(a * b).shortValue();
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return Short.MIN_VALUE;
            } else {
                return Short.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply {@link Short} and {@link Double} numbers.
     *
     * @param number a {@link Short} object
     * @param multiplier a {@link Double} object
     * @return a*b {@link Short} object bound by Short.MIN_VALUE and Short.MAX_VALUE
     */
    private static Short multiply(Short number, Double multiplier) {
        if (number == 0 || multiplier == 0) {
            return 0;
        }

        double bigNumber = Double.max(Math.abs(number), Math.abs(multiplier));
        double smallNumber = Double.min(Math.abs(number), Math.abs(multiplier));

        double maxMultiplier = Short.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return Double.valueOf(number * multiplier).shortValue();
        } else {
            if ((number > 0 && multiplier < 0) || (number < 0 && multiplier > 0)) {
                return Short.MIN_VALUE;
            } else {
                return Short.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply two {@link UByte} numbers.
     *
     * @param a a {@link UByte} object
     * @param b a {@link UByte} object
     * @return a*b {@link UByte} object bound by Byte.MIN_VALUE and Byte.MAX_VALUE
     */
    private static UByte multiply(UByte a, UByte b) {
        if (a.intValue() == 0 || b.intValue() == 0) {
            return UByte.valueOf(Byte.parseByte("0"));
        }

        final int UBYTE_MAX = 255;

        final int unsignedA = UnsignedConversions.toInt(a.byteValue());
        final int unsignedB = UnsignedConversions.toInt(b.byteValue());

        final int multipliedValue = multiply(unsignedA, unsignedB);

        return UByte.valueOf(Integer.valueOf(Integer.min(multipliedValue, UBYTE_MAX)).byteValue());
    }

    /**
     * Multiply {@link UByte} and {@link Double} numbers.
     *
     * @param a a {@link UByte} object
     * @param b a {@link Double} object
     * @return a*b {@link UByte} object bound by 0 and UBYTE_MAX(255)
     */
    private static UByte multiply(UByte a, Double b) {
        if (a.intValue() == 0 || b <= 0) {
            return UByte.valueOf(Byte.parseByte("0"));
        }

        final int UBYTE_MAX = 255;

        final int unsignedA = UnsignedConversions.toInt(a.byteValue());

        final int multipliedValue = multiply(unsignedA, b);

        return UByte.valueOf(Integer.valueOf(Integer.min(multipliedValue, UBYTE_MAX)).byteValue());
    }

    /**
     * Multiply two {@link Byte} numbers.
     *
     * @param a a {@link Byte} object
     * @param b a {@link Byte} object
     * @return a*b {@link Byte} object bound by Byte.MIN_VALUE and Byte.MAX_VALUE
     */
    private static Byte multiply(Byte a, Byte b) {
        if (a == 0 || b == 0) {
            return 0;
        }

        int bigNumber = Integer.max(Math.abs(a), Math.abs(b));
        int smallNumber = Integer.min(Math.abs(a), Math.abs(b));

        int maxMultiplier = Byte.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return Integer.valueOf(a * b).byteValue();
        } else {
            if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
                return Byte.MIN_VALUE;
            } else {
                return Byte.MAX_VALUE;
            }
        }
    }

    /**
     * Multiply {@link Byte} and {@link Double} numbers.
     *
     * @param number a {@link Byte} object
     * @param multiplier a {@link Double} object
     * @return a*b {@link Byte} object bound by Byte.MIN_VALUE and Byte.MAX_VALUE
     */
    private static Byte multiply(Byte number, Double multiplier) {
        if (number == 0 || multiplier == 0) {
            return 0;
        }

        double bigNumber = Double.max(Math.abs(number), Math.abs(multiplier));
        double smallNumber = Double.min(Math.abs(number), Math.abs(multiplier));

        double maxMultiplier = Byte.MAX_VALUE / bigNumber;
        if (smallNumber < maxMultiplier) {
            return Double.valueOf(number*multiplier).byteValue();
        } else {
            if ((number > 0 && multiplier < 0) || (number < 0 && multiplier > 0)) {
                return Byte.MIN_VALUE;
            } else {
                return Byte.MAX_VALUE;
            }
        }
    }

    /**
     * Implementation for multiplying two same {@link VNumber} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VNumber} object with {@param number}*{@param multiplier} if possible
     */
    public static VNumber multiply(VNumber number, VNumber multiplier) throws UnsupportedOperationException {
        if (number instanceof VDouble && multiplier instanceof VDouble) {
            return multiply((VDouble) number, (VDouble) multiplier);
        } else if (number instanceof VFloat && multiplier instanceof VFloat) {
            return multiply((VFloat) number, (VFloat) multiplier);
        } else if (number instanceof VULong && multiplier instanceof VULong) {
            return multiply((VULong) number, (VULong) multiplier);
        } else if (number instanceof VLong && multiplier instanceof VLong) {
            return multiply((VLong) number, (VLong) multiplier);
        } else if (number instanceof VUInt && multiplier instanceof VUInt) {
            return multiply((VUInt) number, (VUInt) multiplier);
        } else if (number instanceof VInt && multiplier instanceof VInt) {
            return multiply((VInt) number, (VInt) multiplier);
        } else if (number instanceof VUShort && multiplier instanceof VUShort) {
            return multiply((VUShort) number, (VUShort) multiplier);
        } else if (number instanceof VShort && multiplier instanceof VShort) {
            return multiply((VShort) number, (VShort) multiplier);
        } else if (number instanceof VUByte && multiplier instanceof VUByte) {
            return multiply((VUByte) number, (VUByte) multiplier);
        } else if (number instanceof VByte && multiplier instanceof VByte) {
            return multiply((VByte) number, (VByte) multiplier);
        }

        if (!number.getClass().equals(multiplier.getClass())) {
            throw new UnsupportedOperationException("Multiplication between different types is not supported except for VDouble multiplier!");
        } else {
            throw new UnsupportedOperationException("Provided Number extension " + number.getClass() + " is unsupported!");
        }
    }

    /**
     * Implementation for multiplying {@link VNumber} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VNumber} object with {@param number}*{@param multiplier} if possible
     */
    public static VNumber multiply(VNumber number, VDouble multiplier) throws UnsupportedOperationException {
        if (number instanceof VDouble) {
            return multiply((VDouble) number, multiplier);
        } else if (number instanceof VFloat) {
            return multiply((VFloat) number, multiplier);
        } else if (number instanceof VULong) {
            return multiply((VULong) number, multiplier);
        } else if (number instanceof VLong) {
            return multiply((VLong) number, multiplier);
        } else if (number instanceof VUInt) {
            return multiply((VUInt) number, multiplier);
        } else if (number instanceof VInt) {
            return multiply((VInt) number, multiplier);
        } else if (number instanceof VUShort) {
            return multiply((VUShort) number, multiplier);
        } else if (number instanceof VShort) {
            return multiply((VShort) number, multiplier);
        } else if (number instanceof VUByte) {
            return multiply((VUByte) number, multiplier);
        } else if (number instanceof VByte) {
            return multiply((VByte) number, multiplier);
        }

        throw new UnsupportedOperationException("Provided Number extension " + number.getClass() + " is unsupported!");
    }

    /**
     * Implementation for multiplying {@link VNumber} and {@link Double} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the multiplication factor
     * @return {@link VNumber} object with {@param number}*{@param multiplier} if possible
     */
    public static VNumber multiply(VNumber number, Double multiplier) throws UnsupportedOperationException {
        return multiply(number, VDouble.of(multiplier, Alarm.none(), Time.nowInvalid(), Display.none()));
    }

    /**
     * Implementation for multiplying two VDouble objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VDouble} object with {@param number}*{@param multiplier} if possible
     */
    private static VDouble multiply(VDouble number, VDouble multiplier) {
        return VDouble.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VFloat} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VFloat} object with {@param number}*{@param multiplier} if possible
     */
    private static VFloat multiply(VFloat number, VFloat multiplier) {
        return VFloat.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VFloat} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VFloat} object with {@param number}*{@param multiplier} if possible
     */
    private static VFloat multiply(VFloat number, VDouble multiplier) {
        return VFloat.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VULong} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VULong} object with {@param number}*{@param multiplier} if possible
     */
    private static VULong multiply(VULong number, VULong multiplier) {
        return VULong.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VULong} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VULong} object with {@param number}*{@param multiplier} if possible
     */
    private static VULong multiply(VULong number, VDouble multiplier) {
        return VULong.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VLong} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VLong} object with {@param number}*{@param multiplier} if possible
     */
    private static VLong multiply(VLong number, VLong multiplier) {
        return VLong.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VLong} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VLong} object with {@param number}*{@param multiplier} if possible
     */
    private static VLong multiply(VLong number, VDouble multiplier) {
        return VLong.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VUInt} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUInt} object with {@param number}*{@param multiplier} if possible
     */
    private static VUInt multiply(VUInt number, VUInt multiplier) {
        return VUInt.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VUInt} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUInt} object with {@param number}*{@param multiplier} if possible
     */
    private static VUInt multiply(VUInt number, VDouble multiplier) {
        return VUInt.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VInt} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VInt} object with {@param number}*{@param multiplier} if possible
     */
    private static VInt multiply(VInt number, VInt multiplier) {
        return VInt.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VInt} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VInt} object with {@param number}*{@param multiplier} if possible
     */
    private static VInt multiply(VInt number, VDouble multiplier) {
        return VInt.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VUShort} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUShort} object with {@param number}*{@param multiplier} if possible
     */
    private static VUShort multiply(VUShort number, VUShort multiplier) {
        return VUShort.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VUShort} and {@link VDouble}objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUShort} object with {@param number}*{@param multiplier} if possible
     */
    private static VUShort multiply(VUShort number, VDouble multiplier) {
        return VUShort.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VShort} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VShort} object with {@param number}*{@param multiplier} if possible
     */
    private static VShort multiply(VShort number, VShort multiplier) {
        return VShort.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VShort} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VShort} object with {@param number}*{@param multiplier} if possible
     */
    private static VShort multiply(VShort number, VDouble multiplier) {
        return VShort.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VUByte} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUByte} object with {@param number}*{@param multiplier} if possible
     */
    private static VUByte multiply(VUByte number, VUByte multiplier) {
        return VUByte.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VUByte} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VUByte} object with {@param number}*{@param multiplier} if possible
     */
    private static VUByte multiply(VUByte number, VDouble multiplier) {
        return VUByte.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying two {@link VByte} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VByte} object with {@param number}*{@param multiplier} if possible
     */
    private static VByte multiply(VByte number, VByte multiplier) {
        return VByte.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Implementation for multiplying {@link VByte} and {@link VDouble} objects.
     *
     * @param number the parameter whose {@link Alarm}, {@link Time}, and {@link Display} are preserved.
     * @param multiplier the parameter whose value is extracted for multiplication
     * @return {@link VByte} object with {@param number}*{@param multiplier} if possible
     */
    private static VByte multiply(VByte number, VDouble multiplier) {
        return VByte.of(multiply(number.getValue(), multiplier.getValue()), number.getAlarm(), number.getTime(), number.getDisplay());
    }

    /**
     * Multiply each element of {@link VNumberArray} and {@link Double} objects.
     *
     * @param numberArray a {@link VNumberArray} object
     * @param multiplier a {@link Double} object
     * @return {@link VNumberArray} whose elements multiplied by the multiplier bound by each extension array type
     */
    public static VNumberArray multiply(VNumberArray numberArray, Double multiplier) throws UnsupportedOperationException {
        if (numberArray instanceof VDoubleArray) {
            return multiply((VDoubleArray) numberArray, multiplier);
        } else if (numberArray instanceof VFloatArray) {
            return multiply((VFloatArray) numberArray, multiplier);
        } else if (numberArray instanceof VULongArray) {
            return multiply((VULongArray) numberArray, multiplier);
        } else if (numberArray instanceof VLongArray) {
            return multiply((VLongArray) numberArray, multiplier);
        } else if (numberArray instanceof VUIntArray) {
            return multiply((VUIntArray) numberArray, multiplier);
        } else if (numberArray instanceof VIntArray) {
            return multiply((VIntArray) numberArray, multiplier);
        } else if (numberArray instanceof VUShortArray) {
            return multiply((VUShortArray) numberArray, multiplier);
        } else if (numberArray instanceof VShortArray) {
            return multiply((VShortArray) numberArray, multiplier);
        } else if (numberArray instanceof VUByteArray) {
            return multiply((VUByteArray) numberArray, multiplier);
        } else if (numberArray instanceof VByteArray) {
            return multiply((VByteArray) numberArray, multiplier);
        }

        throw new UnsupportedOperationException("Provided VNumberArray extension " + numberArray.getClass() + " is unsupported!");
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VDoubleArray}.
     *
     * @param numberArray a {@link VDoubleArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VDoubleArray} whose elements multiplied by the multiplier bound by -Double.MAX_VALUE and Double.MAX_VALUE
     */
    private static VDoubleArray multiply(VDoubleArray numberArray, Double multiplier) {
        double[] numberArrayData = new double[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VDoubleArray.of(ArrayDouble.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VFloatArray}.
     *
     * @param numberArray a {@link VFloatArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VFloatArray} whose elements multiplied by the multiplier bound by -Float.MAX_VALUE and Float.MAX_VALUE
     */
    private static VFloatArray multiply(VFloatArray numberArray, Double multiplier) {
        float[] numberArrayData = new float[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VFloatArray.of(ArrayFloat.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VULongArray}.
     *
     * @param numberArray a {@link VULongArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VULongArray} whose elements multiplied by the multiplier bound by 0 and ULONG_MAX(18,446,744,073,709,551,615)
     */
    private static VULongArray multiply(VULongArray numberArray, Double multiplier) {
        long[] numberArrayData = new long[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(ULong.valueOf(numberArrayData[index]), multiplier).longValue();
        }

        return VULongArray.of(ArrayULong.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VLongArray}.
     *
     * @param numberArray a {@link VLongArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VLongArray} whose elements multiplied by the multiplier bound by Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static VLongArray multiply(VLongArray numberArray, Double multiplier) {
        long[] numberArrayData = new long[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VLongArray.of(ArrayLong.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VUIntArray}.
     *
     * @param numberArray a {@link VUIntArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VUIntArray} whose elements multiplied by the multiplier bound by 0 and UINT_MAX(4,294,967,295)
     */
    private static VUIntArray multiply(VUIntArray numberArray, Double multiplier) {
        int[] numberArrayData = new int[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(UInteger.valueOf(numberArrayData[index]), multiplier).intValue();
        }

        return VUIntArray.of(ArrayUInteger.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VIntArray}.
     *
     * @param numberArray a {@link VIntArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VIntArray} whose elements multiplied by the multiplier bound by Int.MIN_VALUE and Int.MAX_VALUE
     */
    private static VIntArray multiply(VIntArray numberArray, Double multiplier) {
        int[] numberArrayData = new int[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VIntArray.of(ArrayInteger.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VUShortArray}.
     *
     * @param numberArray a {@link VUShortArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VUShortArray} whose elements multiplied by the multiplier bound by 0 and USHORT_MAX(65,535)
     */
    private static VUShortArray multiply(VUShortArray numberArray, Double multiplier) {
        short[] numberArrayData = new short[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(UShort.valueOf(numberArrayData[index]), multiplier).shortValue();
        }

        return VUShortArray.of(ArrayUShort.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VShortArray}.
     *
     * @param numberArray a {@link VShortArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VShortArray} whose elements multiplied by the multiplier bound by Short.MIN_VALUE and Short.MAX_VALUE
     */
    private static VShortArray multiply(VShortArray numberArray, Double multiplier) {
        short[] numberArrayData = new short[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VShortArray.of(ArrayShort.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VUByteArray}.
     *
     * @param numberArray a {@link VUByteArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VUByteArray} whose elements multiplied by the multiplier bound by 0 and UBYTE_MAX(255)
     */
    private static VUByteArray multiply(VUByteArray numberArray, Double multiplier) {
        byte[] numberArrayData = new byte[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(UByte.valueOf(numberArrayData[index]), multiplier).byteValue();
        }

        return VUByteArray.of(ArrayUByte.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }

    /**
     * Implementation of multiplying a {@link Double} multiplier number to {@link VByteArray}.
     *
     * @param numberArray a {@link VByteArray} object which the multiplier is multiplied to.
     * @param multiplier a {@link Double} multiplier number
     * @return {@link VByteArray} whose elements multiplied by the multiplier bound by Byte.MIN_VALUE and Byte.MAX_VALUE
     */
    private static VByteArray multiply(VByteArray numberArray, Double multiplier) {
        byte[] numberArrayData = new byte[numberArray.getData().size()];
        numberArray.getData().toArray(numberArrayData);

        for (int index = 0; index < numberArrayData.length; index++) {
            numberArrayData[index] = SafeMultiply.multiply(numberArrayData[index], multiplier);
        }

        return VByteArray.of(ArrayByte.of(numberArrayData), numberArray.getAlarm(), numberArray.getTime(), numberArray.getDisplay());
    }
}
