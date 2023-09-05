package org.csstudio.apputil.formula.math;

/**
 * Hypot class see @Math hypot function
 */
public class Hypot extends TwoArgMathFunction
{
	/**
     * Constructor
     */
	public Hypot()
    {
        super("hypot", "Hypotenuse (x, y)", Math::hypot);
    }
}