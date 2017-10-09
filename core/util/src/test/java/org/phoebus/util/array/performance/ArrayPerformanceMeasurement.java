/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array.performance;

import org.phoebus.util.array.ArrayShort;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.CollectionNumber;
import org.phoebus.util.array.ArrayFloat;
import org.phoebus.util.array.ArrayByte;
import org.phoebus.util.array.ArrayInt;
import java.util.Random;

/**
 *
 * @author carcassi
 */
public class ArrayPerformanceMeasurement {

    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));

        int arraySize = 100000;
        int nIterations = 10000;

        double[] doubleArray = new double[arraySize];
        float[] floatArray = new float[arraySize];
        int[] intArray = new int[arraySize];
        short[] shortArray = new short[arraySize];
        byte[] byteArray = new byte[arraySize];
        Random rand = new Random();
        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = rand.nextGaussian();
            floatArray[i] = (float) rand.nextGaussian();
            intArray[i] = rand.nextInt(100);
            shortArray[i] = (short) rand.nextInt(100);
        }
        rand.nextBytes(byteArray);

        // Rearrenging the order will change which type executes faster
        profileArrayThroughCollectionNumber(doubleArray, nIterations);
        profileArrayThroughCollectionNumber(floatArray, nIterations);
        profileArrayThroughCollectionNumber(intArray, nIterations);
        profileArrayThroughCollectionNumber(shortArray, nIterations);
        profileArrayThroughCollectionNumber(byteArray, nIterations);
        profileArrayThroughCollectionNumber(doubleArray, nIterations);

        profileJavaArray(doubleArray, nIterations);
        profileJavaArray(intArray, nIterations);
        profileJavaArray(byteArray, nIterations);

        profileArray(doubleArray, nIterations);
        profileArray(intArray, nIterations);
        profileArray(byteArray, nIterations);

    }

    private static void profileArrayThroughCollectionNumber(double[] doubleArray, int nIterations) {
        long startTime = System.nanoTime();
        CollectionNumber list = new ArrayDouble(doubleArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = ArrayOperation.sum.compute(list);
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using double[] through abstract class: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArrayThroughCollectionNumber(float[] floatArray, int nIterations) {
        long startTime = System.nanoTime();
        CollectionNumber list = new ArrayFloat(floatArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = ArrayOperation.average.compute(list);
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using float[] through abstract class: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArrayThroughCollectionNumber(int[] intArray, int nIterations) {
        long startTime = System.nanoTime();
        CollectionNumber list = new ArrayInt(intArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = ArrayOperation.average.compute(list);
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using int[] through abstract class: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArrayThroughCollectionNumber(byte[] byteArray, int nIterations) {
        long startTime = System.nanoTime();
        CollectionNumber list = new ArrayByte(byteArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = ArrayOperation.average.compute(list);
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using byte[] through abstract class: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArrayThroughCollectionNumber(short[] shortArray, int nIterations) {
        long startTime = System.nanoTime();
        CollectionNumber list = new ArrayShort(shortArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = ArrayOperation.average.compute(list);
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using short[] through abstract class: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArray(double[] doubleArray, int nIterations) {
        long startTime = System.nanoTime();
        ArrayDouble coll = new ArrayDouble(doubleArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = 0;
            for (int j = 0; j < coll.size(); j++) {
                sum += coll.getDouble(j);
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using ArrayDouble: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArray(int[] intArray, int nIterations) {
        long startTime = System.nanoTime();
        ArrayInt coll = new ArrayInt(intArray);
        for (int i = 0; i < nIterations; i++) {
            double sum = 0;
            for (int j = 0; j < coll.size(); j++) {
                sum += coll.getDouble(j);
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using ArrayInt: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileArray(byte[] byteArray, int nIterations) {
        long startTime = System.nanoTime();
        ArrayByte coll = new ArrayByte(byteArray);
        for (int i = 0; i < nIterations; i++) {
            int sum = 0;
            for (int j = 0; j < coll.size(); j++) {
                sum += coll.getByte(j);
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using ArrayByte: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileJavaArray(double[] doubleArray, int nIterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < nIterations; i++) {
            double sum = 0;
            for (int j = 0; j < doubleArray.length; j++) {
                sum += doubleArray[j];
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using double[]: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileJavaArray(int[] intArray, int nIterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < nIterations; i++) {
            double sum = 0;
            for (int j = 0; j < intArray.length; j++) {
                sum += intArray[j];
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using int[]: ns " + (stopTime - startTime) / nIterations);
    }

    private static void profileJavaArray(byte[] byteArray, int nIterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < nIterations; i++) {
            int sum = 0;
            for (int j = 0; j < byteArray.length; j++) {
                sum += byteArray[j];
            }
            if (sum == 0) {
                System.out.println("Unexpected value " + sum);
            }
        }
        long stopTime = System.nanoTime();

        System.out.println("Iteration using byte[]: ns " + (stopTime - startTime) / nIterations);
    }
}
