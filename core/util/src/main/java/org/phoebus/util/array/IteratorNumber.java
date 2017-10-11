/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.NoSuchElementException;

/**
 * An iterator for a stream of primitive numbers, which allows to retrieve
 * the value casted in the type you prefer. This class allows to implement
 * a single binding for iterating over a collection instead of six different
 * binding. If the original type is required, instanceof can be used to
 * differentiate between {@link IteratorByte}, {@link IteratorShort},
 * {@link IteratorInt}, {@link IteratorLong}, {@link IteratorFloat} and
 * {@link IteratorDouble}.
 * <p>
 * We looked into making this class implement Iterator, but unfortunately,
 * because of generics being invariant, we cannot provide a scheme that would work naturally
 * in all cases. Ideally, we would want to have {@code IteratorNumber} be {@code Iterator<Number>}
 * and {@code IteratorDouble} be {@code Iterator<Double>}, but this does
 * not work because generics are invariant. We could have
 * {@code Iterator<T extends Number>} and {@code Iterator<Double>}, but that would
 * mean {@code IteratorNumber} would need a type parameter, and the user of the
 * API would have to use bound type parameters like {@code IteratorNumber<? extends Number>},
 * which is awful. We could have all extend {@code Iterator<Number>} but then
 * the foreach loops would return {@code Number} in all cases, even for collections
 * of more specific type.
 *
 * @author Gabriele Carcassi
 */
public interface IteratorNumber {

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@code nextXxx} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();

    /**
     * Returns the next element in the iteration casted to a float.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    float nextFloat();

    /**
     * Returns the next element in the iteration casted to a double.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    double nextDouble();

    /**
     * Returns the next element in the iteration casted to a byte.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    byte nextByte();

    /**
     * Returns the next element in the iteration casted to a short.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    short nextShort();

    /**
     * Returns the next element in the iteration casted to an int.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    int nextInt();

    /**
     * Returns the next element in the iteration casted to a long.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    long nextLong();
}
