/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An iterator of {@code int}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class IteratorInt implements IteratorNumber {

    @Override
    public float nextFloat() {
        return (float) nextInt();
    }

    @Override
    public double nextDouble() {
        return (double) nextInt();
    }

    @Override
    public byte nextByte() {
        return (byte) nextInt();
    }

    @Override
    public short nextShort() {
        return (short) nextInt();
    }

    @Override
    public long nextLong() {
        return (long) nextInt();
    }

}
