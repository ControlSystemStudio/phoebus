package org.csstudio.apputil.formula.math;

/**
 * Tan class see @Math tan function
 */
public class Tan extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public Tan()
    {
        super("tan", "Tangent", Math::tan);
    }
}