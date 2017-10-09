/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 * Multi channel array.
 * <p>
 * This same type can be used in multiple circumstances where data is collected,
 * regardless of how and what of data. The number of values in the multi channel
 * array never changes.
 * <ul>
 *   <li>Synchronized array: {@link Time} returns the reference
 * time of the collected data); each element may or may not retain
 * its time information.</li>
 *   <li>Multi channel: {@link Time} returns the time of the data generation;
 * each element may or may not retain its time information</li>
 * </ul>
 * <p>
 * A {@link MultiScalar} can be automatically converted to a {@link Array} of the
 * same type.
 *
 * @param <T> the type for the multi channel values
 * @author carcassi
 */
public interface MultiScalar<T extends Scalar> extends VType {

    /**
     * The list of values for all the different channels. Never null.
     *
     * @return a {@link List} of values
     */
    List<T> getValues();
}
