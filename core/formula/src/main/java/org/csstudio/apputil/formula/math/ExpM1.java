package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ExpM1 extends OneArgMathFunction
{
    public ExpM1()
    {
        super("expm1", "exp(x)-1", Math::expm1);
    }
}