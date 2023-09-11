package org.csstudio.apputil.formula.math;

/**
 * ToDegrees class see @Math toDegrees function
 */
public class ToDegrees extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public ToDegrees()
    {
        super("toDegrees", "Radians to degrees", Math::toDegrees);
    }
}