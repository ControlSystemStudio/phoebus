/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An iterator of {@code byte}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class IteratorByte implements IteratorNumber {

    @Override
    public float nextFloat() {
        return (float) nextByte();
    }

    @Override
    public double nextDouble() {
        return (double) nextByte();
    }

    @Override
    public short nextShort() {
        return (short) nextByte();
    }

    @Override
    public int nextInt() {
        return (int) nextByte();
    }

    @Override
    public long nextLong() {
        return (long) nextByte();
    }

}
