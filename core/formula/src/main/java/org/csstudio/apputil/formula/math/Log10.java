package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Log10 extends OneArgMathFunction
{
    public Log10()
    {
        super("log10", "Decadic logarithm", Math::log10);
    }
}