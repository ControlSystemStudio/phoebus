package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Abs extends OneArgMathFunction
{
    public Abs()
    {
        super("abs", "Absolute value", Math::abs);
    }
}