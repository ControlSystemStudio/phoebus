package org.csstudio.apputil.formula.math;

/**
 * ATan class see @Math atan function
 */
public class ATan extends OneArgMathFunction
{
	/**
	 * Constructor
	 */
    public ATan()
    {
        super("atan", "Inverse tangent", Math::atan);
    }
}