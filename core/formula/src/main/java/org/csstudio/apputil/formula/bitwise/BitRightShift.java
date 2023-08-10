package org.csstudio.apputil.formula.bitwise;

public class BitRightShift extends TwoArgBitwiseOperation
{
    public BitRightShift()
    {
        super("bitRightShift", "Bitwise Right Shift (x, y)", (a, b) -> a >> b);
    }
}
