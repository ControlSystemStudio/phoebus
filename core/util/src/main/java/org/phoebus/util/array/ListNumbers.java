/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * Utilities for manipulating ListNumbers.
 *
 * @author carcassi
 */
public class ListNumbers {

    /**
     * Creates a sorted view of the given ListNumber.
     * <p>
     * The ListNumber is not sorted in place, and the data is not copied out.
     * Therefore it's intended that the ListNumber is not changed while
     * the view is used.
     *
     * @param values the values to be sorted
     * @return the sorted view
     */
    public static SortedListView sortedView(ListNumber values) {
        SortedListView view = new SortedListView(values);
        if (values.size() <= 1) {
            // Nothing to sort
            return view;
        }

        double value = values.getDouble(0);
        for (int i = 1; i < values.size(); i++) {
            double newValue = values.getDouble(i);
            if (value > newValue) {
                SortedListView.quicksort(view);
                return view;
            }
            value = newValue;
        }

        return view;
    }

    /**
     * Creates a sorted view of the given ListNumber based on the indexes provided.
     * This method can be used to sort the given values based on the ordering
     * by another (sorted) list of values.
     * <p>
     * The ListNumber is not sorted in place, and the data is not copied out.
     * Therefore it's intended that the ListNumber is not changed while
     * the view is used.
     *
     * @param values the values to be sorted
     * @param indexes the ordering to be used for the view
     * @return the sorted view
     */
    public static SortedListView sortedView(ListNumber values, ListInt indexes) {
        SortedListView view = new SortedListView(values, indexes);
        return view;
    }

    /**
     * Finds the value in the list, or the one right below it.
     *
     * @param values a list of values
     * @param value a value
     * @return the index of the value
     */
    public static int binarySearchValueOrLower(ListNumber values, double value) {
        if (value <= values.getDouble(0)) {
            return 0;
        }
        if (value >= values.getDouble(values.size() -1)) {
            return values.size() - 1;
        }

        int index = binarySearch(0, values.size() - 1, values, value);

        while (index != 0 && value == values.getDouble(index - 1)) {
            index--;
        }

        return index;
    }

    /**
     * Finds the value in the list, or the one right above it.
     *
     * @param values a list of values
     * @param value a value
     * @return the index of the value
     */
    public static int binarySearchValueOrHigher(ListNumber values, double value) {
        if (value <= values.getDouble(0)) {
            return 0;
        }
        if (value >= values.getDouble(values.size() -1)) {
            return values.size() - 1;
        }

        int index = binarySearch(0, values.size() - 1, values, value);

        while (index != values.size() - 1 && value > values.getDouble(index)) {
            index++;
        }

        while (index != values.size() - 1 && value == values.getDouble(index + 1)) {
            index++;
        }

        return index;
    }

    private static int binarySearch(int low, int high, ListNumber values, double value) {
        // Taken from JDK
        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = values.getDouble(mid);

            if (midVal < value)
                low = mid + 1;  // Neither val is NaN, thisVal is smaller
            else if (midVal > value)
                high = mid - 1; // Neither val is NaN, thisVal is larger
            else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(value);
                if (midBits == keyBits)     // Values are equal
                    return mid;             // Key found
                else if (midBits < keyBits) // (-0.0, 0.0) or (!NaN, NaN)
                    low = mid + 1;
                else                        // (0.0, -0.0) or (NaN, !NaN)
                    high = mid - 1;
            }
        }

        return low - 1;  // key not found.
    }

    /**
     * Creates a list of equally spaced values given the range and the number of
     * elements.
     * <p>
     * Note that, due to rounding errors in double precision, the difference
     * between the elements may not be exactly the same.
     *
     * @param minValue the first value in the list
     * @param maxValue the last value in the list
     * @param size the size of the list
     * @return a new list
     */
    public static ListNumber linearListFromRange(final double minValue, final double maxValue, final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive (was " + size + " )");
        }
        return new LinearListDoubleFromRange(size, minValue, maxValue);
    }

    /**
     * Creates a list of equally spaced values given the first value, the
     * step between element and the size of the list.
     *
     * @param initialValue the first value in the list
     * @param increment the difference between elements
     * @param size the size of the list
     * @return a new list
     */
    public static ListNumber linearList(final double initialValue, final double increment, final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive (was " + size + " )");
        }
        return new LinearListDouble(size, initialValue, increment);
    }

    /**
     * Tests whether the list contains a equally spaced numbers.
     * <p>
     * Always returns true if the list was created with {@link #linearList(double, double, int) }
     * or {@link #linearListFromRange(double, double, int) }. For all other cases,
     * takes the first and last value, creates a linearListFromRange, and checks
     * whether the difference is greater than the precision allowed by double.
     * Note that this method is really strict, and it may rule out cases
     * that may be considered to be linear.
     *
     * @param listNumber a list number
     * @return true if the elements of the list are equally spaced
     */
    public static boolean isLinear(ListNumber listNumber) {
        if (listNumber instanceof LinearListDouble || listNumber instanceof LinearListDoubleFromRange) {
            return true;
        }
        ListDouble diff = ListMath.subtract(listNumber, linearListFromRange(listNumber.getDouble(0), listNumber.getDouble(listNumber.size() - 1), listNumber.size()));
        for (int i = 0; i < diff.size(); i++) {
            if (Math.abs(diff.getDouble(i)) > Math.ulp(listNumber.getDouble(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts an array of primitive numbers to the appropriate ListNumber
     * implementation.
     *
     * @param primitiveArray must be an array of primitive numbers (byte[],
     *        short[], int[], long[], float[] or double[])
     * @return the wrapped array
     */
    public static ListNumber toListNumber(Object primitiveArray) {
        if (primitiveArray instanceof byte[]) {
            return new ArrayByte((byte[]) primitiveArray);
        } else if (primitiveArray instanceof short[]) {
            return new ArrayShort((short[]) primitiveArray);
        } else if (primitiveArray instanceof int[]) {
            return new ArrayInt((int[]) primitiveArray);
        } else if (primitiveArray instanceof long[]) {
            return new ArrayLong((long[]) primitiveArray);
        } else if (primitiveArray instanceof float[]) {
            return new ArrayFloat((float[]) primitiveArray);
        } else if (primitiveArray instanceof double[]) {
            return new ArrayDouble((double[]) primitiveArray);
        } else {
            throw new IllegalArgumentException(primitiveArray + " is not a an array of primitive numbers");
        }
    }

    private static class LinearListDoubleFromRange extends ListDouble {

        private final int size;
        private final double minValue;
        private final double maxValue;

        public LinearListDoubleFromRange(int size, double minValue, double maxValue) {
            this.size = size;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public double getDouble(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            return minValue + (index * (maxValue - minValue)) / (size - 1);
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static class LinearListDouble extends ListDouble {

        private final int size;
        private final double initialValue;
        private final double increment;

        public LinearListDouble(int size, double initialValue, double increment) {
            this.size = size;
            this.initialValue = initialValue;
            this.increment = increment;
        }

        @Override
        public double getDouble(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            return initialValue + index * increment;
        }

            @Override
            public int size() {
                return size;
            }
    }

    /**
     * Returns a view of the given list that presents only the elements
     * at the given indexes.
     *
     * @param list a numeric list
     * @param indexes the indexes with the values to expose
     * @return a wrapper around list
     */
    public static ListNumber listView(ListNumber list, ListInt indexes) {
        if (list instanceof ListDouble) {
            return new ListView.Double((ListDouble) list, indexes);
        } else if (list instanceof ListFloat) {
            return new ListView.Float((ListFloat) list, indexes);
        } else if (list instanceof ListLong) {
            return new ListView.Long((ListLong) list, indexes);
        } else if (list instanceof ListInt) {
            return new ListView.Int((ListInt) list, indexes);
        } else if (list instanceof ListShort) {
            return new ListView.Short((ListShort) list, indexes);
        } else if (list instanceof ListByte) {
            return new ListView.Byte((ListByte) list, indexes);
        }
        throw new UnsupportedOperationException("Not yet supported");
    }
}
