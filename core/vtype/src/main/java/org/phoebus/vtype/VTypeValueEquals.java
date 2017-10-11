/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.Objects;

/**
 * Helper class that provides functions to test value equality.
 *
 * @author carcassi
 */
public class VTypeValueEquals {

    private VTypeValueEquals() {
        // Do not create
    }

    /**
     * Tests whether two alarms have the same name and severity.
     *
     * @param alarm1 first alarm
     * @param alarm2 second alarm
     * @return true if equal or both null
     */
    public static boolean alarmEquals(Alarm alarm1, Alarm alarm2) {
        if (alarm1 == null && alarm2 == null) {
            return true;
        }

        if (alarm1 == null || alarm2 == null) {
            return false;
        }

        return alarm1.getAlarmSeverity().equals(alarm2.getAlarmSeverity()) &&
                alarm1.getAlarmName().equals(alarm2.getAlarmName());
    }

    /**
     * Tests whether the two time have the same timestamp, user tag
     * and if they are both valid.
     *
     * @param time1 first time
     * @param time2 second time
     * @return  true if equal or both null
     */
    public static boolean timeEquals(Time time1, Time time2) {
        if (time1 == null && time2 == null) {
            return true;
        }

        if (time1 == null || time2 == null) {
            return false;
        }

        return Objects.equals(time1.getTimestamp(), time2.getTimestamp()) &&
                Objects.equals(time1.getTimeUserTag(), time2.getTimeUserTag()) &&
                time1.isTimeValid() == time2.isTimeValid();
    }

    /**
     * Tests whether the two objects are of the same VType.
     *
     * @param obj1 first object
     * @param obj2 second object
     * @return true if same type or both null
     */
    public static boolean typeEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }

        if (obj1 == null || obj2 == null) {
            return false;
        }

        return Objects.equals(ValueUtil.typeOf(obj1), ValueUtil.typeOf(obj2));
    }

    /**
     * Checks whether the two table have the same data: equal column names, number
     * of rows and columns and their value.
     *
     * @param arg1 a table
     * @param arg2 another table
     * @return true if the values are equals
     */
    public static boolean valueEquals(VTable arg1, VTable arg2) {
        if (arg1.getColumnCount() != arg2.getColumnCount()) {
            return false;
        }

        if (arg1.getRowCount() != arg2.getRowCount()) {
            return false;
        }

        for (int i = 0; i < arg1.getColumnCount(); i++) {
            if (!arg1.getColumnName(i).equals(arg2.getColumnName(i))) {
                return false;
            }

            if (!arg1.getColumnType(i).equals(arg2.getColumnType(i))) {
                return false;
            }

            if (!arg1.getColumnData(i).equals(arg2.getColumnData(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the two array have the data: equal data and
     * equal sizes.
     *
     * @param array1 first array
     * @param array2 second array
     * @return true if equal data or both null
     */
    public static boolean valueEquals(VNumberArray array1, VNumberArray array2) {
        return array1.getData().equals(array2.getData()) && array1.getSizes().equals(array2.getSizes());
    }

    /**
     * Checks whether the two number have the same value.
     *
     * @param number1 the first number
     * @param number2 the second number
     * @return true if equal value or both null
     */
    public static boolean valueEquals(VNumber number1, VNumber number2) {
        return number1.getValue().equals(number2.getValue());
    }

    /**
     * Checks whether the two booleans have the same value.
     *
     * @param bool1 the first boolean
     * @param bool2 the second boolean
     * @return true if equal value or both null
     */
    public static boolean valueEquals(VBoolean bool1, VBoolean bool2) {
        return bool1.getValue().equals(bool2.getValue());
    }

    public static boolean valueEquals(VBooleanArray bool1, VBooleanArray bool2) {
        return bool1.getData().equals(bool2.getData());
    }

    public static boolean valueEquals(VString str1, VString str2) {
        return str1.getValue().equals(str2.getValue());
    }

    public static boolean valueEquals(VEnum enum1, VEnum enum2) {
        return enum1.getValue().equals(enum2.getValue()) && enum1.getLabels().equals(enum2.getLabels());
    }

    public static boolean valueEquals(VEnumArray enum1, VEnumArray enum2) {
        return enum1.getIndexes().equals(enum2.getIndexes()) && enum1.getLabels().equals(enum2.getLabels());
    }

    public static boolean valueEquals(VStringArray array1, VStringArray array2) {
        return array1.getData().equals(array2.getData());
    }

    public static boolean valueEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }

        if (obj1 == null || obj2 == null) {
            return false;
        }

        if ((obj1 instanceof VNumberArray) && (obj2 instanceof VNumberArray)) {
            return valueEquals((VNumberArray) obj1, (VNumberArray) obj2);
        }

        if ((obj1 instanceof VStringArray) && (obj2 instanceof VStringArray)) {
            return valueEquals((VStringArray) obj1, (VStringArray) obj2);
        }

        if ((obj1 instanceof VString) && (obj2 instanceof VString)) {
            return valueEquals((VString) obj1, (VString) obj2);
        }

        if ((obj1 instanceof VEnum) && (obj2 instanceof VEnum)) {
            return valueEquals((VEnum) obj1, (VEnum) obj2);
        }

        if ((obj1 instanceof VEnumArray) && (obj2 instanceof VEnumArray)) {
            return valueEquals((VEnumArray) obj1, (VEnumArray) obj2);
        }

        if((obj1 instanceof VNumber) && (obj2 instanceof VNumber)) {
            return valueEquals((VNumber) obj1, (VNumber) obj2);
        }

        if((obj1 instanceof VBoolean) && (obj2 instanceof VBoolean)) {
            return valueEquals((VBoolean) obj1, (VBoolean) obj2);
        }

        if ((obj1 instanceof VBooleanArray) && (obj2 instanceof VBooleanArray)) {
            return valueEquals((VBooleanArray) obj1, (VBooleanArray) obj2);
        }

        if((obj1 instanceof VTable) && (obj2 instanceof VTable)) {
            return valueEquals((VTable) obj1, (VTable) obj2);
        }

        return false;
    }

}
