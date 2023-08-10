package org.csstudio.apputil.formula.bitwise;

public class BitOR extends TwoArgBitwiseOperation
{
    public BitOR()
    {
        super("bitOR", "Bitwise OR (x, y)", (a, b) -> a | b);
    }
}
