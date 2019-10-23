package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Pow extends TwoArgMathFunction
{
    public Pow()
    {
        super("pow", "Power (base, exponent)", Math::pow);
    }
}