package org.csstudio.apputil.formula.math;

/**
 * Log class see @Math log function
 */
public class Log extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Log()
    {
        super("log", "Natural logarithm", Math::log);
    }
}