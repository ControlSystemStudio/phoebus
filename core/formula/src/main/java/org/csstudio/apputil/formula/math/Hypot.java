package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Hypot extends TwoArgMathFunction
{
    public Hypot()
    {
        super("hypot", "Hypotenuse (x, y)", Math::hypot);
    }
}