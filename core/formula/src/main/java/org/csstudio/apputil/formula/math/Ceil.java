package org.csstudio.apputil.formula.math;

/**
 * Ceil class see @Math ceil function
 */
public class Ceil extends OneArgMathFunction
{
	 /**
     * Constructor
     */
	public Ceil()
    {
        super("ceil", "Ceiling", Math::ceil);
    }
}