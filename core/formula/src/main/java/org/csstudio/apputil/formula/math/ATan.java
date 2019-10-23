package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ATan extends OneArgMathFunction
{
    public ATan()
    {
        super("atan", "Inverse tangent", Math::atan);
    }
}