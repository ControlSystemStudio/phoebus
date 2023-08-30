package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class TanH extends OneArgMathFunction
{
    public TanH()
    {
        super("tanh", "Hyperbolic tangent", Math::tanh);
    }
}