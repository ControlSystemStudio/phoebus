/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array.performance;

import java.util.Random;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.CircularBufferDouble;

/**
 *
 * @author carcassi
 */
public class CircularBufferBenchmark {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));

        int nSamples = 100000;
        int capacity = 125000;
        int nIterations = 10000;

        double[] doubleArray = new double[nSamples];
        Random rand = new Random();
        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = rand.nextGaussian();
        }

        CircularBufferDouble list = new CircularBufferDouble(capacity);
        for (int i = 0; i < doubleArray.length; i++) {
            list.addDouble(doubleArray[i]);
        }

        ListBenchmark.profileListDouble(list, nIterations);

        ArrayDouble array = new ArrayDouble(doubleArray);

        ListBenchmark.profileListDouble(array, nIterations);

    }
}
