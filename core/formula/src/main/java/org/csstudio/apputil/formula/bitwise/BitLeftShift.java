package org.csstudio.apputil.formula.bitwise;

public class BitLeftShift extends TwoArgBitwiseOperation
{
    public BitLeftShift()
    {
        super("bitLeftShift", "Bitwise Left Shift (x, y)", (a, b) -> a << b);
    }
}
