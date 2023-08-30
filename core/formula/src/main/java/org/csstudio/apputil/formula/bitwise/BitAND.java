package org.csstudio.apputil.formula.bitwise;

public class BitAND extends TwoArgBitwiseOperation
{
    public BitAND()
    {
        super("bitAND", "Bitwise AND (x, y)", (a, b) -> a & b);
    }
}