/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.List;
import org.phoebus.util.array.ListBoolean;
import org.phoebus.util.array.ListByte;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListLong;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.array.ListShort;

/**
 * Default implementation for formatting.
 *
 * @author carcassi
 */
public class SimpleValueFormat extends ValueFormat {

    private int maxElements;

    /**
     * Formats any scalar and array, by using the server side formatting
     * and limiting the elements of the array displayed to maxElements.
     *
     * @param maxElements maximum number of array elements converted to string
     */
    public SimpleValueFormat(int maxElements) {
        this.maxElements = maxElements;
    }

    @Override
    protected StringBuffer format(Scalar scalar, StringBuffer toAppendTo, FieldPosition pos) {
        if (scalar == null || scalar.getValue() == null) {
            return toAppendTo;
        }

        if (scalar instanceof Display && nf(scalar) != null) {
            NumberFormat f = nf(scalar);
            return f.format(scalar.getValue(), toAppendTo, pos);
        }

        toAppendTo.append(scalar.getValue());
        return toAppendTo;
    }

    /**
     * Returns the appropriate NumberFormat: either the one
     * from the data or the set by the formatting options.
     *
     * @param obj data object
     * @return number format
     */
    private NumberFormat nf(Object obj) {
        if (getNumberFormat() != null)
            return getNumberFormat();

        if (obj instanceof Display) {
            return ((Display) obj).getFormat();
        }

        return null;
    }

    /**
     * Formats a numeric array. This method can be overridden to change
     * the way numeric arrays are formatted.
     *
     * @param array the array to format
     * @param toAppendTo the buffer to append to
     * @param pos the position of the field
     * @return  the string buffer
     */
    protected StringBuffer format(VNumberArray array, StringBuffer toAppendTo, FieldPosition pos) {
        NumberFormat f = nf(array);

        toAppendTo.append("[");
        boolean hasMore = false;

        ListNumber data = array.getData();
        if (data.size() > maxElements) {
            hasMore = true;
        }

        for (int i = 0; i < Math.min(data.size(), maxElements); i++) {
            if (i != 0) {
                toAppendTo.append(", ");
            }
            if (data instanceof ListByte || data instanceof ListShort || data instanceof ListInt || data instanceof ListLong) {
                toAppendTo.append(f.format(data.getLong(i)));
            } else {
                toAppendTo.append(f.format(data.getDouble(i)));
            }
        }

        if (hasMore) {
            toAppendTo.append(", ...");
        }
        toAppendTo.append("]");
        return toAppendTo;
    }

    /**
     * Formats a string array. This method can be overridden to change
     * the way string arrays are formatted.
     *
     * @param data the data to format
     * @param toAppendTo the buffer to append to
     * @param pos the position of the field
     * @return  the string buffer
     */
    protected StringBuffer format(List<String> data, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append("[");
        boolean hasMore = false;

        if (data.size() > maxElements) {
            hasMore = true;
        }

        for (int i = 0; i < Math.min(data.size(), maxElements); i++) {
            if (i != 0) {
                toAppendTo.append(", ");
            }
            toAppendTo.append(data.get(i));
        }

        if (hasMore) {
            toAppendTo.append(", ...");
        }
        toAppendTo.append("]");
        return toAppendTo;
    }

    /**
     * Formats a boolean array. This method can be overridden to change
     * the way string arrays are formatted.
     *
     * @param data the data to format
     * @param toAppendTo the buffer to append to
     * @param pos the position of the field
     * @return  the string buffer
     */
    protected StringBuffer format(ListBoolean data, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append("[");
        boolean hasMore = false;

        if (data.size() > maxElements) {
            hasMore = true;
        }

        for (int i = 0; i < Math.min(data.size(), maxElements); i++) {
            if (i != 0) {
                toAppendTo.append(", ");
            }
            toAppendTo.append(data.getBoolean(i));
        }

        if (hasMore) {
            toAppendTo.append(", ...");
        }
        toAppendTo.append("]");
        return toAppendTo;
    }

    @Override
    protected StringBuffer format(Array array, StringBuffer toAppendTo, FieldPosition pos) {
        if (array instanceof VNumberArray) {
            return format((VNumberArray) array, toAppendTo, pos);
        }

        if (array instanceof VStringArray) {
            return format(((VStringArray) array).getData(), toAppendTo, pos);
        }

        if (array instanceof VBooleanArray) {
            return format(((VBooleanArray) array).getData(), toAppendTo, pos);
        }

        if (array instanceof VEnumArray) {
            return format(((VEnumArray) array).getData(), toAppendTo, pos);
        }

        throw new UnsupportedOperationException("Type " + array.getClass().getName() + " not yet supported.");
    }
}
