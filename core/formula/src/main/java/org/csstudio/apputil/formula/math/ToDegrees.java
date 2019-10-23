package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ToDegrees extends OneArgMathFunction
{
    public ToDegrees()
    {
        super("toDegrees", "Radians to degrees", Math::toDegrees);
    }
}