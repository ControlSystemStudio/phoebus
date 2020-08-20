package org.csstudio.apputil.formula.math;

public class Min extends TwoArgMathFunction {

    protected Min(String name, String description, TwoArgFunction function)
    {
        super("min", "Smaller of the two values", Math::min);
    }
}
