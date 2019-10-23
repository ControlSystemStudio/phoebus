package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Round extends OneArgMathFunction
{
    public Round()
    {
        super("round", "Round", Math::round);
    }
}