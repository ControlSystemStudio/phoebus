package org.csstudio.apputil.formula.math;

import java.util.List;

@SuppressWarnings("nls")
public class Cos extends OneArgMathFunction
{
    public Cos()
    {
        super("cos", "Cosine", Math::cos);
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("angle");
    }
}