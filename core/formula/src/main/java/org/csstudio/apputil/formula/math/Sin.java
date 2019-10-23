package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Sin extends OneArgMathFunction
{
    public Sin()
    {
        super("sin", "Sine", Math::sin);
    }
}