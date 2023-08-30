package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class SinH extends OneArgMathFunction
{
    public SinH()
    {
        super("sinh", "Hyperbolic sine", Math::sinh);
    }
}