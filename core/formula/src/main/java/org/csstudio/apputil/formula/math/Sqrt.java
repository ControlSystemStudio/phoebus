package org.csstudio.apputil.formula.math;

/**
 * Sqrt class see @Math sqrt function
 */
public class Sqrt extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Sqrt()
    {
        super("sqrt", "Square Root", Math::sqrt);
    }
}