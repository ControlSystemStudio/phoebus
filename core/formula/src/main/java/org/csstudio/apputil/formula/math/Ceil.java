package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Ceil extends OneArgMathFunction
{
    public Ceil()
    {
        super("ceil", "Ceiling", Math::ceil);
    }
}