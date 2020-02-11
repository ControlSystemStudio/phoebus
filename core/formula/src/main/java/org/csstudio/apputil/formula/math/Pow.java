package org.csstudio.apputil.formula.math;

import java.util.List;

@SuppressWarnings("nls")
public class Pow extends TwoArgMathFunction
{
    public Pow()
    {
        super("pow", "Power (base, exponent)", Math::pow);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("base", "exponent");
    }
}