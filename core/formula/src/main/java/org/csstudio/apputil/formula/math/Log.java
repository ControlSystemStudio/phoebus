package org.csstudio.apputil.formula.math;

@SuppressWarnings("nls")
public class Log extends OneArgMathFunction
{
    public Log()
    {
        super("log", "Natural logarithm", Math::log);
    }
}