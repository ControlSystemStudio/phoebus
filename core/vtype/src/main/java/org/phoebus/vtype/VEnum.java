/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 * Scalar enum with alarm and timestamp.
 * Given that enumerated values are of very limited use without
 * the labels, and that the current label is the data most likely used, the
 * enum is of type {@link String}. The index is provided as an extra field, and
 * the list of all possible values is always provided.
 *
 * @author carcassi
 */
public interface VEnum extends Scalar, Enum, Alarm, Time {

    /**
     * {@inheritDoc }
     */
    @Override
    String getValue();

    /**
     * Return the index of the value in the list of labels.
     *
     * @return the current index
     */
    int getIndex();
    

    /**
     * Creates a new VEnum.
     *
     * @param index the index
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VEnum create(final int index, List<String> labels, final Alarm alarm, final Time time) {
        return new IVEnum(index, labels, alarm, time);
    }

}
