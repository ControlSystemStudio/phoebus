package org.csstudio.apputil.formula.math;

/**
 * TanH class see @Math tanh function
 */
public class TanH extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public TanH()
    {
        super("tanh", "Hyperbolic tangent", Math::tanh);
    }
}