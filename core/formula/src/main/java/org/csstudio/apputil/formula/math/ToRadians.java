package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ToRadians extends OneArgMathFunction
{
    public ToRadians()
    {
        super("toRadians", "Degrees to radians", Math::toRadians);
    }
}