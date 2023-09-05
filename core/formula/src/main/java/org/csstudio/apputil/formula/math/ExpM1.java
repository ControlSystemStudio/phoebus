package org.csstudio.apputil.formula.math;

/**
 * ExpM1 class see @Math expm1 function
 */
public class ExpM1 extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public ExpM1()
    {
        super("expm1", "exp(x)-1", Math::expm1);
    }
}