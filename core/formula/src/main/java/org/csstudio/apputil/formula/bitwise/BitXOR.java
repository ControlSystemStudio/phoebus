package org.csstudio.apputil.formula.bitwise;

public class BitXOR extends TwoArgBitwiseOperation
{
    public BitXOR()
    {
        super("bitXOR", "Bitwise XOR (x, y)", (a, b) -> a ^ b);
    }
}
