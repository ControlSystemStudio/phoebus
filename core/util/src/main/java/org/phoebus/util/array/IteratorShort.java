/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An iterator of {@code short}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class IteratorShort implements IteratorNumber {

    @Override
    public float nextFloat() {
        return (float) nextShort();
    }

    @Override
    public double nextDouble() {
        return (double) nextShort();
    }

    @Override
    public byte nextByte() {
        return (byte) nextShort();
    }

    @Override
    public int nextInt() {
        return (int) nextShort();
    }

    @Override
    public long nextLong() {
        return (long) nextShort();
    }

}
