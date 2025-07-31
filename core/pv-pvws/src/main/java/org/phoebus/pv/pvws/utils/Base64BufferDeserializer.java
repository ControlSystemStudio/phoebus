package org.phoebus.pv.pvws.utils;

//import org.websocket.models.PV;

import java.nio.*;
import java.util.Base64;

public class Base64BufferDeserializer {

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    public static short[] decodeShorts(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        ShortBuffer shortBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asShortBuffer();

        short[] array = new short[shortBuffer.remaining()];
        shortBuffer.get(array);
        return array;
    }

    public static int[] decodeInts(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        IntBuffer intBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asIntBuffer();

        int[] array = new int[intBuffer.remaining()];
        intBuffer.get(array);
        return array;
    }

    public static float[] decodeFloats(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        FloatBuffer floatBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asFloatBuffer();

        float[] array = new float[floatBuffer.remaining()];
        floatBuffer.get(array);
        return array;
    }

    public static double[] decodeDoubles(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asDoubleBuffer();

        double[] array = new double[doubleBuffer.remaining()];
        doubleBuffer.get(array);
        return array;
    }

}