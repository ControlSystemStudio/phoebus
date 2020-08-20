package org.csstudio.apputil.formula.math;

public class Max extends TwoArgMathFunction {

    protected Max(String name, String description, TwoArgFunction function)
    {
        super("max", "Greater of the two values", Math::max);
    }
}
