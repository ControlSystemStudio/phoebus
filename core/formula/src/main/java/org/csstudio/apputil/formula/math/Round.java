package org.csstudio.apputil.formula.math;

/**
 * Round class see @Math round function
 */
public class Round extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Round()
    {
        super("round", "Round", Math::round);
    }
}