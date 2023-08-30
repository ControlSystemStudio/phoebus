package org.csstudio.apputil.formula.math;

/**
 * Exp class see @Math exp function
 */
public class Exp extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Exp()
    {
        super("exp", "Exponential", Math::exp);
    }
}