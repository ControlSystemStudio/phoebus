package org.epics.pva.exceptions;

/** Protocol exception for malformed array payloads. */
@SuppressWarnings("nls")
public class PVAArraySizeException extends PVAProtocolException
{
    /**
     * @param size Decoded array element count
     * @param remaining Number of bytes left in buffer
     */
    public PVAArraySizeException(final int size, final int remaining)
    {
        this(size, remaining, 1);
    }

    /**
     * @param size Decoded array element count
     * @param remaining Number of bytes left in buffer
     * @param bytesPerElement Number of bytes required per array element
     */
    public PVAArraySizeException(final int size, final int remaining, final int bytesPerElement)
    {
        super(createMessage(size, remaining, bytesPerElement));
    }

    private static String createMessage(final int size, final int remaining, final int bytesPerElement)
    {
        if (size < 0)
            return "Negative array size " + size;

        final long needed = (long) size * bytesPerElement;
        return "Array size " + size + " needs " + needed + " bytes with element size " + bytesPerElement +
               " but buffer has only " + remaining + " bytes";
    }
}
