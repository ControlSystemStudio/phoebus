package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class CosH extends OneArgMathFunction
{
    public CosH()
    {
        super("cosh", "Hyperbolic cosine", Math::cosh);
    }
}