/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An iterator of {@code float}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class IteratorFloat implements IteratorNumber {

    @Override
    public double nextDouble() {
        return (double) nextFloat();
    }

    @Override
    public byte nextByte() {
        return (byte) nextFloat();
    }

    @Override
    public short nextShort() {
        return (short) nextFloat();
    }

    @Override
    public int nextInt() {
        return (int) nextFloat();
    }

    @Override
    public long nextLong() {
        return (long) nextFloat();
    }

}
