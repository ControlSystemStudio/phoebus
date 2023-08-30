package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Tan extends OneArgMathFunction
{
    public Tan()
    {
        super("tan", "Tangent", Math::tan);
    }
}