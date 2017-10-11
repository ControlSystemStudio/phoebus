/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tag interface to mark all the members of the value classes.
 *
 * @author carcassi
 */
public interface VType {

    static final Collection<Class<?>> types = Arrays.<Class<?>>asList(
            VByte.class,
            VShort.class,
            VInt.class,
            VLong.class,
            VFloat.class,
            VDouble.class);

    /**
     * Returns the type of the object by returning the class object of one of the
     * VXxx interfaces. The getClass() methods returns the concrete implementation
     * type, which is of little use. If no super-interface is found, Object.class is
     * used.
     *
     * @param obj
     *            an object implementing a standard type
     * @return the type is implementing
     */
    public static Class<?> typeOf(Object obj) {
        if (obj == null)
            return null;

        for (Class<?> type : types) {
            if (type.isInstance(obj)) {
                return type;
            }
        }

        return Object.class;
    }

}
