/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An iterator of {@code long}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class IteratorLong implements IteratorNumber {

    @Override
    public float nextFloat() {
        return (float) nextLong();
    }

    @Override
    public double nextDouble() {
        return (double) nextLong();
    }

    @Override
    public byte nextByte() {
        return (byte) nextLong();
    }

    @Override
    public short nextShort() {
        return (short) nextLong();
    }

    @Override
    public int nextInt() {
        return (int) nextLong();
    }

}
