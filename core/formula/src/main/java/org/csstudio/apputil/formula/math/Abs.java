package org.csstudio.apputil.formula.math;

/**
 * Abs class see @Math abs function
 */
public class Abs extends OneArgMathFunction
{
    /**
     * Constructor
     */
	public Abs()
    {
        super("abs", "Absolute value", Math::abs);
    }
}