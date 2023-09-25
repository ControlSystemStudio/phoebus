package org.csstudio.apputil.formula.math;

/**
 * Log10 class see @Math log10 function
 */
public class Log10 extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Log10()
    {
        super("log10", "Decadic logarithm", Math::log10);
    }
}