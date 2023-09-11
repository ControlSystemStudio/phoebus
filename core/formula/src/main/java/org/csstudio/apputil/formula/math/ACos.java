package org.csstudio.apputil.formula.math;

/**
 * Acos class see @Math acos function
 */
public class ACos extends OneArgMathFunction
{
	 /**
     * Constructor
     */
	public ACos()
    {
        super("acos", "Inverse cosine", Math::acos);
    }
}