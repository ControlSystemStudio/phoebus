package org.csstudio.apputil.formula.math;

/**
 * Floor class see @Math floor function
 */
public class Floor extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Floor()
    {
        super("floor", "Floor", Math::floor);
    }
}