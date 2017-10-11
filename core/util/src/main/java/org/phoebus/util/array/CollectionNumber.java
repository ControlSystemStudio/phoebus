/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.Collection;

/**
 * A collection of numeric (primitive) elements. It provides a size and
 * can be iterated more than once.
 * <p>
 * The method names are taken from {@link Collection}, though not all
 * methods are specified. At this moment, the class is read-only. If in the
 * future the class is extended, the new methods should match the names from
 * {@link Collection}.
 *
 * @author Gabriele Carcassi
 */
public interface CollectionNumber {

    /**
     * Returns an iterator over the elements of the collection.
     *
     * @return a new iterator
     */
    IteratorNumber iterator();

    /**
     * Returns the number of elements in the collection.
     *
     * @return the number of elements in the collection
     */
    int size();
}
