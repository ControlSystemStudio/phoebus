package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Floor extends OneArgMathFunction
{
    public Floor()
    {
        super("floor", "Floor", Math::floor);
    }
}