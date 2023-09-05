package org.csstudio.apputil.formula.math;

/**
 * CosH class see @Math cosh function
 */
public class CosH extends OneArgMathFunction
{
	/**
     * Constructor
     */
	public CosH()
    {
        super("cosh", "Hyperbolic cosine", Math::cosh);
    }
}