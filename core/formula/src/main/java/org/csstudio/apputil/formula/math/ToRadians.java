package org.csstudio.apputil.formula.math;

/**
 * ToRadians class see @Math toRadians function
 */
public class ToRadians extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public ToRadians()
    {
        super("toRadians", "Degrees to radians", Math::toRadians);
    }
}