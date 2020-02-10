package org.csstudio.apputil.formula.math;

import java.util.List;

@SuppressWarnings("nls")
public class Sin extends OneArgMathFunction
{
    public Sin()
    {
        super("sin", "Sine", Math::sin);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("angle");
    }
}