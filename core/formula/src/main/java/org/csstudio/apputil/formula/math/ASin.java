package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class ASin extends OneArgMathFunction
{
    public ASin()
    {
        super("asin", "Inverse sine", Math::asin);
    }
}