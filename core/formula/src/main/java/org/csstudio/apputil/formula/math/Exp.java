package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Exp extends OneArgMathFunction
{
    public Exp()
    {
        super("exp", "Exponential", Math::exp);
    }
}