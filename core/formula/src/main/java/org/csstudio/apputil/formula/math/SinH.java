package org.csstudio.apputil.formula.math;

/**
 * SinH class see @Math sinh function
 */
public class SinH extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public SinH()
    {
        super("sinh", "Hyperbolic sine", Math::sinh);
    }
}