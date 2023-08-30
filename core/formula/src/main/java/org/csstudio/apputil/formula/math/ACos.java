package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ACos extends OneArgMathFunction
{
    public ACos()
    {
        super("acos", "Inverse cosine", Math::acos);
    }
}