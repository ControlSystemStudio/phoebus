package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ATan2 extends TwoArgMathFunction
{
    public ATan2()
    {
        super("atan2", "Inverse tangent (y, x)", Math::atan2);
    }
}