package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Cos extends OneArgMathFunction
{
    public Cos()
    {
        super("cos", "Cosine", Math::cos);
    }
}