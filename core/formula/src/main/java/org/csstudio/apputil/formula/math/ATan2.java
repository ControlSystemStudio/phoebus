package org.csstudio.apputil.formula.math;

import java.util.List;

@SuppressWarnings("nls")
public class ATan2 extends TwoArgMathFunction
{
    public ATan2()
    {
        super("atan2", "Inverse tangent (y, x)", Math::atan2);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("y", "x");
    }
}