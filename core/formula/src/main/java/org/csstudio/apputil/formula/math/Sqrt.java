package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Sqrt extends OneArgMathFunction
{
    public Sqrt()
    {
        super("sqrt", "Square Root", Math::sqrt);
    }
}