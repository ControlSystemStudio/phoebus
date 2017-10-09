/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.AbstractList;
import java.util.List;

/**
 * Severity of an alarm.
 * <p>
 * Values are provided in order of increasing severity, so you can rely on
 * {@link #ordinal() } and {@link #compareTo(java.lang.Enum) } for comparison
 * and ordering. In case additional AlarmSeverity values are added in the future,
 * which is very unlikely, they will be added in order as well.
 * <p>
 * One should always bear in mind that the alarm severity of the IOC is set on
 * the record, and not on the individual channel. If  one is not connecting
 * to the value field of the record, the severity does not reflect the state
 * of that field.
 * <p>
 * For example: a record may be INVALID meaning that the value of the field
 * was not correctly read by the hardware; if one connects to the display limit
 * field, the value of that field will still be ok, but the alarm severity (if
 * requested) would say INVALID.
 *
 * @author carcassi
 */
public enum AlarmSeverity {

    /**
     * The current value is valid, and there is no alarm.
     */
    NONE,

    /**
     * There is a minor problem with the value: the exact meaning is defined
     * by the channel, but typically this means that the value is valid and is
     * outside some working range.
     */
    MINOR,

    /**
     * There is a major problem with the value: the exact meaning is defined
     * by the channel, but typically this means that the value is valid and is
     * outside some working range.
     */
    MAJOR,

    /**
     * There is a major problem with the value itself: the exact meaning is defined
     * by the channel, but typically this means that the returned value is not a
     * real representation of the actual value.
     */
    INVALID,

    /**
     * The channel cannot be read and its state is undefined: the exact meaning is defined
     * by the channel, but typically this means that the client is either disconnected
     * or connected with no read access. The value is either stale or invalid.
     */
    UNDEFINED;

    private static final List<String> labels = new AbstractList<String>() {
        @Override
        public String get(int index) {
            return AlarmSeverity.values()[index].name();
        }

        @Override
        public int size() {
            return AlarmSeverity.values().length;
        }
    };

    /**
     * Returns the list of labels for the severity.
     * <p>
     * This is useful to create VEnums containing severities.
     *
     * @return an immutable list with the labels
     */
    public static List<String> labels() {
        return labels;
    }
}
