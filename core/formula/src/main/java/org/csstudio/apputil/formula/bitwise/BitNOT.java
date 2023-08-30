package org.csstudio.apputil.formula.bitwise;

public class BitNOT extends OneArgBitwiseOperation
{
    public BitNOT()
    {
        super("bitNOT", "Bitwise NOT (x)", a -> ~a);
    }
}
